package jp.jyn.moreenjoy.immutablespawner;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private ImmutableSpawner instance = null;

    @Override
    public void onEnable() {
        instance = ImmutableSpawner.onEnable(this);
    }

    @Override
    public void onDisable() {
        instance.onDisable();
    }
}
