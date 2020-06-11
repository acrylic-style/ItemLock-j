package xyz.acrylicstyle.itemLock;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
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
import util.CollectionList;
import util.ICollectionList;
import xyz.acrylicstyle.itemLock.commands.IFindCommand;
import xyz.acrylicstyle.itemLock.commands.ILockCommand;
import xyz.acrylicstyle.itemLock.commands.IUnlockCommand;
import xyz.acrylicstyle.paper.Paper;
import xyz.acrylicstyle.paper.inventory.ItemStackUtils;
import xyz.acrylicstyle.paper.nbt.NBTBase;
import xyz.acrylicstyle.paper.nbt.NBTTagCompound;
import xyz.acrylicstyle.paper.nbt.NBTTagString;
import xyz.acrylicstyle.tomeito_api.TomeitoAPI;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ItemLock extends JavaPlugin implements Listener {
    public static ItemLock instance = null;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        TomeitoAPI.registerCommand("ilock", new ILockCommand());
        TomeitoAPI.registerCommand("iunlock", new IUnlockCommand());
        TomeitoAPI.registerCommand("ifind", new IFindCommand());
    }

    @Nullable
    public static UUID getLock(@Nullable ItemStack itemStack) {
        if (itemStack == null) return null;
        if (!Paper.itemStack(itemStack).hasTag()) return null;
        NBTTagCompound tag = Objects.requireNonNull(Paper.itemStack(itemStack).getTag());
        if (!tag.getMap().containsKey("lockUUID")) return null;
        NBTBase nbt = tag.getMap().get("lockUUID");
        if (!(nbt instanceof NBTTagString)) return null;
        try {
            return UUID.fromString(nbt.asString());
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
        CollectionList<String> lore = ICollectionList.asList(meta.hasLore() ? meta.getLore() : new ArrayList<>());
        lore.addAll(lore(uuid));
        meta.setLore(lore);
    }

    @SuppressWarnings("ConstantConditions")
    public static void removeLore(UUID uuid, ItemMeta meta) {
        CollectionList<String> lore = ICollectionList.asList(meta.hasLore() ? meta.getLore() : new ArrayList<>()).reverse();
        ICollectionList.asList(lore(uuid)).reverse().forEach(lore::remove);
        meta.setLore(lore.size() == 0 ? null : lore.reverse());
    }

    public static Map.Entry<String, ItemStack> setLock(@NotNull ItemStack itemStack, @NotNull UUID uuid, boolean force) {
        NBTTagCompound tag = Paper.itemStack(itemStack).hasTag() ? Paper.itemStack(itemStack).getTag() : Paper.itemStack(itemStack).getOrCreateTag();
        if (tag == null) throw new NullPointerException();
        if (!force && tag.getMap().containsKey("lockUUID") && !tag.getMap().get("lockUUID").asString().equals(uuid.toString()))
            return new AbstractMap.SimpleEntry<>(ChatColor.RED + "他人のアイテムロックを上書きすることはできません。", itemStack);
        tag.getMap().put("lockUUID", new NBTTagString(uuid.toString()));
        ItemStackUtils utils = Paper.itemStack(itemStack);
        utils.setTag(tag);
        ItemStack item = utils.getItemStack();
        ItemMeta meta = item.getItemMeta();
        addLore(uuid, meta);
        item.setItemMeta(meta);
        return new AbstractMap.SimpleEntry<>(ChatColor.GREEN + "アイテムロックを設定しました。", item);
    }

    public static Map.Entry<String, ItemStack> unlock(@NotNull ItemStack itemStack, @NotNull UUID uuid, boolean force) {
        NBTTagCompound tag = Paper.itemStack(itemStack).getOrCreateTag();
        if (!force && tag.getMap().containsKey("lockUUID") && !tag.getMap().get("lockUUID").asString().equals(uuid.toString()))
            return new AbstractMap.SimpleEntry<>(ChatColor.RED + "他人のアイテムロックを削除することはできません。", itemStack);
        tag.getMap().remove("lockUUID");
        ItemStackUtils utils = Paper.itemStack(itemStack);
        utils.setTag(tag);
        ItemStack item = utils.getItemStack();
        ItemMeta meta = item.getItemMeta();
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
        e.getItemDrop().setCanMobPickup(false);
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
        if (e.getPlayer() == null) return;
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE) return;
        for (ItemStack item : e.getInventory().getMatrix()) {
            UUID lock = getLock(item);
            if (lock != null && !e.getPlayer().getUniqueId().equals(lock)) e.getInventory().setResult(null);
        }
    }
}
