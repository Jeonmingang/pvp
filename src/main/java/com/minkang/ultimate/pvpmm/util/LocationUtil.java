package com.minkang.ultimate.pvpmm.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class LocationUtil {
    public static String serialize(Location loc) {
        if (loc == null) return null;
        StringBuilder sb = new StringBuilder();
        sb.append(loc.getWorld().getName()).append(",");
        sb.append(loc.getX()).append(",");
        sb.append(loc.getY()).append(",");
        sb.append(loc.getZ()).append(",");
        sb.append(loc.getYaw()).append(",");
        sb.append(loc.getPitch());
        return sb.toString();
    }

    public static Location deserialize(String s) {
        if (s == null) return null;
        String[] p = s.split(",");
        if (p.length < 6) return null;
        World w = Bukkit.getWorld(p[0]);
        if (w == null) return null;
        double x = Double.parseDouble(p[1]);
        double y = Double.parseDouble(p[2]);
        double z = Double.parseDouble(p[3]);
        float yaw = Float.parseFloat(p[4]);
        float pitch = Float.parseFloat(p[5]);
        return new Location(w, x, y, z, yaw, pitch);
    }
}
