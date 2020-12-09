package jp.jyn.moreenjoy.lorebook;

import jp.jyn.moreenjoy.utils.ColorConverter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LoreBook implements Listener {
    private final static String PREFIX = "[MoreEnjoy (LoreBook)] ";

    private final boolean onlyDurable;
    private final int experience;
    private final int maxLength;
    private final int maxLine;

    private final String defaultNotEnough;
    private final Map<String, String> notEnough = new HashMap<>();


    private LoreBook(ConfigurationSection config) {
        this.onlyDurable = config.getBoolean("onlyDurable");
        this.experience = config.getInt("experience");
        this.maxLength = config.getInt("maxLength");
        this.maxLine = config.getInt("maxLine");

        String tmp = config.getString("notEnough");
        this.defaultNotEnough = ColorConverter.convert(PREFIX + tmp);
        for (String key : config.getKeys(false)) { // 実はローカライズ可能
            if (config.isConfigurationSection(key)) {
                notEnough.put(key, ColorConverter.convert(PREFIX + config.getString(key + ".notEnough")));
            }
        }
    }

    public static LoreBook onEnable(Plugin plugin, ConfigurationSection config) {
        LoreBook instance = new LoreBook(config);
        Bukkit.getPluginManager().registerEvents(instance, plugin);
        return instance;
    }

    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrepareAnvilEvent(PrepareAnvilEvent e) {
        // 結果を表示するためにしか使えない
        ItemStack result = e.getResult();
        if (result != null && result.getType() != Material.AIR) {
            // 安全装置
            return;
        }

        AnvilInventory anvil = e.getInventory();
        ItemStack item, book;
        if ((item = anvil.getItem(0)) == null ||
            (book = anvil.getItem(1)) == null) {
            return;
        }

        ItemMeta bookMeta = book.getItemMeta();
        if (!(bookMeta instanceof BookMeta)
            || (onlyDurable && item.getType().getMaxDurability() == 0)
            || !e.getView().getPlayer().hasPermission("moreenjoy.lorebook.use")) {
            return;
        }

        result = item.clone();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) {
            return;
        }

        BookMeta b = (BookMeta) bookMeta;
        // lore
        List<String> pages = b.getPages();
        meta.setLore(pages.isEmpty() ? null : splitPages(pages));
        // name
        String name = anvil.getRenameText();
        if ((name == null || name.isEmpty()) && b.hasTitle()) {
            name = b.getTitle();
        }
        if (name != null && !name.isEmpty()) {
            meta.setDisplayName(name);
        }

        //anvil.setRepairCost(0); // 効かない
        result.setItemMeta(meta);
        e.setResult(result);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClickEvent(InventoryClickEvent e) {
        if (e.getSlotType() != InventoryType.SlotType.RESULT || e.getSlot() != 2) { // 0=素材、1=本、2=結果
            return;
        }

        Inventory inventory;
        HumanEntity human;
        ItemStack item, book;
        if ((inventory = e.getClickedInventory()) == null || inventory.getType() != InventoryType.ANVIL
            || (item = e.getCurrentItem()) == null || item.getType() == Material.AIR
            || (book = inventory.getItem(1)) == null || !(book.getItemMeta() instanceof BookMeta)
            || !((human = e.getWhoClicked()) instanceof Player)) {
            return;
        }

        Player player = (Player) human;
        // 経験値チェック
        if (experience > 0 && player.getGameMode() != GameMode.CREATIVE) {
            int current = player.getLevel();
            if (experience > current) {
                player.sendMessage(notEnough.getOrDefault(player.getLocale(), defaultNotEnough));
                return;
            }
            player.setLevel(current - experience);
        }

        switch (e.getClick()) {
            case LEFT:
            case RIGHT:
                if (player.getItemOnCursor().getType() != Material.AIR) {
                    return;
                }
                player.setItemOnCursor(item);
                break;
            case SHIFT_LEFT:
            case SHIFT_RIGHT:
                Inventory pi = player.getInventory();
                // バニラの挙動とは違うが再現が面倒なのでこうする
                int i = pi.firstEmpty();
                if (i == -1) {
                    return;
                }
                pi.setItem(i, item);
                break;
            default:
                return;
        }

        inventory.setItem(0, null); // 素材
        if (book.getAmount() > 1) {
            book.setAmount(book.getAmount() - 1);
        } else {
            inventory.setItem(1, null);
        }
        e.setCurrentItem(null);
        player.updateInventory();

        // ﾊﾞｧﾝﾊﾞｧﾝﾊﾞｧﾝ!!
        Location location;
        World world;
        if ((location = inventory.getLocation()) != null
            && (world = location.getWorld()) != null) {
            world.playSound(location, Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
        }
    }

    private List<String> splitPages(List<String> pages) {
        return pages.stream()
            .flatMap(page -> Stream.of(page.split("\n"))) // 改行コードはLF
            .flatMap(line -> {
                if (line.length() <= maxLength) {
                    return Stream.of(line);
                }
                Stream.Builder<String> builder = Stream.builder();
                for (int i = 0; i < line.length(); i += maxLength) {
                    builder.add(line.substring(i, Math.min(line.length(), i + maxLength)));
                }
                return builder.build();
            })
            .limit(maxLine)
            .collect(Collectors.toList());
    }
}
