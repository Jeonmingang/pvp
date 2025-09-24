package com.minkang.ultimate.pvpmm.listeners;
import com.minkang.ultimate.pvpmm.manager.MatchManager; import com.minkang.ultimate.pvpmm.model.Match;
import org.bukkit.entity.Player; import org.bukkit.event.EventHandler; import org.bukkit.event.Listener; import org.bukkit.event.entity.EntityDamageByEntityEvent;
public class DamageListener implements Listener {
    private final MatchManager matchManager; private final java.util.Map<java.util.UUID, Long> ffCooldown = new java.util.HashMap<java.util.UUID, Long>();
    public DamageListener(MatchManager matchManager){ this.matchManager=matchManager; }
    @EventHandler public void onDamage(EntityDamageByEntityEvent e){
        if(!(e.getEntity() instanceof Player) || !(e.getDamager() instanceof Player)) return;
        Player v=(Player)e.getEntity(); Player d=(Player)e.getDamager();
        Match mv=matchManager.getMatch(v.getUniqueId()); Match md=matchManager.getMatch(d.getUniqueId()); if(mv==null||md==null) return; if(!mv.getId().equals(md.getId())) return;
        boolean sameA = mv.isInTeamA(v.getUniqueId()) && mv.isInTeamA(d.getUniqueId());
        boolean sameB = mv.isInTeamB(v.getUniqueId()) && mv.isInTeamB(d.getUniqueId());
        if(sameA || sameB){ e.setCancelled(true); long now=System.currentTimeMillis(); Long last=ffCooldown.get(d.getUniqueId()); if(last==null || now-last>2000L){ ffCooldown.put(d.getUniqueId(), now); d.sendMessage("§c[경쟁전] 같은 팀은 공격할 수 없습니다."); } }
    }
}
