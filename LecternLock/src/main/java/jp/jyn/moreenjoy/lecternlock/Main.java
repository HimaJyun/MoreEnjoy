package jp.jyn.moreenjoy.lecternlock;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private LecternLock instance = null;

    @Override
    public void onEnable() {
        instance = LecternLock.onEnable(this);
    }

    @Override
    public void onDisable() {
        instance.onDisable();
    }
}
