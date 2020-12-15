package jp.jyn.moreenjoy;

import jp.jyn.moreenjoy.anyhat.AnyHat;
import jp.jyn.moreenjoy.byebyewither.ByeByeWither;
import jp.jyn.moreenjoy.crystalguard.CrystalGuard;
import jp.jyn.moreenjoy.docklessvehicle.DocklessVehicle;
import jp.jyn.moreenjoy.editsign.EditSign;
import jp.jyn.moreenjoy.immutablespawner.ImmutableSpawner;
import jp.jyn.moreenjoy.infinityfirework.InfinityFirework;
import jp.jyn.moreenjoy.joinmessage.JoinMessage;
import jp.jyn.moreenjoy.lorebook.LoreBook;
import jp.jyn.moreenjoy.morecolor.MoreColor;
import jp.jyn.moreenjoy.novoid.NoVoid;
import jp.jyn.moreenjoy.opennow.OpenNow;
import jp.jyn.moreenjoy.ridenow.RideNow;
import jp.jyn.moreenjoy.thisworld.ThisWorld;
import jp.jyn.moreenjoy.utils.ColorConverter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.BiFunction;

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

        String permission = ColorConverter.convert(config.getString("permission"));
        BiFunction<String, String, PluginCommand> cmd = (prefix, command) -> {
            PluginCommand c = getCommand(command);
            Objects.requireNonNull(c).setPermissionMessage(prefix + permission);
            return c;
        };
        cmd.apply("[MoreEnjoy] ", "moreenjoy-reload");

        if (config.getBoolean("AnyHat.enable")) {
            getLogger().info("Enabling AnyHat");
            AnyHat instance = AnyHat.onEnable(this, cmd.apply(AnyHat.PREFIX, "hat"));
            destructor.add(instance::onDisable);
        }

        if (config.getBoolean("ByeByeWither.enable")) {
            getLogger().info("Enabling ByeByeWither");
            ByeByeWither instance = ByeByeWither.onEnable(this, getSection("ByeByeWither"));
            destructor.add(instance::onDisable);
        }

        if (config.getBoolean("CrystalGuard.enable")) {
            getLogger().info("Enabling CrystalGuard");
            CrystalGuard instance = CrystalGuard.onEnable(this, getSection("CrystalGuard"));
            destructor.add(instance::onDisable);
        }

        if (config.getBoolean("DocklessVehicle.enable")) {
            getLogger().info("Enabling DocklessVehicle");
            DocklessVehicle instance = DocklessVehicle.onEnable(
                this, getSection("DocklessVehicle"), getCommand("dockless")
            );
            destructor.add(instance::onDisable);
        }

        if (config.getBoolean("EditSign.enable")) {
            getLogger().info("Enabling EditSign");
            EditSign instance = EditSign.onEnable(this,
                getSection("EditSign"), cmd.apply(EditSign.PREFIX, "edit")
            );
            destructor.add(instance::onDisable);
        }

        if (config.getBoolean("ImmutableSpawner.enable")) {
            getLogger().info("Enabling ImmutableSpawner");
            ImmutableSpawner instance = ImmutableSpawner.onEnable(this);
            destructor.add(instance::onDisable);
        }

        if (config.getBoolean("InfinityFirework.enable")) {
            getLogger().info("Enabling InfinityFirework");
            InfinityFirework instance = InfinityFirework.onEnable(this);
            destructor.add(instance::onDisable);
        }

        if (config.getBoolean("JoinMessage.enable")) {
            getLogger().info("Enabling JoinMessage");
            JoinMessage instance = JoinMessage.onEnable(this, getSection("JoinMessage"));
            destructor.add(instance::onDisable);
        }

        if (config.getBoolean("LoreBook.enable")) {
            getLogger().info("Enabling LoreBook");
            LoreBook instance = LoreBook.onEnable(this, getSection("LoreBook"));
            destructor.add(instance::onDisable);
        }

        if (config.getBoolean("MoreColor.enable")) {
            getLogger().info("Enabling MoreColor");
            MoreColor instance = MoreColor.onEnable(this, getSection("MoreColor"));
            destructor.add(instance::onDisable);
        }

        if (config.getBoolean("NoVoid.enable")) {
            getLogger().info("Enabling NoVoid");
            NoVoid instance = NoVoid.onEnable(this, getSection("NoVoid"));
            destructor.add(instance::onDisable);
        }

        if (config.getBoolean("OpenNow.enable")) {
            getLogger().info("Enabling OpenNow");
            OpenNow instance = OpenNow.onEnable(this, getSection("OpenNow"));
            destructor.add(instance::onDisable);
        }

        if (config.getBoolean("RideNow.enable")) {
            getLogger().info("Enabling RideNow");
            RideNow instance = RideNow.onEnable(this,
                cmd.apply(RideNow.PREFIX, "boat"), cmd.apply(RideNow.PREFIX, "minecart")
            );
            destructor.add(instance::onDisable);
        }

        if (config.getBoolean("ThisWorld.enable")) {
            getLogger().info("Enabling ThisWorld");
            ThisWorld instance = ThisWorld.onEnable(this,
                getSection("ThisWorld"), cmd.apply(ThisWorld.PREFIX, "thisworld")
            );
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
        onDisable();
        onEnable();
        sender.sendMessage("[MoreEnjoy] Reload complete.");
        return true;
    }

    private ConfigurationSection getSection(String key) {
        return Objects.requireNonNull(config.getConfigurationSection(key));
    }
}
