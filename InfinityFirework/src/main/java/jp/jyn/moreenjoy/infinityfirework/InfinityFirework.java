package jp.jyn.moreenjoy.infinityfirework;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Dispenser;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.plugin.Plugin;

public class InfinityFirework implements Listener {
    private InfinityFirework() {}

    public static InfinityFirework onEnable(Plugin plugin) {
        InfinityFirework instance = new InfinityFirework();
        Bukkit.getPluginManager().registerEvents(instance, plugin);
        return instance;
    }

    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDispenseEvent(BlockDispenseEvent e) {
        if (e.getItem().getType() != Material.FIREWORK_ROCKET || !(e.getBlock().getState() instanceof Dispenser)) {
            return;
        }

        ((Dispenser) e.getBlock().getState()).getInventory().addItem(e.getItem().clone());
    }
}
