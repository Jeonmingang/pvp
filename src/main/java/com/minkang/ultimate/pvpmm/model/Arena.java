package com.minkang.ultimate.pvpmm.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class Arena {
    private final String name;
    private String worldName;
    private Location pos1;
    private Location pos2;
    private Location pos3;
    private Location pos4;

    public Arena(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) { this.worldName = worldName; }

    public Location getPos1() { return pos1; }
    public Location getPos2() { return pos2; }
    public Location getPos3() { return pos3; }
    public Location getPos4() { return pos4; }

    public void setPos(int idx, Location loc) {
        if (idx == 1) pos1 = loc;
        if (idx == 2) pos2 = loc;
        if (idx == 3) pos3 = loc;
        if (idx == 4) pos4 = loc;
        if (loc != null) this.worldName = loc.getWorld().getName();
    }

    public Location getPos(int idx) {
        if (idx == 1) return pos1;
        if (idx == 2) return pos2;
        if (idx == 3) return pos3;
        if (idx == 4) return pos4;
        return null;
    }

    public World getWorld() {
        if (worldName == null) return null;
        return Bukkit.getWorld(worldName);
    }

    public boolean isReady() {
        if (worldName == null) return false;
        if (pos1 == null) return false;
        if (pos2 == null) return false;
        if (pos3 == null) return false;
        if (pos4 == null) return false;
        return true;
    }
}
