package com.kamesuta.programcraft.lua;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

public class Pico {
    LuaMachine m_machine;
    Entity m_entity;
    Location m_targetLocation;

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
            m_entity.teleport(moveTo.toLocation(m_entity.getWorld()));

            // ターゲットに到達したら停止
            if (m_entity.getLocation().distance(m_targetLocation) < 0.1) {
                m_targetLocation = null;
                m_entity.setVelocity(new Vector(0, 0, 0));

                // ターゲットに到達したことを通知
                m_machine.getTimer().queueLuaEvent("pico_move", LuaValue.NIL);
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

    public void register(LuaMachine api) {
        m_machine = api;

        LuaTable pico = new LuaTable();
        api.addAPI("pico", pico);
        LuaTable nativeHandler = new LuaTable();
        pico.set("native", nativeHandler);

        // 前進
        nativeHandler.set("forward", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                // 移動距離を取得
                double distance = arg.checkdouble();
                // yawを取得
                double yaw = m_entity.getLocation().getYaw();
                double side = Math.toRadians(Math.round(yaw / 90) * 90);
                // 向きを東西南北に変換
                Vector dir = new Vector(-Math.sin(side), 0, Math.cos(side));
                // エンティティの向きもスナップする
                m_entity.getLocation().setDirection(dir);
                // ターゲット位置を設定
                m_targetLocation = m_entity.getLocation().toCenterLocation().add(dir.multiply(distance));
                return LuaValue.NIL;
            }
        });
    }
}
