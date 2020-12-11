package jp.jyn.moreenjoy.opennow;

import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/*package*/ enum PlaceOpen {
    RIGHT("right") {
        @Override
        boolean check(Player player, Action action) {
            return action == Action.RIGHT_CLICK_BLOCK && !player.isSneaking();
        }
    },
    RIGHT_SHIFT("right", "shift", "sneak", "sneaking") {
        @Override
        boolean check(Player player, Action action) {
            return action == Action.RIGHT_CLICK_BLOCK && player.isSneaking();
        }
    },
    LEFT("left") {
        @Override
        boolean check(Player player, Action action) {
            return action == Action.LEFT_CLICK_BLOCK && !player.isSneaking();
        }
    },
    LEFT_SHIFT("left", "shift", "sneak", "sneaking") {
        @Override
        boolean check(Player player, Action action) {
            return action == Action.LEFT_CLICK_BLOCK && player.isSneaking();
        }
    };

    /*package*/final static Map<String, PlaceOpen> BY_NAME;
    private String key;
    private String[] prefix;

    static {
        Map<String, PlaceOpen> m = new HashMap<>();
        for (PlaceOpen v : PlaceOpen.values()) {
            String k = v.key;
            if (v.prefix.length == 0) {
                m.put(k, v);
                continue;
            }

            // あり得そうなパターンを組み立てる
            for (String s : v.prefix) {
                m.put(s + "_" + k, v); // shift_right
                m.put(s + "-" + k, v); // shift-right
                m.put(k + "_" + s, v); // right_shift
                m.put(k + "-" + s, v); // right-shift
                m.put(s + k, v); // shiftright
                m.put(k + s, v); // rightshift
                m.put(s.charAt(0) + k, v); // sright
                m.put(k + s.charAt(0), v); // rights
            }
        }
        BY_NAME = Collections.unmodifiableMap(m);
    }

    PlaceOpen(String key, String... prefix) {
        this.key = key;
        this.prefix = prefix;
    }

    /*package*/
    abstract boolean check(Player player, Action action);
}
