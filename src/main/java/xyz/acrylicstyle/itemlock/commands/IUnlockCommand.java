package xyz.acrylicstyle.itemlock.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.acrylicstyle.itemlock.ItemLock;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class IUnlockCommand implements PlayerCommandExecutor, TabCompleter {
    @Override
    public void onCommand(Player player, String[] args) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "このアイテムはロック解除できません。");
            return;
        }
        boolean force = false;
        if (args.length > 0 && args[0].equalsIgnoreCase("force")) {
            if (player.hasPermission("itemlock.op")) force = true;
        }
        Map.Entry<String, ItemStack> entry = ItemLock.unlock(item, player.getUniqueId(), force);
        player.getInventory().setItemInMainHand(entry.getValue());
        player.sendMessage(entry.getKey());
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
