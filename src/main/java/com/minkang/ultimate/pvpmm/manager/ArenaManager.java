package com.minkang.ultimate.pvpmm.manager;
import com.minkang.ultimate.pvpmm.model.Arena;
import com.minkang.ultimate.pvpmm.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import java.io.File; import java.io.IOException; import java.util.*;
public class ArenaManager {
    private final Plugin plugin; private final File file; private final FileConfiguration cfg;
    private final Map<String, Arena> arenas = new HashMap<String, Arena>();
    public ArenaManager(Plugin plugin){ this.plugin=plugin; this.file=new File(plugin.getDataFolder(),"arenas.yml"); if(!file.exists()) plugin.saveResource("arenas.yml", false); this.cfg=YamlConfiguration.loadConfiguration(file); load(); }
    public void load(){
        arenas.clear();
        ConfigurationSection sec=cfg.getConfigurationSection("arenas"); if(sec==null) return;
        for(String key: sec.getKeys(false)){
            ConfigurationSection a=sec.getConfigurationSection(key);
            Arena arena=new Arena(key);
            arena.setWorldName(a.getString("world"));
            arena.setPos(1, LocationUtil.deserialize(a.getString("pos1")));
            arena.setPos(2, LocationUtil.deserialize(a.getString("pos2")));
            arena.setPos(3, LocationUtil.deserialize(a.getString("pos3")));
            arena.setPos(4, LocationUtil.deserialize(a.getString("pos4")));
            arenas.put(key.toLowerCase(java.util.Locale.ROOT), arena);
        }
    }
    public void save(){
        cfg.set("arenas", null);
        // Merge-protect: load existing file to preserve positions when current in-memory pos is null
        org.bukkit.configuration.file.FileConfiguration _existing = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        for(Arena a: arenas.values()){
            String base="arenas."+a.getName();
            cfg.set(base+".world", a.getWorldName());
            if ((a.getPos(1)) != null) {
            cfg.set(base+".pos1", LocationUtil.serialize(a.getPos(1)));
        } else {
            Object _keep = _existing.get(base+".pos1");
            if (_keep != null) cfg.set(base+".pos1", _keep);
        }
            if ((a.getPos(2)) != null) {
            cfg.set(base+".pos2", LocationUtil.serialize(a.getPos(2)));
        } else {
            Object _keep = _existing.get(base+".pos2");
            if (_keep != null) cfg.set(base+".pos2", _keep);
        }
            if ((a.getPos(3)) != null) {
            cfg.set(base+".pos3", LocationUtil.serialize(a.getPos(3)));
        } else {
            Object _keep = _existing.get(base+".pos3");
            if (_keep != null) cfg.set(base+".pos3", _keep);
        }
            if ((a.getPos(4)) != null) {
            cfg.set(base+".pos4", LocationUtil.serialize(a.getPos(4)));
        } else {
            Object _keep = _existing.get(base+".pos4");
            if (_keep != null) cfg.set(base+".pos4", _keep);
        }
        }
        try{ cfg.save(file); }catch(IOException e){ e.printStackTrace(); }
    }
    public Arena create(String name){ String key=name.toLowerCase(java.util.Locale.ROOT); Arena a=arenas.get(key); if(a!=null) return a; a=new Arena(name); arenas.put(key, a); save(); return a; }
    public Arena get(String name){ return name==null?null:arenas.get(name.toLowerCase(java.util.Locale.ROOT)); }
    public java.util.List<Arena> all(){ return new java.util.ArrayList<Arena>(arenas.values()); }
    public boolean setPos(String name, int idx, Location loc){ Arena a=get(name); if(a==null) return false; a.setPos(idx, loc); save(); return true; }
    public boolean deleteByIndex(int oneBasedIndex){
        java.util.List<Arena> list=new java.util.ArrayList<Arena>(arenas.values());
        java.util.Collections.sort(list, new java.util.Comparator<Arena>(){ public int compare(Arena x, Arena y){ return x.getName().compareToIgnoreCase(y.getName()); } });
        int idx=oneBasedIndex-1; if(idx<0 || idx>=list.size()) return false;
        Arena target=list.get(idx); arenas.remove(target.getName().toLowerCase(java.util.Locale.ROOT)); save(); return true;
    }
}
