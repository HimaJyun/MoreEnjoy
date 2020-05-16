package jp.jyn.moreenjoy.novoid;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class NoVoid implements Listener {
    private final Set<String> exclude = new HashSet<>();

    private NoVoid(Collection<String> exclude) {
        for (String s : exclude) {
            this.exclude.add(s.toLowerCase(Locale.ENGLISH));
        }
    }

    public static NoVoid onEnable(Plugin plugin, ConfigurationSection config) {
        NoVoid instance = new NoVoid(config.getStringList("exclude"));
        Bukkit.getServer().getPluginManager().registerEvents(instance, plugin);
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
        World world = player.getWorld();

        // exclude check
        if (exclude.contains(world.getName().toLowerCase(Locale.ENGLISH))) {
            return;
        }

        // no fall damage
        PotionEffect potion = player.getPotionEffect(PotionEffectType.SLOW_FALLING);
        if (potion == null) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 2, 0));
        }

        // teleport and cancel
        player.teleport(world.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        e.setCancelled(true);
    }
}
