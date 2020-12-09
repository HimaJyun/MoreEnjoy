package jp.jyn.moreenjoy.thisworld;

import jp.jyn.moreenjoy.utils.ColorConverter;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThisWorld implements TabExecutor, Listener {
    private final static String PREFIX = "[MoreEnjoy (ThisWorld)] ";

    private final Plugin plugin;
    private final PluginCommand command;

    private final String unavailable;
    private final Map<String, TitleConfig> defaultTitle = new HashMap<>();
    private final Map<String, Map<String, TitleConfig>> title = new HashMap<>();

    private ThisWorld(Plugin plugin, ConfigurationSection config, PluginCommand command) {
        this.plugin = plugin;
        this.command = command;

        this.unavailable = ColorConverter.convert(config.getString("unavailable"));

        TitleConfig def = TitleConfig.initDefault(
            config.getInt("time.faedin"), config.getInt("time.stay"), config.getInt("time.fadeout"),
            config.getInt("time.delay")
        );

        ConfigurationSection d = config.getConfigurationSection("title");
        for (String key : d.getKeys(false)) {
            defaultTitle.put(key, TitleConfig.loadConfig(d.getConfigurationSection(key), def));
        }

        // 実はローカライズできる
        for (String locale : config.getKeys(false)) {
            if (!config.isConfigurationSection(locale)
                || locale.equals("time") || locale.equals("title")) {
                continue;
            }
            ConfigurationSection l = config.getConfigurationSection(locale);
            // コピーしてから上書き -> デフォルトにあってローカライズにない物はそのまま移される、そうでない物は上書きされる
            Map<String, TitleConfig> worlds = new HashMap<>(defaultTitle);
            for (String key : l.getKeys(false)) {
                TitleConfig c = defaultTitle.getOrDefault(key, def);
                worlds.put(key, TitleConfig.loadConfig(l.getConfigurationSection(key), c));
            }
            title.put(locale, Collections.unmodifiableMap(worlds));
        }
    }

    public static ThisWorld onEnable(Plugin plugin, ConfigurationSection config, PluginCommand command) {
        ThisWorld instance = new ThisWorld(plugin, config, command);
        Bukkit.getPluginManager().registerEvents(instance, plugin);
        command.setExecutor(instance);
        command.setTabCompleter(instance);
        return instance;
    }

    public void onDisable() {
        command.setTabCompleter(plugin);
        command.setExecutor(plugin);
        HandlerList.unregisterAll(this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChangedWorldEvent(PlayerChangedWorldEvent e) {
        Player player = e.getPlayer();
        if (!player.hasPermission("moreenjoy.thisworld.show")) {
            return;
        }

        TitleConfig t = title.getOrDefault(player.getLocale(), defaultTitle).get(player.getWorld().getName());
        if (t != null) {
            if (t.delay > 0) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> send(player, t), t.delay);
            } else {
                send(player, t);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "This command can only be run by players.");
            return true;
        }
        if (!sender.hasPermission("moreenjoy.thisworld.show")) {
            sender.sendMessage(PREFIX + ChatColor.RED + "You don't have permission!");
            return true;
        }

        Player player = (Player) sender;
        TitleConfig t = title.getOrDefault(player.getLocale(), defaultTitle).get(player.getWorld().getName());
        if (t == null) {
            player.sendMessage(unavailable);
            return true;
        }

        send(player, t);
        return true;
    }

    private void send(Player player, TitleConfig config) {
        Player.Spigot spigot = player.spigot();
        player.sendTitle(config.title, config.subTitle, config.fadeIn, config.stay, config.fadeOut);
        spigot.sendMessage(ChatMessageType.ACTION_BAR, config.actionbar);
        for (BaseComponent[] c : config.chat) {
            spigot.sendMessage(ChatMessageType.SYSTEM, c);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
