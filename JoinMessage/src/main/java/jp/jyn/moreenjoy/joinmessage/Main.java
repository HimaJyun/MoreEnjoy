package jp.jyn.moreenjoy.joinmessage;

import jp.jyn.moreenjoy.utils.AbstractMain;

public class Main extends AbstractMain<JoinMessage> {
    @Override
    protected JoinMessage init() {
        return JoinMessage.onEnable(this, getConfig());
    }

    @Override
    public void onDisable() {
        instance.onDisable();
    }
}
