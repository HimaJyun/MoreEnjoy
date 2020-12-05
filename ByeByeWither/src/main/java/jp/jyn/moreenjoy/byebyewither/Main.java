package jp.jyn.moreenjoy.byebyewither;

import jp.jyn.moreenjoy.utils.AbstractMain;

public class Main extends AbstractMain<ByeByeWither> {
    @Override
    protected ByeByeWither init() {
        return ByeByeWither.onEnable(this, getConfig());
    }

    @Override
    public void onDisable() {
        instance.onDisable();
    }
}
