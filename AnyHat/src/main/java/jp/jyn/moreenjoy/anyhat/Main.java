package jp.jyn.moreenjoy.anyhat;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private AnyHat instance = null;

    @Override
    public void onEnable() {
        instance = AnyHat.onEnable(this, getCommand("hat"));
    }

    @Override
    public void onDisable() {
        instance.onDisable();
    }
}
