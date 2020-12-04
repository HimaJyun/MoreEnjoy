package jp.jyn.moreenjoy.utils;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColorConverterTest {

    @Test
    public void rawTest() {
        assertEquals("raw string", ColorConverter.convert("raw string"));
    }

    @Test
    public void emptyTest() {
        assertEquals("", ColorConverter.convert(""));
    }

    @Test
    public void colorTest() {
        assertEquals(ChatColor.BLACK.toString(), ColorConverter.convert("&0"));
        assertEquals(ChatColor.BLACK + "aaa", ColorConverter.convert("&0aaa"));
        assertEquals("aaa" + ChatColor.BLACK, ColorConverter.convert("aaa&0"));
        assertEquals("a" + ChatColor.BLACK + "a", ColorConverter.convert("a&0a"));
        assertEquals(ChatColor.BLACK.toString() + ChatColor.BLACK.toString(), ColorConverter.convert("&0&0"));
        assertEquals(ChatColor.RESET.toString(), ColorConverter.convert("&R"));
    }

    @Test
    public void hexColor() {
        assertEquals(
            "\u00A7x\u00A7a\u00A7a\u00A7b\u00A7b\u00A7c\u00A7c",
            ColorConverter.convert("#aabbcc")
        );
        assertEquals(
            "\u00A7x\u00A7a\u00A7a\u00A7b\u00A7b\u00A7c\u00A7caaa",
            ColorConverter.convert("#aabbccaaa")
        );
        assertEquals(
            "aaa\u00A7x\u00A7a\u00A7a\u00A7b\u00A7b\u00A7c\u00A7c",
            ColorConverter.convert("aaa#aabbcc")
        );
        assertEquals(
            "aaa\u00A7x\u00A7a\u00A7a\u00A7b\u00A7b\u00A7c\u00A7caaa",
            ColorConverter.convert("aaa#aabbccaaa")
        );
        assertEquals(
            "\u00A7x\u00A7a\u00A7a\u00A7b\u00A7b\u00A7c\u00A7c\u00A7x\u00A7a\u00A7a\u00A7b\u00A7b\u00A7c\u00A7c",
            ColorConverter.convert("#aabbcc#aabbcc")
        );
        assertEquals(
            "\u00A7x\u00A7a\u00A7a\u00A7b\u00A7b\u00A7c\u00A7c",
            ColorConverter.convert("#abc")
        );
        assertEquals(
            "\u00A7x\u00A7a\u00A7a\u00A7b\u00A7b\u00A7c\u00A7c",
            ColorConverter.convert("#AABBCC")
        );
    }

    @Test
    public void escapeTest() {
        assertEquals("&", ColorConverter.convert("&"));
        assertEquals("&&", ColorConverter.convert("&&"));
        assertEquals("&_", ColorConverter.convert("&_"));
        assertEquals("&0", ColorConverter.convert("&&0"));
        assertEquals("&0&0", ColorConverter.convert("&&0&&0"));
        assertEquals("aaa&0aaa", ColorConverter.convert("aaa&&0aaa"));
        assertEquals("#000000", ColorConverter.convert("&#000000"));
        assertEquals("#000", ColorConverter.convert("&#000"));
    }
}
