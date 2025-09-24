package com.minkang.ultimate.pvpmm.service;
import org.bukkit.entity.Player; import org.bukkit.plugin.Plugin; import java.util.*;
public class TeamService {
    private final Plugin plugin; private final Map<java.util.UUID, Integer> playerTeamId = new HashMap<java.util.UUID, Integer>(); private final Map<Integer, Set<java.util.UUID>> teamMembers = new HashMap<Integer, Set<java.util.UUID>>();
    private int idSeq=1; public TeamService(Plugin plugin){ this.plugin=plugin; }
    public boolean hasTeam(Player p){ return playerTeamId.containsKey(p.getUniqueId()); }
    public Set<java.util.UUID> getTeam(java.util.UUID id){ Integer tid=playerTeamId.get(id); if(tid==null) return null; Set<java.util.UUID> s=teamMembers.get(tid); return s==null?null:new HashSet<java.util.UUID>(s); }
    public boolean createOrJoin(Player a, Player b){
        if(a==null||b==null) return false; if(a.getUniqueId().equals(b.getUniqueId())) return false;
        Integer ia=playerTeamId.get(a.getUniqueId()); Integer ib=playerTeamId.get(b.getUniqueId());
        if(ia==null&&ib==null){ int nid=idSeq++; Set<java.util.UUID> m=new HashSet<java.util.UUID>(); m.add(a.getUniqueId()); m.add(b.getUniqueId()); teamMembers.put(nid,m); playerTeamId.put(a.getUniqueId(),nid); playerTeamId.put(b.getUniqueId(),nid); return true; }
        if(ia!=null&&ib==null){ Set<java.util.UUID> m=teamMembers.get(ia); if(m.size()>=2) return false; m.add(b.getUniqueId()); playerTeamId.put(b.getUniqueId(), ia); return true; }
        if(ia==null&&ib!=null){ Set<java.util.UUID> m=teamMembers.get(ib); if(m.size()>=2) return false; m.add(a.getUniqueId()); playerTeamId.put(a.getUniqueId(), ib); return true; }
        return false;
    }
    public boolean leaveTeam(Player p){
        java.util.UUID uid=p.getUniqueId(); Integer id=playerTeamId.get(uid); if(id==null) return false;
        Set<java.util.UUID> mem=teamMembers.get(id); if(mem==null){ playerTeamId.remove(uid); return true; }
        mem.remove(uid); playerTeamId.remove(uid);
        if(mem.isEmpty()) teamMembers.remove(id);
        else if(mem.size()==1){ java.util.UUID other=mem.iterator().next(); teamMembers.remove(id); playerTeamId.remove(other); }
        return true;
    }
    public void clearWholeTeam(java.util.UUID uid){ Integer id=playerTeamId.get(uid); if(id==null) return; Set<java.util.UUID> mem=teamMembers.remove(id); if(mem!=null) for(java.util.UUID u: mem) playerTeamId.remove(u); }
}
