package jp.jyn.moreenjoy.crystalguard;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;

public class CrystalGuard implements Listener {
    private final Set<String> damage = new HashSet<>();
    private final Set<String> break_ = new HashSet<>();

    private CrystalGuard(ConfigurationSection config) {
        this.damage.addAll(config.getStringList("damage"));
        this.break_.addAll(config.getStringList("break"));
    }

    public static CrystalGuard onEnable(Plugin plugin, ConfigurationSection config) {
        CrystalGuard instance = new CrystalGuard(config);
        Bukkit.getPluginManager().registerEvents(instance, plugin);
        return instance;
    }

    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        // on damage, off block break
        if (e.getEntityType() != EntityType.ENDER_CRYSTAL) {
            return;
        }

        Location loc = e.getLocation();
        World world = loc.getWorld();
        if (!break_.contains(world.getName())) {
            e.setCancelled(true);

            // fake explosion
            world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 4.0f, 0.7f);
            world.spawnParticle(Particle.EXPLOSION_HUGE, loc, 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        // off explode (壊せなくなる)
        //if (e.getEntityType() == EntityType.ENDER_CRYSTAL)e.setCancelled(true);

        // off damage, on block break
        if (e.getDamager().getType() == EntityType.ENDER_CRYSTAL
            && !damage.contains(e.getDamager().getLocation().getWorld().getName())) {
            e.setCancelled(true);
        }
    }
}
