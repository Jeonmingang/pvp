package com.minkang.ultimate.pvpmm.listeners;
import com.minkang.ultimate.pvpmm.manager.MatchManager;
import com.minkang.ultimate.pvpmm.model.Match;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Sound;
import java.util.*;

public class MoveListener implements Listener {
    private final MatchManager matchManager;
    // last countdown second shown to each player to avoid spam
    private final Map<java.util.UUID, Integer> lastShownSecond = new HashMap<java.util.UUID, Integer>();

    public MoveListener(MatchManager matchManager) { this.matchManager = matchManager; }

    @EventHandler
    public void onMove(PlayerMoveEvent e){
        Player p = e.getPlayer();
        Match m = matchManager.getMatch(p.getUniqueId());
        if (m == null) return;

        if (m.getState() == Match.State.PREP) {
            long elapsed = System.currentTimeMillis() - m.getStartMillis();
            int left = Math.max(0, 3 - (int)(elapsed / 1000L));

            // Fallback HUD: show 3-2-1 using classic titles on 1.16.5
            Integer last = lastShownSecond.get(p.getUniqueId());
            if (left > 0 && (last == null || last.intValue() != left)) {
                p.sendTitle("§e" + left, "§7곧 시작", 0, 20, 0);
                lastShownSecond.put(p.getUniqueId(), left);
            }

            if (elapsed >= 3000L) {
                // Force start (failsafe for environments where the primary countdown task failed)
                m.setState(Match.State.RUNNING);
                // Clear any lastShownSecond record so it's fresh next round
                lastShownSecond.remove(p.getUniqueId());

                // "Start!" HUD and sound
                p.sendTitle("§a시작!", "§7행운을 빕니다", 5, 30, 5);
                try { p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f); } catch (Throwable ignored) {}

                // Allow the motion by not freezing below
                return;
            }

            // Still in PREP window: freeze positional movement, allow head turning
            Location from = e.getFrom();
            Location to = e.getTo();
            if (to == null) return;
            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                e.setTo(new Location(from.getWorld(), from.getX(), from.getY(), from.getZ(), to.getYaw(), to.getPitch()));
            }
        }
    }
}
