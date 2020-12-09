package jp.jyn.moreenjoy.novoid;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;

public class NoVoid implements Listener {
    private final Plugin plugin;
    private final Set<String> exclude = new HashSet<>();

    private NoVoid(Plugin plugin, ConfigurationSection config) {
        this.plugin = plugin;
        exclude.addAll(config.getStringList("exclude"));
    }

    public static NoVoid onEnable(Plugin plugin, ConfigurationSection config) {
        NoVoid instance = new NoVoid(plugin, config);
        Bukkit.getPluginManager().registerEvents(instance, plugin);
        return instance;
    }

    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageEvent(EntityDamageEvent e) {
        // only for Player void damage
        if (e.getCause() != EntityDamageEvent.DamageCause.VOID
            || e.getEntityType() != EntityType.PLAYER) {
            return;
        }

        Player player = (Player) e.getEntity();
        if (exclude.contains(player.getWorld().getName())          // exclude
            || player.getLocation().getBlockY() > 0) { // kill
            return;
        }

        // stop falling.
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.setFallDistance(0);
            player.teleport(player.getWorld().getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        });
        e.setCancelled(true);
    }
}
