package com.minkang.ultimate.pvpmm.listeners;

import com.minkang.ultimate.pvpmm.manager.MatchManager;
import com.minkang.ultimate.pvpmm.model.Match;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class DamageListener implements Listener {

    private final MatchManager matchManager;

    public DamageListener(MatchManager matchManager) {
        this.matchManager = matchManager;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        if (!(e.getDamager() instanceof Player)) return;
        Player victim = (Player)e.getEntity();
        Player damager = (Player)e.getDamager();

        Match mv = matchManager.getMatch(victim.getUniqueId());
        Match md = matchManager.getMatch(damager.getUniqueId());
        if (mv == null || md == null) return;
        if (!mv.getId().equals(md.getId())) return;

        boolean sameA = mv.isInTeamA(victim.getUniqueId()) && mv.isInTeamA(damager.getUniqueId());
        boolean sameB = mv.isInTeamB(victim.getUniqueId()) && mv.isInTeamB(damager.getUniqueId());
        if (sameA || sameB) {
            e.setCancelled(true);
        }
    }
}
