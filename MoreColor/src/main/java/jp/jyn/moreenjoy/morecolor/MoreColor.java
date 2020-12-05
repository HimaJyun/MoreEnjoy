package jp.jyn.moreenjoy.morecolor;

import jp.jyn.moreenjoy.utils.ColorConverter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MoreColor implements Listener {
    private final String marker;
    private final boolean signAlways;
    private final boolean bookAlways;

    private MoreColor(String marker, boolean signAlways, boolean bookAlways) {
        this.marker = marker;
        this.signAlways = signAlways;
        this.bookAlways = bookAlways;
    }

    public static MoreColor onEnable(Plugin plugin, ConfigurationSection config) {
        Function<String, Boolean> value = s -> {
            if (s == null) {
                return null;
            }
            switch (s.toLowerCase(Locale.ENGLISH)) {
                case "always":
                case "true":
                    return Boolean.TRUE;
                case "marker":
                    return Boolean.FALSE;
                default:
                    return null;
            }
        };

        Boolean sign = value.apply(config.getString("sign"));
        Boolean book = value.apply(config.getString("book"));
        MoreColor instance = new MoreColor(config.getString("marker"), sign != null && sign, book != null && book);
        Bukkit.getPluginManager().registerEvents(instance, plugin);
        if (sign == null) {
            SignChangeEvent.getHandlerList().unregister(instance);
        }
        if (book == null) {
            PlayerEditBookEvent.getHandlerList().unregister(instance);
        }
        return instance;
    }

    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    @SuppressWarnings("ConstantConditions")
    @EventHandler(ignoreCancelled = true)
    public void onSignChangeEvent(SignChangeEvent e) {
        if (!signAlways) {
            if (!e.getLine(0).startsWith(marker)) {
                return;
            }
            e.setLine(0, e.getLine(0).substring(marker.length()));
        }
        e.setLine(0, ColorConverter.convert(e.getLine(0)));
        e.setLine(1, ColorConverter.convert(e.getLine(1)));
        e.setLine(2, ColorConverter.convert(e.getLine(2)));
        e.setLine(3, ColorConverter.convert(e.getLine(3)));
    }

    @SuppressWarnings("ConstantConditions")
    @EventHandler(ignoreCancelled = true)
    public void onPlayerEditBookEvent(PlayerEditBookEvent e) {
        if (!e.isSigning()) {
            return;
        }

        BookMeta meta = e.getNewBookMeta();
        if (!bookAlways) {
            if (!meta.getPage(1).startsWith(marker)) {
                return;
            }
            meta.setPage(1, meta.getPage(1).substring(marker.length()));
        }

        // content
        meta.setPages(meta.getPages().stream().map(ColorConverter::convert).collect(Collectors.toList()));

        // title
        if (meta.hasTitle()) {
            meta.setTitle(ColorConverter.convert(meta.getTitle()));
        }
        e.setNewBookMeta(meta);
    }
}
