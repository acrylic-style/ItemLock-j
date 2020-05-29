package xyz.acrylicstyle.itemLock.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import util.CollectionSet;
import xyz.acrylicstyle.itemLock.ItemLock;
import xyz.acrylicstyle.shared.BaseMojangAPI;
import xyz.acrylicstyle.tomeito_api.command.PlayerOpCommandExecutor;

import java.util.UUID;

public class IFindCommand extends PlayerOpCommandExecutor {
    @Override
    public void onCommand(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "/ifind <player> " + ChatColor.GRAY + "- " + ChatColor.AQUA + "現在オンラインのプレイヤーで<player>のデータが保持されているアイテムを持っているプレイヤーを取得します。");
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                UUID uuid = BaseMojangAPI.getUniqueId(args[0]);
                if (uuid == null) {
                    player.sendMessage(ChatColor.RED + "プレイヤーが無効です。");
                    return;
                }
                CollectionSet<String> players = new CollectionSet<>();
                Bukkit.getOnlinePlayers().forEach(p -> {
                    for (ItemStack item : p.getInventory().getContents()) if (uuid.equals(ItemLock.getLock(item))) players.add(p.getName());
                });
                if (players.size() == 0) {
                    player.sendMessage(ChatColor.GREEN + "アイテムを持っているプレイヤーが見つかりませんでした。");
                    return;
                }
                player.sendMessage(ChatColor.GREEN + "現在" + ChatColor.YELLOW + players.join(", ") + ChatColor.GREEN + "がアイテムを所持しています。");
            }
        }.runTaskAsynchronously(ItemLock.instance);
    }
}
