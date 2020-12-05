package jp.jyn.moreenjoy.novoid;

import jp.jyn.moreenjoy.utils.AbstractMain;

public class Main extends AbstractMain<NoVoid> {
    @Override
    protected NoVoid init() {
        return NoVoid.onEnable(this, getConfig());
    }

    @Override
    public void onDisable() {
        instance.onDisable();
    }
}
