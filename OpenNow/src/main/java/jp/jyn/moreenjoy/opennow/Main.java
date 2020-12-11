package jp.jyn.moreenjoy.opennow;

import jp.jyn.moreenjoy.utils.AbstractMain;

public class Main extends AbstractMain<OpenNow> {
    @Override
    protected OpenNow init() {
        return OpenNow.onEnable(this, getConfig());
    }

    @Override
    public void onDisable() {
        instance.onDisable();
    }
}
