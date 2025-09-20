package com.minkang.ultimate.pvpmm.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class Arena {
    private final String name;
    private String worldName;
    private Location pos1, pos2, pos3, pos4;
    public Arena(String name) { this.name = name; }
    public String getName() { return name; }
    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) { this.worldName = worldName; }
    public org.bukkit.Location getPos1() { return pos1; }
    public org.bukkit.Location getPos2() { return pos2; }
    public org.bukkit.Location getPos3() { return pos3; }
    public org.bukkit.Location getPos4() { return pos4; }
    public void setPos(int idx, Location loc) {
        if (idx==1) pos1 = loc;
        if (idx==2) pos2 = loc;
        if (idx==3) pos3 = loc;
        if (idx==4) pos4 = loc;
        if (loc != null) worldName = loc.getWorld().getName();
    }
    public Location getPos(int idx) {
        if (idx==1) return pos1;
        if (idx==2) return pos2;
        if (idx==3) return pos3;
        if (idx==4) return pos4;
        return null;
    }
    public World getWorld() { return worldName==null?null:Bukkit.getWorld(worldName); }
    public boolean isReady() { return worldName!=null && pos1!=null && pos2!=null && pos3!=null && pos4!=null; }
}
