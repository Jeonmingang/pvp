package com.minkang.ultimate.pvpmm.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class Arena {
    private final String name;
    private String worldName;
    private Location pos1,pos2,pos3,pos4;
    public Arena(String name){ this.name=name; }
    public String getName(){ return name; }
    public String getWorldName(){ return worldName; }
    public void setWorldName(String w){ this.worldName=w; }
    public Location getPos(int i){ if(i==1)return pos1; if(i==2)return pos2; if(i==3)return pos3; if(i==4)return pos4; return null; }
    public void setPos(int i, Location l){ if(i==1)pos1=l; if(i==2)pos2=l; if(i==3)pos3=l; if(i==4)pos4=l; if(l!=null) worldName=l.getWorld().getName(); }
    public World getWorld(){ return worldName==null?null:Bukkit.getWorld(worldName); }
    public boolean isReady(){ return worldName!=null && pos1!=null && pos2!=null && pos3!=null && pos4!=null; }
}
