package jp.jyn.moreenjoy.thisworld;

import jp.jyn.moreenjoy.utils.ColorConverter;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* package */ final class TitleConfig {
    public final String title;
    public final String subTitle;
    public final BaseComponent[] actionbar;
    public final Iterable<BaseComponent[]> chat;
    public final int fadeIn;
    public final int stay;
    public final int fadeOut;
    public final int delay;

    private TitleConfig(String title, String subTitle,
                        BaseComponent[] actionbar, Iterable<BaseComponent[]> chat,
                        int fadeIn, int stay, int fadeOut, int delay) {
        this.title = title;
        this.subTitle = subTitle;
        this.actionbar = actionbar;
        this.chat = chat;
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
        this.delay = delay;
    }

    public static TitleConfig initDefault(int fadeIn, int stay, int fadeOut, int delay) {
        return new TitleConfig(
            "", "", new TextComponent[0], Collections.emptyList(),
            fadeIn, stay, fadeOut, delay
        );
    }

    public static TitleConfig loadConfig(ConfigurationSection config, TitleConfig def) {
        String tmp;
        return new TitleConfig(
            (tmp = config.getString("title", null)) == null ? def.title : ColorConverter.convert(tmp),
            (tmp = config.getString("subtitle", null)) == null ? def.subTitle : ColorConverter.convert(tmp),
            (tmp = config.getString("actionbar", null)) == null ? def.actionbar : colorComponent(tmp),
            getComponentIterable(config, "chat", def.chat),
            config.getInt("fadein", def.fadeIn),
            config.getInt("stay", def.stay),
            config.getInt("fadeout", def.fadeOut),
            config.getInt("delay", def.delay)
        );
    }

    private static BaseComponent[] colorComponent(String str) {
        return TextComponent.fromLegacyText(ColorConverter.convert(str));
    }

    private static Iterable<BaseComponent[]> getComponentIterable(ConfigurationSection config, String key,
                                                                  Iterable<BaseComponent[]> def) {
        // なんでgetStringListにデフォルト指定できる奴がないの……
        Object obj = config.get(key, null);
        if (obj == null) {
            return def;
        } else if (obj instanceof String) {
            return Collections.singletonList(colorComponent((String) obj));
        }

        List<?> list = (List<?>) obj;
        if (list.isEmpty()) {
            return Collections.emptyList();
        }

        List<BaseComponent[]> result = new ArrayList<>(list.size());
        for (Object o : list) {
            result.add(colorComponent(String.valueOf(o)));
        }
        return result;
    }
}
