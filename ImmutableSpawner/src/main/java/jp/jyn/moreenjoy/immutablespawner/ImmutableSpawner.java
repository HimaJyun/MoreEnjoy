package jp.jyn.moreenjoy.immutablespawner;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;
import java.util.Set;

public class ImmutableSpawner implements Listener {
    private final Set<Material> egg = EnumSet.noneOf(Material.class);

    private ImmutableSpawner() {
        for (Material material : Material.values()) {
            if (material.name().endsWith("_SPAWN_EGG")) {
                egg.add(material);
            }
        }
    }

    public static ImmutableSpawner onEnable(Plugin plugin) {
        ImmutableSpawner instance = new ImmutableSpawner();
        Bukkit.getPluginManager().registerEvents(instance, plugin);
        return instance;
    }

    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK
            || e.getClickedBlock().getType() != Material.SPAWNER
            || !egg.contains(e.getMaterial())
            || e.getPlayer().hasPermission("moreenjoy.immutablespawner.allow")) {
            return;
        }
        e.setCancelled(true);
    }

}
