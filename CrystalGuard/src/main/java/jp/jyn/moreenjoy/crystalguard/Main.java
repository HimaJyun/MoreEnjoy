package jp.jyn.moreenjoy.crystalguard;

import jp.jyn.moreenjoy.utils.AbstractMain;

public class Main extends AbstractMain<CrystalGuard> {
    @Override
    protected CrystalGuard init() {
        return CrystalGuard.onEnable(this, getConfig());
    }

    @Override
    public void onDisable() {
        instance.onDisable();
    }
}
