package jp.jyn.moreenjoy.docklessvehicle;

import jp.jyn.moreenjoy.utils.ColorConverter;
import jp.jyn.moreenjoy.utils.PersistentMoreType;
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
    private enum BoatMode {
        ALWAYS("always", "true") {
            @Override
            public boolean check(Vehicle boat, Entity player) {
                return true;
            }
        },
        NO_PLAYER("player", "no-player", "no_player", "noplayer") {
            @Override
            boolean check(Vehicle boat, Entity player) {
                List<Entity> passengers = boat.getPassengers();
                if (passengers.size() < 2) {
                    return true;
                }
                for (Entity entity : passengers) {
                    if (entity.getType() == EntityType.PLAYER && !entity.equals(player)) {
                        return false;
                    }
                }
                return true;
            }
        },
        ONLY_EMPTY("empty", "only-empty", "only_empty", "onlyempty") {
            @Override
            boolean check(Vehicle boat, Entity player) {
                return boat.getPassengers().size() < 2;
            }
        },
        FIRST("first", "is-first", "is_first", "isfirst") {
            @Override
            boolean check(Vehicle boat, Entity player) {
                return boat.getPassengers().get(0).equals(player);
            }
        };

        private final static Map<String, BoatMode> STR_MODE;
        private String[] tmp;

        static {
            Map<String, BoatMode> result = new HashMap<>();
            for (BoatMode value : values()) {
                for (String key : value.tmp) {
                    result.put(key, value);
                }
                value.tmp = null;
            }
            STR_MODE = Collections.unmodifiableMap(result);
        }

        BoatMode(String... key) {
            this.tmp = key;
        }

        /* package */
        abstract boolean check(Vehicle boat, Entity player);
    }

    private final static String PREFIX = "[MoreEnjoy (DocklessVehicle)] ";

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

        this.minecart = config.getBoolean("minecart");
        this.sendToInventory = config.getBoolean("sendToInventory");
        this.boat = BoatMode.STR_MODE.get(config.getString("boat").toLowerCase(Locale.ENGLISH));
        this.persistentKey = new NamespacedKey(plugin, "docklessvehicle");

        String ry = config.getString("ready");
        String sp = config.getString("stop");
        String st = config.getString("start");
        defaultReady = ColorConverter.convert(PREFIX + ry);
        defaultStop = ColorConverter.convert(PREFIX + sp);
        defaultStart = ColorConverter.convert(PREFIX + st);
        for (String key : config.getKeys(false)) { // 実はローカライズできる
            if (config.isConfigurationSection(key)) {
                ConfigurationSection locale = config.getConfigurationSection(key);
                ready.put(key, ColorConverter.convert(PREFIX + locale.getString("ready", ry)));
                stop.put(key, ColorConverter.convert(PREFIX + locale.getString("stop", sp)));
                start.put(key, ColorConverter.convert(PREFIX + locale.getString("start", st)));
            }
        }
    }

    public static DocklessVehicle onEnable(Plugin plugin, ConfigurationSection config, PluginCommand command) {
        DocklessVehicle instance = new DocklessVehicle(plugin, config, command);
        Bukkit.getPluginManager().registerEvents(instance, plugin);
        if (!instance.minecart) {
            VehicleDestroyEvent.getHandlerList().unregister(instance);
        }

        command.setDescription("Stop automatic collection.");
        command.setPermission("moreenjoy.docklessvehicle.dock");
        command.setExecutor(instance);
        command.setTabCompleter(instance);
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
        Material material;
        switch (vehicle.getType()) {
            case BOAT:
                if (boat != null
                    && !vehicle.isDead()
                    && !isPersistent(vehicle)
                    && boat.check(vehicle, entity)) {
                    Boat boat = (Boat) vehicle;
                    autoCollect(getBoatType(boat.getWoodType()), vehicle, (Player) entity);
                }
                break;
            case MINECART:
                if (minecart
                    && !vehicle.isDead()
                    && !isPersistent(vehicle)) {
                    autoCollect(Material.MINECART, vehicle, (Player) entity);
                }
                break;
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

        if (player != null && minecart && !isPersistent(vehicle)) {
            Location location = vehicle.getLocation();
            location.getWorld().dropItemNaturally(location, new ItemStack(Material.MINECART));
            vehicle.remove();
            e.setCancelled(true);
        }
    }

    private void autoCollect(Material material, Vehicle vehicle, Player player) {
        ItemStack item = new ItemStack(material);
        if (sendToInventory) {
            Inventory inventory = player.getInventory();
            int i = inventory.firstEmpty();
            if (i != -1) {
                inventory.setItem(i, item);
                vehicle.remove();
                return;
            }
        }

        Location location = vehicle.getLocation();
        location.getWorld().dropItemNaturally(location, item);
        vehicle.remove();
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
        return entity.getPersistentDataContainer().has(persistentKey, PersistentMoreType.BOOLEAN);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "This command can only be run by players.");
            return true;
        }

        Function<PersistentDataContainer, Boolean> operator = args.length == 0
            ? meta -> meta.has(persistentKey, PersistentMoreType.BOOLEAN) ? removePersist(meta) : setPersist(meta)
            : args[0].equalsIgnoreCase("true") ? this::setPersist
            : args[0].equalsIgnoreCase("false") ? this::removePersist
            : null;
        if (operator == null) {
            return false;
        }

        Player player = (Player) sender;
        dock.put(player.getUniqueId(), operator);
        player.sendMessage(ready.getOrDefault(player.getLocale(), defaultReady));
        return true;
    }

    private boolean setPersist(PersistentDataContainer meta) {
        meta.set(persistentKey, PersistentMoreType.BOOLEAN, Boolean.TRUE);
        return true;
    }

    private boolean removePersist(PersistentDataContainer meta) {
        meta.remove(persistentKey);
        return false;
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

        player.sendMessage(operator.apply(vehicle.getPersistentDataContainer())
            ? stop.getOrDefault(player.getLocale(), defaultStop)
            : start.getOrDefault(player.getLocale(), defaultStart)
        );
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
