package jp.jyn.moreenjoy.docklessvehicle;

import jp.jyn.moreenjoy.utils.AbstractMain;

public class Main extends AbstractMain<DocklessVehicle> {
    @Override
    protected DocklessVehicle init() {
        return DocklessVehicle.onEnable(this, getConfig(), getCommand("dockless"));
    }

    @Override
    public void onDisable() {
        instance.onDisable();
    }
}
