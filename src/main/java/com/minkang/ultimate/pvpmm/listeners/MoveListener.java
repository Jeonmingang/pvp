package com.minkang.ultimate.pvpmm.listeners;
import com.minkang.ultimate.pvpmm.manager.MatchManager;
import com.minkang.ultimate.pvpmm.model.Match;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.entity.Player;
import org.bukkit.Location;

public class MoveListener implements Listener {
    private final MatchManager matchManager;
    public MoveListener(MatchManager matchManager) { this.matchManager = matchManager; }

    @EventHandler
    public void onMove(PlayerMoveEvent e){
        Player p = e.getPlayer();
        Match m = matchManager.getMatch(p.getUniqueId());
        if (m == null) return;
        if (m.getState() == Match.State.PREP) {
            Location from = e.getFrom();
            Location to = e.getTo();
            if (to == null) return;
            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                e.setTo(new Location(from.getWorld(), from.getX(), from.getY(), from.getZ(), to.getYaw(), to.getPitch()));
            }
        }
    }
}
