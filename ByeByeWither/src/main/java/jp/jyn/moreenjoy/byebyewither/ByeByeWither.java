package jp.jyn.moreenjoy.byebyewither;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;

public class ByeByeWither implements Listener {
    private final Set<String> wither = new HashSet<>();
    private final Set<String> dragon = new HashSet<>();

    private ByeByeWither(ConfigurationSection config) {
        wither.addAll(config.getStringList("wither"));
        dragon.addAll(config.getStringList("dragon"));
    }

    public static ByeByeWither onEnable(Plugin plugin, ConfigurationSection config) {
        ByeByeWither instance = new ByeByeWither(config);
        Bukkit.getPluginManager().registerEvents(instance, plugin);
        return instance;
    }

    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (e.getEntityType() != EntityType.WITHER
            || e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.BUILD_WITHER
            || wither.contains(e.getLocation().getWorld().getName())) {
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK
            || e.getMaterial() != Material.END_CRYSTAL
            || e.getClickedBlock().getType() != Material.BEDROCK) {
            return;
        }

        Player player = e.getPlayer();
        World world = player.getWorld();
        if (world.getEnvironment() != World.Environment.THE_END
            || dragon.contains(world.getName())
            || player.hasPermission("moreenjoy.byebyewither.dragon")) {
            return;
        }

        e.setCancelled(true);
    }
}
