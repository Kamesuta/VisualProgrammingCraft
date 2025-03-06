package com.kamesuta.programcraft.lua;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

public class Pico {
    LuaMachine m_machine;
    Entity m_entity;
    Location m_targetLocation;
    Double m_targetYaw;

    public void spawn(World world, Location location, String name) {
        Allay entity = world.spawn(location, Allay.class);
        // タグを追加
        entity.addScoreboardTag("pico");
        entity.addScoreboardTag("pico_" + name);
        // 無敵
        entity.setInvulnerable(true);
        // 名前を表示
        entity.setCustomNameVisible(true);
        entity.setCustomName(name);
        // AIを無効化
        entity.setAI(false);
        m_entity = entity;
    }

    public void despawn() {
        m_entity.remove();
    }

    public void update() {
        // ターゲットが設定されている場合は移動
        if (m_targetLocation != null) {
            // ターゲットに向かって移動 (0.1マスだけ移動)
            Vector moveTo = moveTowards(m_entity.getLocation().toVector(), m_targetLocation.toVector(), 0.1);
            Location loc = m_entity.getLocation();
            loc.set(moveTo.getX(), moveTo.getY(), moveTo.getZ());
            m_entity.teleport(loc);

            // ターゲットに到達したら停止
            if (m_entity.getLocation().distance(m_targetLocation) < 0.1) {
                m_targetLocation = null;
                m_entity.setVelocity(new Vector(0, 0, 0));

                // ターゲットに到達したことを通知
                m_machine.getTimer().queueLuaEvent("pico_move", LuaValue.NIL);
            }
        }

        // ターゲットが設定されている場合は旋回
        else if (m_targetYaw != null) {
            // ターゲットに向かって旋回 (5度だけ旋回)
            Location location = m_entity.getLocation();
            double yawTo = rotateTowards(location.getYaw(), m_targetYaw);
            location.setYaw((float) yawTo);
            m_entity.teleport(location);

            // ターゲットに到達したら停止
            if (Math.abs(m_targetYaw - yawTo) < 0.1) {
                m_targetYaw = null;

                // ターゲットに到達したことを通知
                m_machine.getTimer().queueLuaEvent("pico_turn", LuaValue.NIL);
            }
        }
    }

    private static Vector moveTowards(Vector moveFrom, Vector moveTo, double moveLength) {
        Vector direction = moveTo.clone().subtract(moveFrom); // moveFrom から moveTo への方向ベクトル
        double distance = direction.length(); // moveFrom から moveTo までの距離

        if (distance <= moveLength) {
            return moveTo.clone(); // 既に十分に近い場合 moveTo を返す
        }

        return moveFrom.clone().add(direction.normalize().multiply(moveLength)); // 指定距離だけ進む
    }

    private static double rotateTowards(double yawFrom, double yawTo) {
        double deltaYaw = yawTo - yawFrom;
        if (deltaYaw > 180) {
            deltaYaw -= 360;
        } else if (deltaYaw < -180) {
            deltaYaw += 360;
        }
        return yawFrom + Math.signum(deltaYaw) * Math.min(Math.abs(deltaYaw), 5);
    }

    public void register(LuaMachine api) {
        m_machine = api;

        LuaTable pico = new LuaTable();
        api.addAPI("pico", pico);
        LuaTable nativeHandler = new LuaTable();
        pico.set("native", nativeHandler);

        // 移動
        nativeHandler.set("move", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {
                // 向き
                double side;
                int updown;
                switch (arg1.checkjstring()) {
                    case "forward" -> {
                        side = 0;
                        updown = 0;
                    }
                    case "right" -> {
                        side = 90;
                        updown = 0;
                    }
                    case "back" -> {
                        side = 180;
                        updown = 0;
                    }
                    case "left" -> {
                        side = 270;
                        updown = 0;
                    }
                    case "up" -> {
                        side = 0;
                        updown = 1;
                    }
                    case "down" -> {
                        side = 0;
                        updown = -1;
                    }
                    default -> throw new LuaError("Invalid side: " + arg1.checkjstring());
                }

                // 移動距離を取得
                double distance = arg2.checkdouble();
                // yawを取得
                double yaw = side + m_entity.getLocation().getYaw();
                double angle = Math.toRadians(Math.round(yaw / 90) * 90);
                // 向きを東西南北に変換
                Vector dir = updown == 0
                        ? new Vector(-Math.sin(angle), 0, Math.cos(angle))
                        : new Vector(0, updown, 0);
                // ターゲット位置を設定
                m_targetLocation = m_entity.getLocation().toCenterLocation().add(dir.multiply(distance));
                // ターゲット方向を設定
                m_targetYaw = side;
                return LuaValue.NIL;
            }
        });

        // 旋回
        nativeHandler.set("turn", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                // 向き
                double side = switch (arg.checkjstring()) {
                    case "right" -> 90;
                    case "left" -> -90;
                    default -> throw new LuaError("Invalid side: " + arg.checkjstring());
                };

                // ターゲット方向を設定
                m_targetYaw = side + m_entity.getLocation().getYaw();
                return LuaValue.NIL;
            }
        });
    }
}
