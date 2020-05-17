package jp.jyn.moreenjoy.ridenow;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.TreeSpecies;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class RideNow implements TabExecutor, Listener {
    private final static String PREFIX = "[MoreEnjoy (RideNow)] ";
    private final static String METADATA_KEY = "moreenjoy.ridenow";

    private final Plugin plugin;
    private final PluginCommand boat;
    private final PluginCommand minecart;

    private final Random random = new Random();

    private RideNow(Plugin plugin, PluginCommand boat, PluginCommand minecart) {
        this.plugin = plugin;
        this.boat = boat;
        this.minecart = minecart;
    }

    // TODO: 乗り物別にインナークラスを作った方が良いかも
    public static RideNow onEnable(Plugin plugin, PluginCommand boat, PluginCommand minecart) {
        Objects.requireNonNull(plugin);
        if (boat == null && minecart == null) {
            throw new NullPointerException("boat and minecraft is null.");
        }

        RideNow instance = new RideNow(plugin, boat, minecart);
        if (boat != null) {
            boat.setExecutor(instance);
            boat.setTabCompleter(instance);
        }
        if (minecart != null) {
            minecart.setExecutor(instance);
            minecart.setTabCompleter(instance);
        }

        Bukkit.getPluginManager().registerEvents(instance, plugin);
        return instance;
    }

    public void onDisable() {
        HandlerList.unregisterAll(this);

        if (minecart != null) {
            minecart.setTabCompleter(plugin);
            minecart.setExecutor(plugin);
        }
        if (boat != null) {
            boat.setTabCompleter(plugin);
            boat.setExecutor(plugin);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "This command can only be run by players.");
            return true;
        }

        EntityType type;
        if (boat != null && command.getName().equalsIgnoreCase(boat.getName())) {
            if (!sender.hasPermission("moreenjoy.ridenow.boat")) {
                sender.sendMessage(PREFIX + ChatColor.RED + "You don't have permission!");
                return true;
            }
            type = EntityType.BOAT;
        } else if (minecart != null && command.getName().equalsIgnoreCase(minecart.getName())) {
            if (!sender.hasPermission("moreenjoy.ridenow.minecart")) {
                sender.sendMessage(PREFIX + ChatColor.RED + "You don't have permission!");
                return true;
            }
            type = EntityType.MINECART;
        } else {
            throw new IllegalStateException("Unknown command.");
        }

        Player player = (Player) sender;

        // TODO: 目の前に召喚する方が良いかも
        Vehicle vehicle = (Vehicle) player.getWorld().spawnEntity(player.getLocation(), type);
        vehicle.addPassenger(player);

        // Random wood type.
        if (type == EntityType.BOAT) {
            TreeSpecies tree = TreeSpecies.values()[random.nextInt(TreeSpecies.values().length)];
            ((Boat) vehicle).setWoodType(tree);
        }

        vehicle.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, Boolean.TRUE));
        return true;
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleExitEvent(VehicleExitEvent e) {
        if (e.getExited().getType() != EntityType.PLAYER) {
            return;
        }

        Vehicle vehicle = e.getVehicle();
        if (!vehicle.hasMetadata(METADATA_KEY)) {
            return;
        }

        vehicle.remove();
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleDestroyEvent(VehicleDestroyEvent e) {
        Vehicle vehicle = e.getVehicle();
        if (!vehicle.hasMetadata(METADATA_KEY)) {
            return;
        }
        vehicle.remove();
        e.setCancelled(true);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
