package jp.jyn.moreenjoy.anyhat;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.List;

public class AnyHat implements TabExecutor {
    public final static String PREFIX = "[MoreEnjoy (AnyHat)] ";

    private final Plugin plugin;
    private final PluginCommand command;

    private AnyHat(Plugin plugin, PluginCommand command) {
        this.plugin = plugin;
        this.command = command;
    }

    public static AnyHat onEnable(Plugin plugin, PluginCommand command) {
        AnyHat instance = new AnyHat(plugin, command);
        command.setPermission("moreenjoy.anyhat.hat");
        command.setDescription("Wear everything on your head.");
        command.setAliases(Collections.singletonList("anyhat"));

        command.setExecutor(instance);
        command.setTabCompleter(instance);
        return instance;
    }

    public void onDisable() {
        command.setTabCompleter(plugin);
        command.setExecutor(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "This command can only be run by players.");
            return true;
        }

        // get inventory
        PlayerInventory inventory = ((Player) sender).getInventory();

        // swap main hand to helmet
        ItemStack mainHand = inventory.getItemInMainHand();
        ItemStack helmet = inventory.getHelmet();
        inventory.setItemInMainHand(helmet);
        inventory.setHelmet(mainHand);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
