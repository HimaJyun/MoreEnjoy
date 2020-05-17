package jp.jyn.moreenjoy.ridenow;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private RideNow instance = null;

    @Override
    public void onEnable() {
        instance = RideNow.onEnable(this, getCommand("boat"), getCommand("minecart"));
    }

    @Override
    public void onDisable() {
        instance.onDisable();
    }
}
