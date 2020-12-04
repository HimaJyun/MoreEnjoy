package jp.jyn.moreenjoy.utils;

import org.bukkit.ChatColor;

public class ColorConverter {
    private ColorConverter() {}

    // TODO: 汚い

    public static String convert(String str) {
        switch (str.length()) {
            case 0:
                return "";
            case 1:
                return str;
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0, l = str.length(); i < l; i++) {
            char c = str.charAt(i);
            switch (c) {
                case '&':
                    if (i + 1 >= l) { // 末尾
                        break;
                    }

                    ChatColor color = ChatColor.getByChar(lower(str.charAt(i + 1)));
                    if (color == null) { // 色ではない
                        break;
                    }

                    if ((i - 1) >= 0 && str.charAt(i - 1) == '&') { // エスケープ
                        sb.deleteCharAt(sb.length() - 1);
                        break;
                    }

                    sb.append(ChatColor.COLOR_CHAR).append(color.getChar());
                    i += 1;
                    continue;
                case '#':
                    if (i + 3 >= l) { // 末尾
                        break;
                    }

                    int j = 3;
                    char r, rr, g, gg, b, bb;
                    r = rr = str.charAt(i + 1);
                    g = gg = str.charAt(i + 2);
                    b = bb = str.charAt(i + 3);
                    if (!isHex(r) || !isHex(g) || !isHex(b)) {
                        break;
                    }
                    // 3桁は確定

                    if (i + 6 < l) { // 6桁チェック
                        char g2 = str.charAt(i + 4), b1 = str.charAt(i + 5), b2 = str.charAt(i + 6);
                        if (isHex(g2) && isHex(b1) && isHex(b2)) {
                            // 6桁
                            rr = g;
                            g = b;
                            gg = g2;
                            b = b1;
                            bb = b2;
                            j = 6;
                        }
                    }

                    if ((i - 1) >= 0 && str.charAt(i - 1) == '&') { // エスケープ
                        sb.deleteCharAt(sb.length() - 1);
                        break;
                    }

                    sb.append(ChatColor.COLOR_CHAR).append('x');
                    sb.append(ChatColor.COLOR_CHAR).append(lower(r));
                    sb.append(ChatColor.COLOR_CHAR).append(lower(rr));
                    sb.append(ChatColor.COLOR_CHAR).append(lower(g));
                    sb.append(ChatColor.COLOR_CHAR).append(lower(gg));
                    sb.append(ChatColor.COLOR_CHAR).append(lower(b));
                    sb.append(ChatColor.COLOR_CHAR).append(lower(bb));
                    i += j;
                    continue;
            }
            sb.append(c);
        }

        return sb.toString();
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'Z');
    }

    private static char lower(char c) {
        if (c >= 'A' && c <= 'Z') {
            c += 32;
        }
        return c;
    }

}
