package xyz.acrylicstyle.itemLock.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.acrylicstyle.itemLock.ItemLock;
import xyz.acrylicstyle.tomeito_api.command.PlayerCommandExecutor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ILockCommand extends PlayerCommandExecutor implements TabCompleter {
    @Override
    public void onCommand(Player player, String[] args) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "このアイテムはロックできません。");
            return;
        }
        boolean force = false;
        if (args.length > 0 && args[0].equalsIgnoreCase("force")) {
            if (player.isOp()) force = true;
        }
        Map.Entry<String, ItemStack> entry = ItemLock.setLock(item, player.getUniqueId(), force);
        player.getInventory().setItemInMainHand(entry.getValue());
        player.sendMessage(entry.getKey());
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
