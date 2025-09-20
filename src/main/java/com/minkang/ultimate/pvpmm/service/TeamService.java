package com.minkang.ultimate.pvpmm.service;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.*;

public class TeamService {
    private final Plugin plugin;
    private final Map<java.util.UUID, Integer> playerTeamId = new HashMap<java.util.UUID, Integer>();
    private final Map<Integer, Set<java.util.UUID>> teamMembers = new HashMap<Integer, Set<java.util.UUID>>();
    private int idSeq = 1;
    public TeamService(Plugin plugin) { this.plugin = plugin; }

    public boolean hasTeam(Player p) { return playerTeamId.containsKey(p.getUniqueId()); }
    public Set<java.util.UUID> getTeam(java.util.UUID uuid) {
        Integer id = playerTeamId.get(uuid);
        if (id == null) return null;
        return new HashSet<java.util.UUID>(teamMembers.get(id));
    }
    public int teamSize(java.util.UUID uuid) { Set<java.util.UUID> t=getTeam(uuid); return t==null?0:t.size(); }

    public boolean createOrJoin(Player a, Player b) {
        if (a==null||b==null) return false;
        if (a.getUniqueId().equals(b.getUniqueId())) return false;
        if (hasTeam(a) && teamSize(a.getUniqueId())>=2) return false;
        if (hasTeam(b) && teamSize(b.getUniqueId())>=2) return false;
        Integer ida = playerTeamId.get(a.getUniqueId());
        Integer idb = playerTeamId.get(b.getUniqueId());
        if (ida==null && idb==null) {
            int nid = idSeq++;
            Set<java.util.UUID> m = new HashSet<java.util.UUID>();
            m.add(a.getUniqueId()); m.add(b.getUniqueId());
            teamMembers.put(nid, m);
            playerTeamId.put(a.getUniqueId(), nid);
            playerTeamId.put(b.getUniqueId(), nid);
            return true;
        }
        if (ida!=null && idb==null) {
            Set<java.util.UUID> m = teamMembers.get(ida);
            if (m.size()>=2) return false;
            m.add(b.getUniqueId());
            playerTeamId.put(b.getUniqueId(), ida);
            return true;
        }
        if (ida==null && idb!=null) {
            Set<java.util.UUID> m = teamMembers.get(idb);
            if (m.size()>=2) return false;
            m.add(a.getUniqueId());
            playerTeamId.put(a.getUniqueId(), idb);
            return true;
        }
        return false;
    }

    public Set<java.util.UUID> getOrCreateSolo(Player p) { Set<java.util.UUID> s=new HashSet<java.util.UUID>(); s.add(p.getUniqueId()); return s; }
    public void clearTeam(java.util.UUID uuid) {
        Integer id = playerTeamId.remove(uuid);
        if (id==null) return;
        Set<java.util.UUID> members = teamMembers.get(id);
        if (members==null) return;
        members.remove(uuid);
        if (members.isEmpty()) teamMembers.remove(id);
    }
    public void clearWholeTeam(java.util.UUID uuid) {
        Integer id = playerTeamId.get(uuid);
        if (id==null) return;
        Set<java.util.UUID> m = teamMembers.remove(id);
        if (m!=null) for (java.util.UUID u: m) playerTeamId.remove(u);
    }

    public boolean leaveTeam(Player p) {
        java.util.UUID uid = p.getUniqueId();
        Integer id = playerTeamId.get(uid);
        if (id == null) return false;
        Set<java.util.UUID> mem = teamMembers.get(id);
        if (mem == null) { playerTeamId.remove(uid); return true; }
        mem.remove(uid);
        playerTeamId.remove(uid);
        if (mem.isEmpty()) teamMembers.remove(id);
        else if (mem.size()==1) {
            java.util.UUID other = mem.iterator().next();
            teamMembers.remove(id);
            playerTeamId.remove(other);
        }
        return true;
    }
}
