package jp.jyn.moreenjoy.joinmessage;

import jp.jyn.moreenjoy.utils.ColorConverter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.regex.Pattern;

public class JoinMessage implements Listener {
    private final static String PATTERN = "\\{\\s*player\\s*}";

    private final String[] join;
    private final String[] quit;

    private JoinMessage(ConfigurationSection config) {
        final Pattern regex = Pattern.compile(PATTERN);
        String s = config.getString("join", null);
        join = s == null ? null : regex.split(ColorConverter.convert(s), -1);
        s = config.getString("quit", null);
        quit = s == null ? null : regex.split(ColorConverter.convert(s), -1);
    }

    public static JoinMessage onEnable(Plugin plugin, ConfigurationSection config) {
        JoinMessage instance = new JoinMessage(config);
        Bukkit.getPluginManager().registerEvents(instance, plugin);
        if (instance.join == null) {
            PlayerJoinEvent.getHandlerList().unregister(instance);
        }
        if (instance.quit == null) {
            PlayerQuitEvent.getHandlerList().unregister(instance);
        }
        return instance;
    }

    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoinEvent(PlayerJoinEvent e) {
        e.setJoinMessage(String.join(e.getPlayer().getName(), join));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuitEvent(PlayerQuitEvent e) {
        e.setQuitMessage(String.join(e.getPlayer().getName(), quit));
    }
}
