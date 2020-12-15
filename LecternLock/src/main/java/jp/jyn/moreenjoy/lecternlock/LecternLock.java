package jp.jyn.moreenjoy.lecternlock;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class LecternLock implements Listener {
    private LecternLock() {}

    public static LecternLock onEnable(Plugin plugin) {
        LecternLock instance = new LecternLock();
        Bukkit.getPluginManager().registerEvents(instance, plugin);
        return instance;
    }

    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTakeLecternBookEvent(PlayerTakeLecternBookEvent e) {
        ItemStack item = e.getBook();
        if (item == null) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof BookMeta)) {
            return;
        }

        BookMeta book = (BookMeta) meta;
        if (!book.hasAuthor()) {
            return;
        }

        String author = book.getAuthor();
        if (author == null) {
            return;
        }

        Player player = e.getPlayer();
        if (player.hasPermission("moreenjoy.lecternlock.passthrough")) {
            return;
        }
        
        if (!author.equals(e.getPlayer().getName())) {
            e.setCancelled(true);
        }
    }
}
