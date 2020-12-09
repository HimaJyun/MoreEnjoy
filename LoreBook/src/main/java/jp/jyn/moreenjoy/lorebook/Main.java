package jp.jyn.moreenjoy.lorebook;

import jp.jyn.moreenjoy.utils.AbstractMain;

public class Main extends AbstractMain<LoreBook> {
    @Override
    protected LoreBook init() {
        return LoreBook.onEnable(this, getConfig());
    }

    @Override
    public void onDisable() {
        instance.onDisable();
    }
}
