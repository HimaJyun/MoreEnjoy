package jp.jyn.moreenjoy.anyhat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;

public class Main extends JavaPlugin {
    private AnyHat instance = null;

    @Override
    public void onEnable() {
        instance = AnyHat.onEnable();
    }

    @Override
    public void onDisable() {
        instance.onDisable();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0
            || !args[0].equalsIgnoreCase("reload")
            || !sender.hasPermission("moreenjoy.anyhat.reload")) {
            return instance.onCommand(sender, command, label, args);
        }

        onDisable();
        onEnable();
        sender.sendMessage("[MoreEnjoy (AnyHat)] Reload complete.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1
            || !"reload".startsWith(args[0])
            || !sender.hasPermission("moreenjoy.anyhat.reload")) {
            return instance.onTabComplete(sender, command, alias, args);
        }
        return Collections.singletonList("reload");
    }
}
