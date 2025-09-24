package com.minkang.ultimate.pvpmm.util;
import org.bukkit.Bukkit; import org.bukkit.Location; import org.bukkit.World;
public class LocationUtil {
    public static String serialize(Location loc){ if(loc==null) return null; return String.format("%s,%f,%f,%f,%f,%f", loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()); }
    public static Location deserialize(String s){ if(s==null) return null; String[] p=s.split(","); if(p.length<6) return null; World w=Bukkit.getWorld(p[0]); if(w==null) return null; return new Location(w, Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3]), Float.parseFloat(p[4]), Float.parseFloat(p[5])); }
}
