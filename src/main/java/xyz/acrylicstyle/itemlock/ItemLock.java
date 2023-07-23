package xyz.acrylicstyle.itemlock;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.acrylicstyle.itemlock.commands.ILockCommand;
import xyz.acrylicstyle.itemlock.commands.IUnlockCommand;

import java.util.*;

public class ItemLock extends JavaPlugin implements Listener {
    // TODO: PlayerInteractEvent
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("ilock")).setExecutor(new ILockCommand());
        Objects.requireNonNull(getCommand("iunlock")).setExecutor(new IUnlockCommand());
    }

    @Nullable
    public static UUID getLock(@Nullable ItemStack itemStack) {
        if (itemStack == null) return null;
        net.minecraft.world.item.ItemStack nms = CraftItemStack.asNMSCopy(itemStack);
        if (nms.v() == null) return null;
        NBTTagCompound tag = nms.w();
        if (!tag.e("lockUUID")) return null;
        NBTBase nbt = tag.c("lockUUID");
        if (!(nbt instanceof NBTTagString)) return null;
        try {
            return UUID.fromString(nbt.m_());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static List<String> lore(UUID uuid) {
        return Arrays.asList(
                ChatColor.YELLOW + "データ: " + uuid.toString(),
                "",
                ChatColor.GOLD + "クラフトしたりしてアイテムが変わるとデータが削除されます。",
                ChatColor.GOLD + "ロックされたままの状態にしたい場合は",
                ChatColor.GOLD + "データが削除された後に再度ロックする必要があります。",
                "",
                ChatColor.GOLD + "ロックしている人とは違う人がこのアイテムを",
                ChatColor.GOLD + "持っている場合は、このアイテムを使用したブロックの設置も",
                ChatColor.GOLD + "クラフトもできなくなります。",
                "",
                ChatColor.GREEN + "ロック: " + ChatColor.YELLOW + "/ilock",
                ChatColor.LIGHT_PURPLE + "ロック解除: " + ChatColor.YELLOW + "/iunlock"
        );
    }

    @SuppressWarnings("ConstantConditions")
    public static void addLore(UUID uuid, ItemMeta meta) {
        List<String> lore = new ArrayList<>(meta.hasLore() ? meta.getLore() : new ArrayList<>());
        lore.addAll(lore(uuid));
        meta.setLore(lore);
    }

    @SuppressWarnings("ConstantConditions")
    public static void removeLore(UUID uuid, ItemMeta meta) {
        List<String> lore = new ArrayList<>(meta.hasLore() ? meta.getLore() : new ArrayList<>());
        Collections.reverse(lore);
        List<String> lore2 = lore(uuid);
        Collections.reverse(lore2);
        lore2.forEach(lore::remove);
        Collections.reverse(lore);
        meta.setLore(lore.size() == 0 ? null : lore);
    }

    public static Map.Entry<String, ItemStack> setLock(@NotNull ItemStack itemStack, @NotNull UUID uuid, boolean force) {
        net.minecraft.world.item.ItemStack nms = CraftItemStack.asNMSCopy(itemStack);
        NBTTagCompound tag = nms.w();
        if (tag == null) throw new NullPointerException();
        if (!force && tag.e("lockUUID") && !Objects.requireNonNull(tag.c("lockUUID")).m_().equals(uuid.toString()))
            return new AbstractMap.SimpleEntry<>(ChatColor.RED + "他人のアイテムロックを上書きすることはできません。", itemStack);
        tag.a("lockUUID", NBTTagString.a(uuid.toString()));
        nms.c(tag);
        ItemStack item = CraftItemStack.asBukkitCopy(nms);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        addLore(uuid, meta);
        item.setItemMeta(meta);
        return new AbstractMap.SimpleEntry<>(ChatColor.GREEN + "アイテムロックを設定しました。", item);
    }

    public static Map.Entry<String, ItemStack> unlock(@NotNull ItemStack itemStack, @NotNull UUID uuid, boolean force) {
        net.minecraft.world.item.ItemStack nms = CraftItemStack.asNMSCopy(itemStack);
        NBTTagCompound tag = nms.w();
        if (!force && tag.e("lockUUID") && !Objects.requireNonNull(tag.c("lockUUID")).m_().equals(uuid.toString()))
            return new AbstractMap.SimpleEntry<>(ChatColor.RED + "他人のアイテムロックを削除することはできません。", itemStack);
        tag.r("lockUUID");
        nms.c(tag);
        ItemStack item = CraftItemStack.asBukkitCopy(nms);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        removeLore(uuid, meta);
        item.setItemMeta(meta);
        return new AbstractMap.SimpleEntry<>(ChatColor.GREEN + "アイテムロックを削除しました。", item);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        UUID lock = getLock(e.getItemInHand());
        if (lock == null) return;
        if (!e.getPlayer().getUniqueId().equals(lock)) e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        UUID lock = getLock(e.getItemDrop().getItemStack());
        if (lock == null) return;
        e.getItemDrop().setInvulnerable(true);
        e.getItemDrop().setOwner(lock);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked().getGameMode() == GameMode.CREATIVE) return;
        ItemStack item = e.getCurrentItem();
        if (item == null) return;
        UUID lock = getLock(item);
        if (lock == null) return;
        if (e.getInventory().getType() == InventoryType.FURNACE
                || e.getInventory().getType() == InventoryType.BLAST_FURNACE
                || e.getInventory().getType() == InventoryType.GRINDSTONE
                || e.getInventory().getType() == InventoryType.BEACON
                || e.getInventory().getType() == InventoryType.STONECUTTER
                || e.getInventory().getType() == InventoryType.SMOKER
                || e.getInventory().getType() == InventoryType.LOOM
                || e.getInventory().getType() == InventoryType.ANVIL
                || e.getInventory().getType() == InventoryType.CARTOGRAPHY
                || e.getInventory().getType() == InventoryType.ENCHANTING) {
            if (!e.getWhoClicked().getUniqueId().equals(lock)) e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent e) {
        if (e.getView().getPlayer().getGameMode() == GameMode.CREATIVE) return;
        for (ItemStack item : e.getInventory().getMatrix()) {
            UUID lock = getLock(item);
            if (lock != null && !e.getView().getPlayer().getUniqueId().equals(lock)) e.getInventory().setResult(null);
        }
    }
}
