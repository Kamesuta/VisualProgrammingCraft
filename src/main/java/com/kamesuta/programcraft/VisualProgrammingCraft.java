package com.kamesuta.programcraft;

import com.kamesuta.programcraft.lua.LuaMachine;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class VisualProgrammingCraft extends JavaPlugin {
    public static VisualProgrammingCraft instance;
    public static Logger logger;
    public static String biosText;
    private static Map<String, LuaMachine> luaMachines = new HashMap<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        logger = getLogger();

        // Load the bios
        try {
            biosText = loadTextResource("bios.lua");
        } catch (IOException e) {
            logger.severe("Could not load bios.lua: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
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
            sender.sendMessage("Usage: /pico <create|destroy|list|run>");
            return true;
        }
        String subCommand = args[0];

        if (subCommand.equalsIgnoreCase("list")) {
            sender.sendMessage("Machines:");
            for (String machineName : luaMachines.keySet()) {
                sender.sendMessage("  " + machineName);
            }
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("Usage: /pico <name> <create|destroy|list|run>");
            return true;
        }
        String name = args[1];

        if (subCommand.equalsIgnoreCase("create")) {
            if (luaMachines.containsKey(name)) {
                sender.sendMessage("Machine already exists: " + name);
                return true;
            }
            LuaMachine machine = new LuaMachine();
            machine.addAPI("println", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue luaValue) {
                    sender.sendMessage(luaValue.tojstring());
                    return LuaValue.NIL;
                }
            });
            luaMachines.put(name, machine);
            sender.sendMessage("Machine created: " + name);
            return true;
        } else if (subCommand.equalsIgnoreCase("destroy")) {
            LuaMachine machine = luaMachines.get(name);
            if (machine == null) {
                sender.sendMessage("Machine does not exist: " + name);
                return true;
            }
            machine.unload();
            luaMachines.remove(name);
            sender.sendMessage("Machine destroyed: " + name);
            return true;
        } else if (subCommand.equalsIgnoreCase("run")) {
            if (args.length < 3) {
                sender.sendMessage("Usage: /pico run <name> <code>");
                return true;
            }
            LuaMachine machine = luaMachines.get(name);
            if (machine == null) {
                sender.sendMessage("Machine does not exist: " + name);
                return true;
            }
            if (!machine.isFinished()) {
                sender.sendMessage("Machine is already running: " + name);
                return true;
            }

            String code = args[2];
            machine.loadBios(biosText + "\n" + code);
            machine.handleEvent(null, null);
            return true;
        } else {
            sender.sendMessage("Usage: /pico <create|destroy|list|run> <name>");
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
                throw new IOException("Could not find file: " + filename);
            }
            return new BufferedReader(reader)
                    .lines()
                    .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new IOException("Could not read file: " + filename);
        }
    }
}
