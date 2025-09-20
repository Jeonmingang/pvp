package com.minkang.ultimate.pvpmm.manager;

import com.minkang.ultimate.pvpmm.model.Arena;
import com.minkang.ultimate.pvpmm.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class ArenaManager {
    private final Plugin plugin;
    private final File file;
    private final FileConfiguration cfg;
    private final Map<String, Arena> arenas = new HashMap<String, Arena>();

    public ArenaManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
        if (!file.exists()) plugin.saveResource("arenas.yml", false);
        this.cfg = YamlConfiguration.loadConfiguration(file);
        load();
    }

    public void load() {
        arenas.clear();
        ConfigurationSection sec = cfg.getConfigurationSection("arenas");
        if (sec==null) return;
        for (String key : sec.getKeys(false)) {
            ConfigurationSection a = sec.getConfigurationSection(key);
            Arena arena = new Arena(key);
            String w = a.getString("world");
            arena.setWorldName(w);
            arena.setPos(1, LocationUtil.deserialize(a.getString("pos1")));
            arena.setPos(2, LocationUtil.deserialize(a.getString("pos2")));
            arena.setPos(3, LocationUtil.deserialize(a.getString("pos3")));
            arena.setPos(4, LocationUtil.deserialize(a.getString("pos4")));
            arenas.put(key.toLowerCase(java.util.Locale.ROOT), arena);
        }
    }

    public void save() {
        cfg.set("arenas", null);
        for (Arena arena : arenas.values()) {
            String base = "arenas." + arena.getName();
            cfg.set(base + ".world", arena.getWorldName());
            cfg.set(base + ".pos1", com.minkang.ultimate.pvpmm.util.LocationUtil.serialize(arena.getPos1()));
            cfg.set(base + ".pos2", com.minkang.ultimate.pvpmm.util.LocationUtil.serialize(arena.getPos2()));
            cfg.set(base + ".pos3", com.minkang.ultimate.pvpmm.util.LocationUtil.serialize(arena.getPos3()));
            cfg.set(base + ".pos4", com.minkang.ultimate.pvpmm.util.LocationUtil.serialize(arena.getPos4()));
        }
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public Arena create(String name) {
        String key = name.toLowerCase(java.util.Locale.ROOT);
        Arena a = arenas.get(key);
        if (a!=null) return a;
        a = new Arena(name);
        arenas.put(key, a);
        save();
        return a;
    }

    public Arena get(String name) { return name==null?null:arenas.get(name.toLowerCase(java.util.Locale.ROOT)); }
    public java.util.List<Arena> all() { return new java.util.ArrayList<Arena>(arenas.values()); }
    public boolean setPos(String name, int idx, Location loc) {
        Arena a = get(name);
        if (a==null) return false;
        a.setPos(idx, loc);
        save();
        return true;
    }
}
