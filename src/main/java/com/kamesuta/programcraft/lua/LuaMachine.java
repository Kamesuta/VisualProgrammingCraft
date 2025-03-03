package com.kamesuta.programcraft.lua;

import com.kamesuta.programcraft.VisualProgrammingCraft;
import org.bukkit.Bukkit;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaThread;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.util.logging.Level;
import java.util.stream.Stream;

public class LuaMachine {
    private LuaValue m_globals;
    private LuaValue m_loadString;
    private LuaValue m_assert;
    private LuaValue m_coroutine_create;
    private LuaValue m_coroutine_resume;
    private LuaValue m_coroutine_yield;

    private LuaValue m_mainRoutine;
    private String m_eventFilter;
    private String m_softAbortMessage;
    private String m_hardAbortMessage;

    public LuaMachine() {
        // Create an environment to run in
        m_globals = JsePlatform.debugGlobals();
        m_loadString = m_globals.get("load");
        m_assert = m_globals.get("assert");

        LuaValue coroutine = m_globals.get("coroutine");
        final LuaValue native_coroutine_create = coroutine.get("create");

        LuaValue debug = m_globals.get("debug");
        final LuaValue debug_sethook = debug.get("sethook");

        coroutine.set("create", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue value) {
                final LuaThread thread = native_coroutine_create.call(value).checkthread();
                debug_sethook.invoke(new LuaValue[]{
                        thread,
                        new ZeroArgFunction() {
                            @Override
                            public LuaValue call() {
                                String hardAbortMessage = m_hardAbortMessage;
                                if (hardAbortMessage != null) {
                                    // スレッド停止
                                    if (thread.state.function == null) {
                                        throw new LuaError("cannot yield main thread");
                                    }
                                    thread.state.lua_yield(LuaValue.NIL);
                                }
                                return LuaValue.NIL;
                            }
                        },
                        LuaValue.NIL,
                        LuaValue.valueOf(100000)
                });
                return thread;
            }
        });

        m_coroutine_create = coroutine.get("create");
        m_coroutine_resume = coroutine.get("resume");
        m_coroutine_yield = coroutine.get("yield");

        // Remove globals we don't want to expose
        m_globals.set("collectgarbage", LuaValue.NIL);
        m_globals.set("dofile", LuaValue.NIL);
        m_globals.set("loadfile", LuaValue.NIL);
        m_globals.set("module", LuaValue.NIL);
        m_globals.set("require", LuaValue.NIL);
        m_globals.set("package", LuaValue.NIL);
        m_globals.set("io", LuaValue.NIL);
        m_globals.set("os", LuaValue.NIL);
        m_globals.set("print", LuaValue.NIL);
        m_globals.set("luajava", LuaValue.NIL);
        m_globals.set("debug", LuaValue.NIL);
        m_globals.set("newproxy", LuaValue.NIL);
        m_globals.set("__inext", LuaValue.NIL);

        // Add version globals
        m_globals.set("_VERSION", "Lua 5.1");
        String version = VisualProgrammingCraft.instance.getDescription().getVersion();
        m_globals.set("_HOST", "VisualProgrammingCraft " + version + " (Minecraft " + Bukkit.getServer().getVersion() + ")");

        // Our main function will go here
        m_mainRoutine = null;
        m_eventFilter = null;

        m_softAbortMessage = null;
        m_hardAbortMessage = null;
    }

    public void addAPI(String name, LuaValue api) {
        m_globals.set(name, api);
    }

    public void loadBios(String biosText) {
        // Begin executing a file (ie, the bios)
        if (m_mainRoutine != null) {
            return;
        }

        try {
            // Load it
            LuaValue program = m_assert.call(m_loadString.call(
                    LuaValue.valueOf(biosText), LuaValue.valueOf("bios.lua")
            ));
            m_mainRoutine = m_coroutine_create.call(program);
        } catch (LuaError e) {
            VisualProgrammingCraft.logger.log(Level.WARNING, "Could not load bios.lua ", e);
            if (m_mainRoutine != null) {
                ((LuaThread) m_mainRoutine).abandon();
                m_mainRoutine = null;
            }
        }
    }

    public void handleEvent(String eventName, Varargs arguments) {
        if (m_mainRoutine == null) {
            return;
        }

        if (m_eventFilter != null && eventName != null && !eventName.equals(m_eventFilter) && !eventName.equals("terminate")) {
            return;
        }

        try {
            LuaValue[] resumeArgs = Stream.concat(
                            Stream.of(m_mainRoutine),
                            eventName == null ? Stream.empty() : Stream.of(LuaValue.valueOf(eventName))
                    )
                    .toArray(LuaValue[]::new);

            Varargs results = m_coroutine_resume.invoke(
                    arguments == null
                            ? LuaValue.varargsOf(resumeArgs)
                            : LuaValue.varargsOf(resumeArgs, arguments));
            if (m_hardAbortMessage != null) {
                throw new LuaError(m_hardAbortMessage);
            } else if (!results.arg1().checkboolean()) {
                throw new LuaError(results.arg(2).checkstring().toString());
            } else {
                LuaValue filter = results.arg(2);
                if (filter.isstring()) {
                    m_eventFilter = filter.toString();
                } else {
                    m_eventFilter = null;
                }
            }

            LuaThread mainThread = (LuaThread) m_mainRoutine;
            if (mainThread.getStatus().equals("dead")) {
                m_mainRoutine = null;
            }
        } catch (LuaError e) {
            ((LuaThread) m_mainRoutine).abandon();
            m_mainRoutine = null;
        } finally {
            m_softAbortMessage = null;
            m_hardAbortMessage = null;
        }
    }

    public void softAbort(String abortMessage) {
        m_softAbortMessage = abortMessage;
    }

    public void hardAbort(String abortMessage) {
        m_softAbortMessage = abortMessage;
        m_hardAbortMessage = abortMessage;
    }

    public boolean isFinished() {
        return (m_mainRoutine == null);
    }

    public void unload() {
        if (m_mainRoutine != null) {
            LuaThread mainThread = (LuaThread) m_mainRoutine;
            mainThread.abandon();
            m_mainRoutine = null;
        }
    }

    private void tryAbort() throws LuaError {
        String abortMessage = m_softAbortMessage;
        if (abortMessage != null) {
            m_softAbortMessage = null;
            m_hardAbortMessage = null;
            throw new LuaError(abortMessage);
        }
    }
}
