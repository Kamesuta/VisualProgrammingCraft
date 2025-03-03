package com.kamesuta.programcraft.lua;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

public class LuaTimer {
    private final Map<Integer, Timer> m_timers;
    private int m_nextTimerToken;
    private LuaMachine m_machine;

    private final LinkedBlockingQueue<Runnable> m_tasks = new LinkedBlockingQueue<>();

    private static class Timer {
        public int m_ticksLeft;

        public Timer(int ticksLeft) {
            m_ticksLeft = ticksLeft;
        }
    }

    public LuaTimer() {
        m_nextTimerToken = 0;
        m_timers = new HashMap<>();
    }

    public void update() {
        synchronized (m_timers) {
            // Countdown all of our active timers
            Iterator<Map.Entry<Integer, Timer>> it = m_timers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, Timer> entry = it.next();
                Timer timer = entry.getValue();
                timer.m_ticksLeft--;
                if (timer.m_ticksLeft <= 0) {
                    // Queue the "timer" event
                    queueLuaEvent("timer", LuaValue.valueOf(entry.getKey()));
                    it.remove();
                }
            }
        }

        synchronized (m_tasks) {
            // Run all queued events
            while (!m_tasks.isEmpty()) {
                m_tasks.poll().run();
            }
        }
    }

    public void register(LuaMachine api) {
        m_machine = api;

        api.addAPI("queueEvent", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                queueLuaEvent(args.checkjstring(1), args.subargs(2));
                return LuaValue.NIL;
            }
        });

        api.addAPI("startTimer", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                double timer = args.checkdouble(1);
                synchronized (m_timers) {
                    m_timers.put(m_nextTimerToken, new Timer((int) Math.round(timer * 20.0)));
                    return LuaValue.valueOf(m_nextTimerToken++);
                }
            }
        });

        api.addAPI("cancelTimer", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                int token = args.checkint(1);
                synchronized (m_timers) {
                    m_timers.remove(token);
                }
                return LuaValue.NIL;
            }
        });
    }

    public void queueLuaEvent(String event, Varargs arguments) {
        m_tasks.add(() -> m_machine.handleEvent(event, arguments));
    }
}
