package jp.jyn.moreenjoy.editsign;

import jp.jyn.moreenjoy.utils.ColorConverter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.regex.Pattern;

public class EditSign implements Listener, TabExecutor {
    public final static String PREFIX = "[MoreEnjoy (EditSign)] ";
    private final static String VARIABLE = "\\{\\s*value\\s*}";

    private final Plugin plugin;
    private final PluginCommand command;

    private final String defaultError;
    private final String[] defaultReady;
    private final Map<String, String> error = new HashMap<>();
    private final Map<String, String[]> ready = new HashMap<>();

    private final Map<UUID, Map.Entry<Integer, String>> edit = new HashMap<>();

    private EditSign(Plugin plugin, ConfigurationSection config, PluginCommand command) {
        this.plugin = plugin;
        this.command = command;
        this.command.setPermission("moreenjoy.editsign.edit");
        this.command.setDescription("Edit sign.");
        this.command.setAliases(Collections.singletonList("editsign"));

        Pattern v = Pattern.compile(VARIABLE);
        String e = config.getString("error");
        String r = config.getString("ready");
        defaultError = ColorConverter.convert(PREFIX + e);
        defaultReady = v.split(ColorConverter.convert(PREFIX + r), -1);
        // 実はローカライズできる
        for (String key : config.getKeys(false)) {
            if (config.isConfigurationSection(key)) {
                error.put(key, ColorConverter.convert(PREFIX + config.getString(key + ".error", e)));
                ready.put(key, v.split(ColorConverter.convert(PREFIX + config.getString(key + ".ready", r)), -1));
            }
        }
    }

    public static EditSign onEnable(Plugin plugin, ConfigurationSection config, PluginCommand command) {
        EditSign instance = new EditSign(plugin, config, command);
        command.setExecutor(instance);
        command.setTabCompleter(instance);
        Bukkit.getPluginManager().registerEvents(instance, plugin);
        // 要らない方を登録解除
        if (config.getBoolean("safety")) {
            PlayerInteractEvent.getHandlerList().unregister(instance);
        } else {
            BlockBreakEvent.getHandlerList().unregister(instance);
        }
        return instance;
    }

    public void onDisable() {
        HandlerList.unregisterAll(this);
        command.setTabCompleter(plugin);
        command.setExecutor(plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEvent(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (apply(e.getPlayer(), e.getClickedBlock())) {
            e.setCancelled(true);
        }
    }

    // EventPriority.HIGHにしておけば後で発動する==保護系プラグインが先に発動するはず
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreakEvent(BlockBreakEvent e) {
        if (apply(e.getPlayer(), e.getBlock())) {
            e.setCancelled(true);
        }
    }

    private boolean apply(Player player, Block block) {
        BlockState state = block.getState();
        if (!(state instanceof Sign)) {
            return false;
        }

        Map.Entry<Integer, String> m = edit.remove(player.getUniqueId());
        if (m == null) {
            return false;
        }

        Sign sign = (Sign) state;
        sign.setLine(m.getKey(), m.getValue());
        sign.update();
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "This command can only be run by players.");
            return true;
        }

        Player player = (Player) sender;
        if (args.length < 2) {
            player.sendMessage(error.getOrDefault(player.getLocale(), defaultError));
            return false;
        }

        int page;
        try {
            page = Integer.parseInt(args[0]);
            if (page < 1 || page > 4) {
                player.sendMessage(error.getOrDefault(player.getLocale(), defaultError));
                return false;
            }
        } catch (NumberFormatException ignore) {
            player.sendMessage(error.getOrDefault(player.getLocale(), defaultError));
            return false;
        }

        StringJoiner j = new StringJoiner(" ");
        for (int i = 1; i < args.length; i++) {
            j.add(args[i]);
        }

        String newValue = ColorConverter.convert(j.toString());
        edit.put(
            player.getUniqueId(),
            new AbstractMap.SimpleImmutableEntry<>(page - 1, newValue)
        );
        player.sendMessage(String.join(newValue, ready.getOrDefault(player.getLocale(), defaultReady)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && args[0].isEmpty()) {
            return Arrays.asList("1", "2", "3", "4");
        }
        return Collections.emptyList();
    }
}
