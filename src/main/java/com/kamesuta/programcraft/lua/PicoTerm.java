package com.kamesuta.programcraft.lua;

import org.bukkit.command.CommandSender;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

public class PicoTerm {
    private String name;
    private CommandSender sender;

    public void setOutput(String name, CommandSender sender) {
        this.name = name;
        this.sender = sender;
    }

    public void register(LuaMachine api) {
        // プリント系
        api.addAPI("print", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue luaValue) {
                print(luaValue.tojstring());
                return LuaValue.NIL;
            }
        });
    }

    public void print(String text) {
        sender.sendMessage(String.format("[%s] %s", name, text));
    }

    public void printError(LuaError error) {
        sender.sendMessage(String.format("[%s] §c%s", name, error.getMessage()));
    }
}
