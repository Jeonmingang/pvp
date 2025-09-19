package com.minkang.ultimate.pvpmm.listeners;

import com.minkang.ultimate.pvpmm.manager.MatchManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class DeathQuitListener implements Listener {

    private final MatchManager matchManager;

    public DeathQuitListener(MatchManager matchManager) {
        this.matchManager = matchManager;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        java.util.UUID id = e.getEntity().getUniqueId();
        matchManager.playerDiedOrQuit(id);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        java.util.UUID id = e.getPlayer().getUniqueId();
        matchManager.playerDiedOrQuit(id);
    }
}
