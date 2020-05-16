package jp.jyn.moreenjoy;

import jp.jyn.moreenjoy.anyhat.AnyHat;
import jp.jyn.moreenjoy.novoid.NoVoid;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public class MoreEnjoy extends JavaPlugin {
    private final Deque<Runnable> destructor = new ArrayDeque<>();
    private FileConfiguration config = null;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (config != null) {
            reloadConfig();
        }
        config = getConfig();

        if (config.getBoolean("AnyHat.enable")) {
            getLogger().info("Enabling AnyHat");
            AnyHat instance = AnyHat.onEnable(this, getCommand("hat"));
            destructor.add(instance::onDisable);
        }

        if (config.getBoolean("NoVoid.enable")) {
            getLogger().info("Enabling NoVoid");
            NoVoid instance = NoVoid.onEnable(this, getSection("NoVoid"));
            destructor.add(instance::onDisable);
        }
    }

    @Override
    public void onDisable() {
        while (!destructor.isEmpty()) {
            destructor.removeFirst().run();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage("[MoreEnjoy] " + ChatColor.RED + "This feature is currently unavailable!");
        return true;
    }

    private ConfigurationSection getSection(String key) {
        return Objects.requireNonNull(config.getConfigurationSection(key));
    }
}
