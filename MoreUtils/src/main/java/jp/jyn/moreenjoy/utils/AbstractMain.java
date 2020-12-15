package jp.jyn.moreenjoy.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;

public abstract class AbstractMain<T> extends JavaPlugin {
    protected T instance;

    abstract protected T init();

    abstract public void onDisable();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        instance = init();
    }

    @Override
    public PluginCommand getCommand(String name) {
        PluginCommand command = super.getCommand(name);
        if (command != null) {
            command.setPermissionMessage("[MoreEnjoy (" + this.getName() + ")] "
                + ChatColor.RED + "You don't have permission!!");
        }
        return command;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            return false;
        }

        onDisable();
        onEnable();
        sender.sendMessage("[MoreEnjoy (" + this.getName() + ")] Reload complete.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1 || !"reload".startsWith(args[0])) {
            return Collections.emptyList();
        }
        return Collections.singletonList("reload");
    }
}
