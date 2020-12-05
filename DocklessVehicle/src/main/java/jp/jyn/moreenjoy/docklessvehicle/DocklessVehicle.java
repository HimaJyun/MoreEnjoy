package jp.jyn.moreenjoy.docklessvehicle;

import jp.jyn.moreenjoy.utils.ColorConverter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.TreeSpecies;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
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
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DocklessVehicle implements Listener, TabExecutor {
    private final static String PREFIX = "[MoreEnjoy (DocklessVehicle)] ";

    private enum BoatMode {
        ALWAYS,
        NO_PLAYER,
        ONLY_EMPTY,
        FIRST
    }

    private final Plugin plugin;
    private final PluginCommand command;

    private final BoatMode boat;
    private final boolean minecart;
    private final boolean sendToInventory;

    private final String defaultReady;
    private final String defaultStop;
    private final String defaultStart;
    private final Map<String, String> ready = new HashMap<>();
    private final Map<String, String> stop = new HashMap<>();
    private final Map<String, String> start = new HashMap<>();

    private final NamespacedKey persistentKey;
    private final Map<UUID, Function<PersistentDataContainer, Boolean>> dock = new HashMap<>();

    private DocklessVehicle(Plugin plugin, ConfigurationSection config, PluginCommand command) {
        this.plugin = plugin;
        this.command = command;
        this.persistentKey = new NamespacedKey(plugin, "docklessvehicle");

        this.minecart = config.getBoolean("minecart");
        this.sendToInventory = config.getBoolean("sendToInventory");

        switch (config.getString("boat").toLowerCase(Locale.ENGLISH)) {
            case "always":
            case "true":
                boat = BoatMode.ALWAYS;
                break;
            case "player":
            case "no-player":
            case "no_player":
            case "noplayer":
                boat = BoatMode.NO_PLAYER;
                break;
            case "empty":
            case "only-empty":
            case "only_empty":
            case "onlyempty":
                boat = BoatMode.ONLY_EMPTY;
                break;
            case "first":
            case "is-first":
            case "is_first":
            case "isfirst":
                boat = BoatMode.FIRST;
                break;
            default:
                boat = null;
                break;
        }

        String ry = config.getString("ready");
        String sp = config.getString("stop");
        String st = config.getString("start");
        defaultReady = ColorConverter.convert(PREFIX + ry);
        defaultStop = ColorConverter.convert(PREFIX + sp);
        defaultStart = ColorConverter.convert(PREFIX + st);
        for (String key : config.getKeys(false)) { // 実はローカライズできる
            if (config.isConfigurationSection(key)) {
                ready.put(key, ColorConverter.convert(PREFIX + config.getString(key + ".ready", ry)));
                stop.put(key, ColorConverter.convert(PREFIX + config.getString(key + ".stop", sp)));
                start.put(key, ColorConverter.convert(PREFIX + config.getString(key + ".start", st)));
            }
        }
    }

    public static DocklessVehicle onEnable(Plugin plugin, ConfigurationSection config, PluginCommand command) {
        DocklessVehicle instance = new DocklessVehicle(plugin, config, command);
        Bukkit.getPluginManager().registerEvents(instance, plugin);
        if (!instance.minecart) {
            VehicleDestroyEvent.getHandlerList().unregister(instance);
        }
        command.setExecutor(instance);
        return instance;
    }

    public void onDisable() {
        command.setExecutor(plugin);
        HandlerList.unregisterAll(this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleExitEvent(VehicleExitEvent e) {
        Entity entity = e.getExited();
        if (entity.getType() != EntityType.PLAYER) {
            return;
        }

        Vehicle vehicle = e.getVehicle();
        switch (vehicle.getType()) {
            case BOAT:
                if (boat != null && !isPersistent(vehicle)) {
                    onBoatExit((Boat) vehicle, (Player) entity);
                }
                return;
            case MINECART:
                if (minecart && !isPersistent(vehicle)) {
                    onMinecartExit(vehicle, (Player) entity);
                }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleDestroyEvent(VehicleDestroyEvent e) {
        // 乗ったままサボテンとかに突っ込むと出る
        // 突っ込む相手(ダメージ？)によってisDead()の出方が変わる
        // トロッコはこれがないとアイテムが増殖するが、逆にボートはこれがあるとアイテムが増殖する
        Vehicle vehicle = e.getVehicle();
        if (vehicle.getType() != EntityType.MINECART) {
            return;
        }

        Player player = null;
        for (Entity entity : vehicle.getPassengers()) {
            if (entity.getType() == EntityType.PLAYER) {
                player = (Player) entity;
                break;
            }
        }

        if (player == null || !minecart || isPersistent(vehicle)) {
            return;
        }

        dropItem(Material.MINECART, vehicle.getLocation());
        vehicle.remove();
        e.setCancelled(true);
    }

    private void onBoatExit(Boat vehicle, Player player) {
        // 回収済みボート
        if (vehicle.isDead()) {
            return;
        }

        switch (boat) {
            case FIRST:
                if (!vehicle.getPassengers().get(0).equals(player)) {
                    return;
                }
                break;
            case NO_PLAYER:
                List<Entity> passengers = vehicle.getPassengers();
                if (passengers.size() <= 1) {
                    break;
                }
                for (Entity entity : passengers) {
                    if (entity.getType() != EntityType.PLAYER
                        || entity.equals(player)) {
                        continue;
                    }
                    return;
                }
                break;
            case ONLY_EMPTY:
                if (vehicle.getPassengers().size() > 1) {
                    return;
                }
                break;
        }

        Material material = getBoatType(vehicle.getWoodType());
        if (!sendToInventory || !addInventory(material, player.getInventory())) {
            dropItem(material, vehicle.getLocation());
        }
        vehicle.remove();
    }

    private void onMinecartExit(Vehicle vehicle, Player player) {
        if (vehicle.isDead()) {
            return;
        }

        if (!sendToInventory || !addInventory(Material.MINECART, player.getInventory())) {
            dropItem(Material.MINECART, vehicle.getLocation());
        }
        vehicle.remove();
    }

    private void dropItem(Material material, Location location) {
        location.getWorld().dropItemNaturally(location, new ItemStack(material));
    }

    private boolean addInventory(Material material, Inventory inventory) {
        int i = inventory.firstEmpty();
        if (i == -1) {
            return false;
        }
        inventory.setItem(i, new ItemStack(material));
        return true;
    }

    private Material getBoatType(TreeSpecies tree) {
        switch (tree) {
            case BIRCH:
                return Material.BIRCH_BOAT;
            case ACACIA:
                return Material.ACACIA_BOAT;
            case JUNGLE:
                return Material.JUNGLE_BOAT;
            case REDWOOD:
                return Material.SPRUCE_BOAT;
            case DARK_OAK:
                return Material.DARK_OAK_BOAT;
            default:
                return Material.OAK_BOAT;
        }
    }

    private boolean isPersistent(Entity entity) {
        return entity.getPersistentDataContainer().has(persistentKey, PersistentDataType.BYTE);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "This command can only be run by players.");
            return true;
        }
        if (!sender.hasPermission("moreenjoy.docklessvehicle.dock")) {
            sender.sendMessage(PREFIX + ChatColor.RED + "You don't have permission!");
            return true;
        }

        Function<PersistentDataContainer, Boolean> operator = meta -> {
            if (meta.has(persistentKey, PersistentDataType.BYTE)) {
                meta.remove(persistentKey);
                return false;
            } else {
                meta.set(persistentKey, PersistentDataType.BYTE, (byte) 1);
                return true;
            }
        };
        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("true")) {
                operator = meta -> {
                    meta.set(persistentKey, PersistentDataType.BYTE, (byte) 1);
                    return true;
                };
            } else if (args[0].equalsIgnoreCase("false")) {
                operator = meta -> {
                    meta.remove(persistentKey);
                    return false;
                };
            } else {
                return false;
            }
        }

        Player player = (Player) sender;
        dock.put(player.getUniqueId(), operator);
        player.sendMessage(ready.getOrDefault(player.getLocale(), defaultReady));
        return true;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityInteractEvent(VehicleEnterEvent e) {
        Entity entity = e.getEntered();
        if (entity.getType() != EntityType.PLAYER) {
            return;
        }

        Vehicle vehicle = e.getVehicle();
        switch (vehicle.getType()) {
            case BOAT:
            case MINECART:
                break;
            default:
                return;
        }

        Player player = (Player) entity;
        Function<PersistentDataContainer, Boolean> operator = dock.remove(player.getUniqueId());
        if (operator == null) {
            return;
        }

        if (operator.apply(vehicle.getPersistentDataContainer())) {
            player.sendMessage(stop.getOrDefault(player.getLocale(), defaultStop));
        } else {
            player.sendMessage(start.getOrDefault(player.getLocale(), defaultStart));
        }
        e.setCancelled(true);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Stream.of("true", "false").filter(s -> s.startsWith(args[0])).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
