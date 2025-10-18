package com.minkang.ultimate.pvpmm.listeners;

import com.minkang.ultimate.pvpmm.manager.MatchManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;

public class JoinKickListener implements Listener {
    private final MatchManager matchManager;
    public JoinKickListener(MatchManager matchManager){
        this.matchManager = matchManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e){
        // Apply any pending restores from matches ended while the player was offline
        matchManager.applyPendingRestore(e.getPlayer());
    }

    @EventHandler
    public void onKick(PlayerKickEvent e){
        // Treat kicks as quits for match resolution
        matchManager.playerDiedOrQuit(e.getPlayer().getUniqueId());
    }
}
