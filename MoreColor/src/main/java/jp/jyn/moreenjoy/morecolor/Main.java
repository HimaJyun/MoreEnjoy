package jp.jyn.moreenjoy.morecolor;

import jp.jyn.moreenjoy.utils.AbstractMain;

public class Main extends AbstractMain<MoreColor> {
    @Override
    protected MoreColor init() {
        return MoreColor.onEnable(this, getConfig());
    }

    @Override
    public void onDisable() {
        instance.onDisable();
    }
}
