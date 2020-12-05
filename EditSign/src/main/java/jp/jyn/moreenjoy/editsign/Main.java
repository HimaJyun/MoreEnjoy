package jp.jyn.moreenjoy.editsign;

import jp.jyn.moreenjoy.utils.AbstractMain;

public class Main extends AbstractMain<EditSign> {
    @Override
    protected EditSign init() {
        return EditSign.onEnable(this, getConfig(), getCommand("edit"));
    }

    @Override
    public void onDisable() {
        instance.onDisable();
    }
}
