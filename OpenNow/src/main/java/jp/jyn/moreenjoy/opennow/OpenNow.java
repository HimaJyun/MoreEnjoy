package jp.jyn.moreenjoy.opennow;

import jp.jyn.moreenjoy.utils.ColorConverter;
import jp.jyn.moreenjoy.utils.PersistentMoreType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class OpenNow implements Listener {
    private final static String PATTERN = "\\{\\s*player\\s*}";
    private final static Set<Material> AVAILABLE;

    static {
        Set<Material> tmp = Arrays.stream(Material.values())
            .filter(m -> m.name().endsWith("SHULKER_BOX") && !m.name().startsWith("LEGACY_"))
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(Material.class)));
        tmp.add(Material.ENDER_CHEST);
        AVAILABLE = Collections.unmodifiableSet(tmp);
    }

    private final boolean enableEnder;
    private final boolean enableShulker;
    private final boolean enderOnlyOne;

    private final String[] lore;
    private final PlaceOpen place;
    private final NamespacedKey key;

    private final Map<UUID, OpenedShulker> opened = new HashMap<>();

    private OpenNow(Plugin plugin, ConfigurationSection config) {
        this.enableEnder = config.getBoolean("ender");
        this.enableShulker = config.getBoolean("shulker");
        this.enderOnlyOne = config.getBoolean("enderOnlyOne");

        this.lore = ColorConverter.convert(config.getString("lore")).split(PATTERN, -1);
        this.place = PlaceOpen.BY_NAME.get(config.getString("placeOpen"));
        this.key = new NamespacedKey(plugin, "opened_by");
    }

    public static OpenNow onEnable(Plugin plugin, ConfigurationSection config) {
        OpenNow instance = new OpenNow(plugin, config);
        Bukkit.getPluginManager().registerEvents(instance, plugin);
        if (instance.place == null) {
            // 設置オープン無効
            PlayerInteractEvent.getHandlerList().unregister(instance);
        }
        return instance;
    }

    public void onDisable() {
        for (Map.Entry<UUID, OpenedShulker> entry : opened.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                player.closeInventory();
            }
            closeShulker(player, entry.getValue());
        }
        // 開きっぱなしのインベントリを全て閉める
        HandlerList.unregisterAll(this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEvent(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (e.getHand() == EquipmentSlot.HAND
            && place.check(player, e.getAction())
            && AVAILABLE.contains(e.getMaterial())) {
            if (e.getMaterial() == Material.ENDER_CHEST) {
                if (!enableEnder) {
                    return;
                }
                openEnder(player);
            } else {
                if (!enableShulker) {
                    return;
                }
                openShulker(player, player.getInventory(), e.getItem());
            }
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClickEvent(InventoryClickEvent e) {
        ItemStack item = e.getCurrentItem();
        if (item == null || !AVAILABLE.contains(item.getType())) {
            return;
        }

        // 開いているアイテムは触らせない
        if (isTakeAction(e.getAction()) && isOpened(item)) { // TODO: 捨てる操作とかを正しく実装したい
            e.setCancelled(true);
            return;
        }

        if (e.getClick() != ClickType.RIGHT
            || (e.getSlotType() != InventoryType.SlotType.CONTAINER
            && e.getSlotType() != InventoryType.SlotType.QUICKBAR)) {
            return;
        }

        if (item.getType() == Material.ENDER_CHEST) {
            if (!enableEnder) {
                return;
            }
            if (enderOnlyOne && item.getAmount() != 1) {
                return;
            }
            openEnder(e.getWhoClicked());
        } else {
            if (!enableShulker) {
                return;
            }
            openShulker(e.getWhoClicked(), e.getClickedInventory(), item);
        }
        e.setCancelled(true);
    }

    private void openEnder(HumanEntity player) {
        player.openInventory(player.getEnderChest());
        playSound(player, Sound.BLOCK_ENDER_CHEST_OPEN);
    }

    // TODO: ひとまとめにできる
    private void openShulker(HumanEntity player, Inventory from, ItemStack item) {
        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        if (meta == null) {
            return;
        }
        ShulkerBox shulker = (ShulkerBox) meta.getBlockState();

        // シュルカーボックスを直接開くことはできない
        String title = meta.hasDisplayName() ? meta.getDisplayName() : InventoryType.SHULKER_BOX.getDefaultTitle();
        // InventoryType.SHULKER_BOXにしておけばシュルカー入れ子防止のハンドリングもしてくれるっぽい？
        Inventory inventory = Bukkit.createInventory(null, InventoryType.SHULKER_BOX, title);
        inventory.setContents(shulker.getInventory().getContents());

        player.openInventory(inventory);
        setLore(meta, player);
        // フラグ付け
        meta.getPersistentDataContainer().set(key, PersistentMoreType.UUID, player.getUniqueId());
        meta.setBlockState(shulker);
        item.setItemMeta(meta);
        opened.put(player.getUniqueId(), new OpenedShulker(item.getType(), inventory, from));

        playSound(player, Sound.BLOCK_SHULKER_BOX_OPEN);
    }

    private void closeShulker(HumanEntity player, OpenedShulker shulker) {
        // TODO: この辺のゴチャゴチャした操作を切り出す
        if (shulker == null) {
            return;
        }

        Map.Entry<Integer, ? extends ItemStack> entry = findOpenedShulker(player, shulker.from, shulker.material);
        if (entry == null) {
            return; // TODO: バグ?
        }
        ItemStack item = entry.getValue();

        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        if (meta == null) {
            return;
        }
        ShulkerBox box = (ShulkerBox) meta.getBlockState();

        box.getInventory().setContents(shulker.inventory.getContents());
        meta.getPersistentDataContainer().remove(key);
        removeLore(meta);
        meta.setBlockState(box);
        item.setItemMeta(meta);
        shulker.from.setItem(entry.getKey(), item);

        playSound(player, Sound.BLOCK_SHULKER_BOX_CLOSE);
    }

    private boolean isOpened(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer data = meta.getPersistentDataContainer();
        return data.has(key, PersistentDataType.INTEGER_ARRAY);
    }

    private void removeLore(ItemMeta meta) {
        if (!meta.hasLore()) {
            return;
        }

        List<String> l = meta.getLore();
        if (l == null) {
            return;
        }

        l = l.subList(1, l.size());
        meta.setLore(l.isEmpty() ? null : l);
    }

    private void setLore(ItemMeta meta, HumanEntity player) {
        List<String> l = new ArrayList<>();
        l.add(String.join(player.getName(), lore));
        if (meta.hasLore()) {
            List<String> old = meta.getLore();
            if (old != null) l.addAll(old);
        }
        meta.setLore(l);
    }

    private void breakContainer(Block block) {
        BlockState state = block.getState();
        if (!(state instanceof BlockInventoryHolder)) {
            return;
        }
        Inventory inventory = ((BlockInventoryHolder) state).getInventory();
        for (ItemStack item : inventory.getStorageContents()) {
            ItemMeta meta;
            if (item == null || !AVAILABLE.contains(item.getType())
                || (meta = item.getItemMeta()) == null) {
                continue;
            }

            PersistentDataContainer data = meta.getPersistentDataContainer();
            UUID uuid = data.get(key, PersistentMoreType.UUID);
            if (uuid == null) {
                continue;
            }
            data.remove(key);

            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.closeInventory();
            }

            OpenedShulker shulker = opened.remove(uuid);
            if (shulker == null || !(meta instanceof BlockStateMeta)) {
                continue;
            }
            BlockStateMeta blockState = (BlockStateMeta) meta;
            ShulkerBox box = (ShulkerBox) blockState.getBlockState();

            box.getInventory().setContents(shulker.inventory.getContents());
            removeLore(blockState);
            blockState.setBlockState(box);
            item.setItemMeta(blockState);
            // TODO: setItem必要？

            if (player != null) {
                playSound(player, Sound.BLOCK_SHULKER_BOX_CLOSE);
            }
        }
    }

    // 優先度調整
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreakEvent(BlockBreakEvent e) {
        breakContainer(e.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplodeEvent(EntityExplodeEvent e) {
        for (Block block : e.blockList()) {
            breakContainer(block);
        }
    }

    //<editor-fold desc="Close Event Handler" defaultstate="collapsed">
    @EventHandler(ignoreCancelled = true)
    public void onInventoryCloseEvent(InventoryCloseEvent e) {
        HumanEntity player = e.getPlayer();
        closeShulker(player, opened.remove(player.getUniqueId()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeathEvent(PlayerDeathEvent e) {
        Player player = e.getEntity();
        closeShulker(player, opened.remove(player.getUniqueId()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItemEvent(PlayerDropItemEvent e) {
        ItemStack item = e.getItemDrop().getItemStack();
        if (AVAILABLE.contains(item.getType()) && isOpened(item)) {
            e.setCancelled(true); // TODO: 止めるんじゃなくて閉める
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryMoveItemEvent(InventoryMoveItemEvent e) {
        ItemStack item = e.getItem();
        if (AVAILABLE.contains(item.getType()) && isOpened(item)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDispenseEvent(BlockDispenseEvent e) {
        ItemStack item = e.getItem();
        if (AVAILABLE.contains(item.getType()) && isOpened(item)) {
            e.setCancelled(true);
        }
    }
    //</editor-fold>

    private Map.Entry<Integer, ? extends ItemStack> findOpenedShulker(HumanEntity player, Inventory inventory,
                                                                      Material material) {
        // TODO: intで返す方が良いのでは？
        UUID uuid = player.getUniqueId();
        for (Map.Entry<Integer, ? extends ItemStack> entry : inventory.all(material).entrySet()) {
            ItemStack item = entry.getValue();
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                continue;
            }

            PersistentDataContainer data = meta.getPersistentDataContainer();
            UUID u = data.get(key, PersistentMoreType.UUID);
            if (uuid.equals(u)) {
                return entry;
            }
        }
        return null;
    }

    private void playSound(Entity entity, Sound sound) {
        Location location = entity.getLocation();
        World world = location.getWorld();
        if (world != null) {
            world.playSound(location, sound, 1.0f, 1.0f);
        }
    }

    private boolean isTakeAction(InventoryAction action) {
        switch (action) {
            case PICKUP_ALL:
            case PICKUP_HALF:
            case PICKUP_ONE: // 原理的に起きなさそう
            case PICKUP_SOME: // 原理的に起きなさそう
            case DROP_ONE_SLOT:
            case DROP_ALL_SLOT:
            case DROP_ALL_CURSOR:
            case DROP_ONE_CURSOR: // 原理的に起きなさそう
            case HOTBAR_SWAP:
            case HOTBAR_MOVE_AND_READD:
            case SWAP_WITH_CURSOR:
            case MOVE_TO_OTHER_INVENTORY:
                return true;
            default:
                return false;
        }
    }

    private final static class OpenedShulker {
        private final Material material;
        private final Inventory inventory;
        private final Inventory from;

        private OpenedShulker(Material material, Inventory inventory, Inventory from) {
            this.material = material;
            this.inventory = inventory;
            this.from = from;
        }
    }
}
