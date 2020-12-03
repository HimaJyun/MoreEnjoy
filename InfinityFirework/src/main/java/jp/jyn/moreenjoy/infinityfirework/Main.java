package jp.jyn.moreenjoy.infinityfirework;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private InfinityFirework instance;

    @Override
    public void onEnable() {
        instance = InfinityFirework.onEnable(this);
    }

    @Override
    public void onDisable() {
        instance.onDisable();
    }
}
