package com.minkang.ultimate.pvpmm.listeners;

import com.minkang.ultimate.pvpmm.Main;
import com.minkang.ultimate.pvpmm.manager.MatchManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandBlockListener implements Listener {
    private final MatchManager matchManager;
    private final Main plugin;
    public CommandBlockListener(MatchManager mm, Main plugin) { this.matchManager = mm; this.plugin = plugin; }

    @EventHandler
    public void onCmd(PlayerCommandPreprocessEvent e) {
        if (!matchManager.isInMatch(e.getPlayer().getUniqueId())) return;
        if (plugin.getConfig().getBoolean("match.allowCommandsWhileInMatch", false)) return;
        java.util.List<String> blocked = plugin.getConfig().getStringList("match.blockedCommands");
        String msg = e.getMessage().toLowerCase();
        for (String cmd : blocked) {
            if (msg.startsWith(cmd.toLowerCase())) {
                e.setCancelled(true);
                e.getPlayer().sendMessage("§c[경쟁전] 매치 중에는 해당 명령어를 사용할 수 없습니다.");
                return;
            }
        }
    }
}
