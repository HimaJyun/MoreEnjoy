package jp.jyn.moreenjoy.ridenow;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.TreeSpecies;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RideNow implements TabCompleter, Listener {
    public final static String PREFIX = "[MoreEnjoy (RideNow)] ";

    private final Plugin plugin;
    private final PluginCommand boat;
    private final PluginCommand minecart;

    private final NamespacedKey key;

    private RideNow(Plugin plugin, PluginCommand boat, PluginCommand minecart) {
        this.plugin = plugin;
        this.boat = boat;
        this.minecart = minecart;

        this.key = new NamespacedKey(plugin, "ridenow");

        boat.setPermission("moreenjoy.ridenow.boat");
        minecart.setPermission("moreenjoy.ridenow.minecart");
        boat.setDescription("Ride boat.");
        minecart.setDescription("Ride minecart.");

        boat.setExecutor((s, c, l, a) -> isServer(s) || ride((Player) s, EntityType.BOAT));
        minecart.setExecutor((s, c, l, a) -> isServer(s) || ride((Player) s, EntityType.MINECART));
        boat.setTabCompleter(this);
        minecart.setTabCompleter(this);
    }

    public static RideNow onEnable(Plugin plugin, PluginCommand boat, PluginCommand minecart) {
        RideNow instance = new RideNow(plugin, boat, minecart);
        Bukkit.getPluginManager().registerEvents(instance, plugin);
        return instance;
    }

    public void onDisable() {
        minecart.setTabCompleter(plugin);
        boat.setTabCompleter(plugin);
        minecart.setExecutor(plugin);
        boat.setExecutor(plugin);
        HandlerList.unregisterAll(this);
    }

    private boolean isServer(CommandSender sender) {
        if (sender instanceof Player) {
            return false;
        } else {
            sender.sendMessage(PREFIX + ChatColor.RED + "This command can only be run by players.");
            return true;
        }
    }

    private boolean ride(Player player, EntityType type) {
        Location location = player.getLocation();
        // 既に乗ってる時に更に乗ると少しずつ沈んでいくので乗り物による下降分だけ補正掛ける
        Entity ride = player.getVehicle();
        if (ride != null) {
            switch (ride.getType()) {
                case BOAT:
                    location.setY(location.getY() + 0.55);
                    break;
                case MINECART:
                    location.setY(location.getY() + 0.35);
                    break;
            }
        }

        Vehicle vehicle = (Vehicle) location.getWorld().spawnEntity(location, type);
        vehicle.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        vehicle.addPassenger(player);

        // Random wood type.
        if (type == EntityType.BOAT) {
            TreeSpecies[] t = TreeSpecies.values();
            ((Boat) vehicle).setWoodType(t[ThreadLocalRandom.current().nextInt(t.length)]);
        }
        return true;
    }

    // EventPriority.LOW == 先に出る、先に消してignoreCancelledによるスルーを期待する
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onVehicleExitEvent(VehicleExitEvent e) {
        if (e.getExited().getType() != EntityType.PLAYER) {
            return;
        }

        Vehicle vehicle = e.getVehicle();
        if (vehicle.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
            vehicle.remove();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onVehicleDestroyEvent(VehicleDestroyEvent e) {
        Vehicle vehicle = e.getVehicle();
        if (vehicle.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
            vehicle.remove();
            e.setCancelled(true);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
