package xyz.acrylicstyle.itemlock.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public interface PlayerCommandExecutor extends CommandExecutor {
    @Override
    default boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command cannot be executed from console.");
            return true;
        }
        onCommand((Player) sender, args);
        return true;
    }

    void onCommand(Player player, String[] args);
}
