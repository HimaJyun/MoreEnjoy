package jp.jyn.moreenjoy.slabcombine;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

public class SlabCombine implements Listener {
    private final static Map<Material, Material> SLAB_MAPPING;

    static {
        Map<Material, Material> m = new EnumMap<>(Material.class);

        m.put(Material.BRICK_SLAB, Material.BRICKS);
        for (Material material : Material.values()) {
            String name = material.name();
            if (name.startsWith("LEGACY_") || !name.endsWith("_SLAB")) {
                continue;
            }
            String s = name.substring(0, name.length() - 5); // "_SLAB".length()
            Material p = null;
            if (name.endsWith("_BRICK_SLAB")) p = Material.getMaterial(s + "S"); // BRICK_SLAB
            if (p == null) p = Material.getMaterial(s + "_PLANKS"); // PLANKS
            if (p == null) p = Material.getMaterial(s + "_BLOCK");
            if (p == null) p = Material.getMaterial(s);

            if (p != null) {
                m.put(material, p);
            }
        }

        SLAB_MAPPING = Collections.unmodifiableMap(m);
    }

    private final Map<Material, Set<NamespacedKey>> registered = new EnumMap<>(Material.class);

    private SlabCombine(Plugin plugin, ConfigurationSection config) {
        // TODO: 汚い
        Set<Material> exclude = new HashSet<>();
        for (String value : config.getStringList("exclude")) {
            Material m = Material.getMaterial(value.toUpperCase(Locale.ENGLISH));
            if (m == null) {
                plugin.getLogger().warning("Parse error: " + value);
                continue;
            }
            exclude.add(m);
        }

        Map<Material, Set<NamespacedKey>> m = new EnumMap<>(Material.class);
        for (Map.Entry<Material, Material> entry : SLAB_MAPPING.entrySet()) {
            if (exclude.contains(entry.getKey()) || exclude.contains(entry.getValue())) {
                continue;
            }

            NamespacedKey key = new NamespacedKey(plugin, entry.getValue().name().toLowerCase(Locale.ENGLISH));
            ShapedRecipe recipe = new ShapedRecipe(key, new ItemStack(entry.getValue()));
            recipe.shape("S", "S").setIngredient('S', entry.getKey()); // TODO: この辺りも設定可能にする
            if (Bukkit.addRecipe(recipe)) {
                registered.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).add(key);
            }
        }

        for (Map.Entry<String, ConfigurationSection> entry
            : getSectionList(config, "custom", ConfigurationSection::getConfigurationSection)) {
            ItemStack result = parseItem(entry.getKey());
            if (result == null) {
                plugin.getLogger().severe("Parse error: " + entry.getKey());
                continue;
            }

            if (entry.getValue().isList("layout")) {
                ShapedRecipe r = customShapedRecipe(plugin, result, entry.getValue());
                if (r == null) {
                    continue;
                }
                if (Bukkit.addRecipe(r)) {
                    NamespacedKey k = r.getKey();
                    for (Map.Entry<Character, ItemStack> e : r.getIngredientMap().entrySet()) {
                        ItemStack item = e.getValue();
                        registered.computeIfAbsent(item.getType(), ignore -> new HashSet<>()).add(k);
                    }
                }
            } else {
                ShapelessRecipe r = customShapelessRecipe(plugin, result, entry.getValue());
                if (r == null) {
                    continue;
                }
                if (Bukkit.addRecipe(r)) {
                    NamespacedKey k = r.getKey();
                    for (ItemStack item : r.getIngredientList()) {
                        registered.computeIfAbsent(item.getType(), ignore -> new HashSet<>()).add(k);
                    }
                }
            }
        }
    }

    public static SlabCombine onEnable(Plugin plugin, ConfigurationSection config) {
        return new SlabCombine(plugin, config);
    }

    public void onDisable() {
    }

    private ShapelessRecipe customShapelessRecipe(Plugin plugin, ItemStack result, ConfigurationSection config) {
        NamespacedKey key = new NamespacedKey(
            plugin,
            Objects.requireNonNull(config.getString("key", result.getType().name())).toLowerCase(Locale.ENGLISH)
        );

        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        String group = config.getString("group", null);
        if (group != null) {
            recipe.setGroup(group);
        }

        List<String> list = config.getStringList("material");
        if (list.isEmpty()) {
            plugin.getLogger().severe("need material: " + config.getName());
            return null;
        }

        for (String material : list) {
            ItemStack item = parseItem(material);
            if (item == null) {
                plugin.getLogger().severe("Parse error: " + material);
                return null;
            }
            recipe.addIngredient(item.getAmount(), item.getType());
        }
        return recipe;
    }

    private ShapedRecipe customShapedRecipe(Plugin plugin, ItemStack result, ConfigurationSection config) {
        NamespacedKey key = new NamespacedKey(
            plugin,
            Objects.requireNonNull(config.getString("key", result.getType().name())).toLowerCase(Locale.ENGLISH)
        );

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(config.getStringList("layout").toArray(new String[0]));
        String group = config.getString("group", null);
        if (group != null) {
            recipe.setGroup(group);
        }

        List<Map.Entry<String, String>> list = getSectionList(config, "material", ConfigurationSection::getString);
        if (list.isEmpty()) {
            plugin.getLogger().severe("need material: " + config.getName());
            return null;
        }

        for (Map.Entry<String, String> entry : list) {
            Material material = Material.getMaterial(entry.getValue());
            if (material == null) {
                plugin.getLogger().severe("Parse error: " + entry.getValue());
                return null;
            }
            if (entry.getKey().length() > 1) {
                plugin.getLogger().warning("must be 1 character: " + entry.getKey());
            }
            recipe.setIngredient(entry.getKey().charAt(0), material);
        }
        return recipe;
    }

    private ItemStack parseItem(String str) {
        String[] value = str.split("\\*", 2);
        Material material = Material.getMaterial(value[0]);
        if (material == null) {
            return null;
        }

        int amount = 1;
        if (value.length > 1) {
            try {
                amount = Integer.parseInt(value[1]);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return new ItemStack(material, amount);
    }

    private <T> List<Map.Entry<String, T>> getSectionList(ConfigurationSection config, String key,
                                                          BiFunction<ConfigurationSection, String, T> mapper) {
        if (!config.isConfigurationSection(key)) {
            return Collections.emptyList();
        }

        ConfigurationSection c = Objects.requireNonNull(config.getConfigurationSection(key));
        List<Map.Entry<String, T>> result = new ArrayList<>();
        for (String k : c.getKeys(false)) {
            result.add(new AbstractMap.SimpleImmutableEntry<>(
                k,
                mapper.apply(c, k)
            ));
        }
        return result;
    }
}
