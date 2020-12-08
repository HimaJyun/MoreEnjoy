package jp.jyn.moreenjoy.thisworld;

import jp.jyn.moreenjoy.utils.AbstractMain;

public class Main extends AbstractMain<ThisWorld> {
    @Override
    protected ThisWorld init() {
        return ThisWorld.onEnable(this, getConfig(), getCommand("thisworld"));
    }

    @Override
    public void onDisable() {
        instance.onDisable();
    }
}
