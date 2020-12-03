package jp.jyn.moreenjoy.byebyewither;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;

public class Main extends JavaPlugin {
    private ByeByeWither instance;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        instance = ByeByeWither.onEnable(this, getConfig());
    }

    @Override
    public void onDisable() {
        instance.onDisable();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            return false;
        }

        onDisable();
        onEnable();
        sender.sendMessage("[MoreEnjoy (ByeByeWither)] Reload complete.");
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
