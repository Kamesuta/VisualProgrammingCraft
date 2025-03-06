package com.kamesuta.programcraft;

import com.kamesuta.programcraft.lua.LuaMachine;
import org.bukkit.Location;
import org.bukkit.block.CommandBlock;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class VisualProgrammingCraft extends JavaPlugin {
    public static VisualProgrammingCraft instance;
    public static Logger logger;

    public String biosText;
    private final Map<String, LuaMachine> luaMachines = new HashMap<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        logger = getLogger();

        // Load the bios
        try {
            biosText = loadTextResource("bios.lua");
        } catch (IOException e) {
            logger.severe("bios.luaの読み込みに失敗: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Tick the timers
        getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (LuaMachine machine : luaMachines.values()) {
                machine.getTimer().update();
                machine.getPico().update();
            }
        }, 0, 1);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!command.getName().equalsIgnoreCase("pico")) {
            return false;
        }

        if (args.length < 1) {
            sender.sendMessage("使用法: /pico <create|destroy|list|run>");
            return true;
        }
        String subCommand = args[0];

        if (subCommand.equalsIgnoreCase("list")) {
            sender.sendMessage("Pico:");
            for (String machineName : luaMachines.keySet()) {
                sender.sendMessage("  " + machineName);
            }
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("使用法: /pico <name> <create|destroy|list|run>");
            return true;
        }
        String name = args[1];

        if (subCommand.equalsIgnoreCase("create")) {
            // プレイヤー/コマンドブロック座標取得
            Location location = null;
            if (sender instanceof Player) {
                location = ((Player) sender).getLocation();
            } else if (sender instanceof CommandBlock) {
                location = ((CommandBlock) sender).getLocation();
            }
            if (location == null) {
                sender.sendMessage("位置を特定できません");
                return true;
            }

            // マシン追加
            if (luaMachines.containsKey(name)) {
                sender.sendMessage("Picoは既に存在します: " + name);
                return true;
            }
            LuaMachine machine = new LuaMachine();
            luaMachines.put(name, machine);

            // プリント系
            machine.addAPI("print", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue luaValue) {
                    sender.sendMessage(luaValue.tojstring());
                    return LuaValue.NIL;
                }
            });

            // Pico
            machine.getPico().spawn(location.getWorld(), location, name);

            sender.sendMessage("Picoを作成しました: " + name);
            return true;
        } else if (subCommand.equalsIgnoreCase("destroy")) {
            LuaMachine machine = luaMachines.get(name);
            if (machine == null) {
                sender.sendMessage("Picoは存在しません: " + name);
                return true;
            }

            // Pico
            machine.getPico().despawn();

            machine.unload();
            luaMachines.remove(name);
            sender.sendMessage("Picoは破棄されました: " + name);
            return true;
        } else if (subCommand.equalsIgnoreCase("run")) {
            if (args.length < 3) {
                sender.sendMessage("使用法: /pico run <name> <code>");
                return true;
            }
            LuaMachine machine = luaMachines.get(name);
            if (machine == null) {
                sender.sendMessage("Picoは存在しません: " + name);
                return true;
            }
            if (!machine.isFinished()) {
                sender.sendMessage("Picoは既に実行中です: " + name);
                return true;
            }

            String code = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            try {
                machine.loadBios(biosText + "\n" + code);
            } catch (LuaError e) {
                sender.sendMessage("コードの読み込みに失敗: " + e.getMessage());
                return true;
            }
            machine.handleEvent(null, null);
            return true;
        } else {
            sender.sendMessage("使用法: /pico <create|destroy|list|run> <name>");
            return true;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!command.getName().equalsIgnoreCase("pico")) {
            return null;
        }

        if (args.length == 1) {
            return List.of("create", "destroy", "list", "run");
        } else if (args.length == 2) {
            return luaMachines.keySet().stream()
                    .filter(name -> name.startsWith(args[1]))
                    .collect(Collectors.toList());
        }

        return null;
    }

    private String loadTextResource(String filename) throws IOException {
        // Load the bios
        try (Reader reader = getTextResource(filename)) {
            if (reader == null) {
                throw new IOException("ファイルの読み込みに失敗: " + filename);
            }
            return new BufferedReader(reader)
                    .lines()
                    .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new IOException("ファイルの読み込みに失敗: " + filename, e);
        }
    }
}
