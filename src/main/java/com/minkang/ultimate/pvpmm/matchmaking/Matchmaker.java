package com.minkang.ultimate.pvpmm.matchmaking;

import com.minkang.ultimate.pvpmm.Main;
import com.minkang.ultimate.pvpmm.manager.ArenaManager;
import com.minkang.ultimate.pvpmm.manager.MatchManager;
import com.minkang.ultimate.pvpmm.model.Arena;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.*;

public class Matchmaker {

    private final Plugin plugin;
    private final ArenaManager arenaManager;
    private final MatchManager matchManager;
    private final com.minkang.ultimate.pvpmm.service.TeamService teamService;

    private final LinkedList<java.util.UUID> soloQueue = new LinkedList<java.util.UUID>();
    private final LinkedList<Set<java.util.UUID>> duoQueue = new LinkedList<Set<java.util.UUID>>();
    private final Map<java.util.UUID, Long> enqueuedAt = new HashMap<java.util.UUID, Long>();

    private BukkitRunnable task;

    public Matchmaker(Plugin plugin, ArenaManager arenaManager, MatchManager matchManager, com.minkang.ultimate.pvpmm.service.TeamService teamService) {
        this.plugin = plugin; this.arenaManager = arenaManager; this.matchManager = matchManager; this.teamService = teamService;
    }

    public void start() {
        stop();
        int interval = Main.get().getConfig().getInt("queue.searchIntervalTicks", 60);
        task = new BukkitRunnable(){ @Override public void run(){ tick(); } };
        task.runTaskTimer(plugin, interval, interval);
    }

    public void stop(){ if(task!=null){ task.cancel(); task=null; } }

    public boolean enqueue(Player p) {
        if (matchManager.isInMatch(p.getUniqueId())) { p.sendMessage("§c[경쟁전] 이미 매치 중입니다."); return false; }
        if (enqueuedAt.containsKey(p.getUniqueId())) { p.sendMessage("§c[경쟁전] 이미 매칭 대기 중입니다. /경쟁전 매치 취소"); return false; }
        if (teamService.hasTeam(p)) {
            Set<java.util.UUID> team = teamService.getTeam(p.getUniqueId());
            if (team==null) team = teamService.getOrCreateSolo(p);
            if (team.size()>2) { p.sendMessage("§c[경쟁전] 팀 인원 초과."); return false; }
            boolean ok = true; for (java.util.UUID m : team) if (enqueuedAt.containsKey(m)) ok=false;
            if (!ok) { p.sendMessage("§c[경쟁전] 팀원 중 대기 중인 인원이 있습니다."); return false; }
            duoQueue.add(new HashSet<java.util.UUID>(team));
            for (java.util.UUID m : team) enqueuedAt.put(m, System.currentTimeMillis());
            broadcastJoin(team);
            return true;
        } else {
            soloQueue.add(p.getUniqueId());
            enqueuedAt.put(p.getUniqueId(), System.currentTimeMillis());
            Bukkit.broadcastMessage("§d[경쟁전] §f" + p.getName() + " §7매칭 대기중 (솔로)");
            return true;
        }
    }

    public boolean cancel(Player p) {
        boolean removed = soloQueue.remove(p.getUniqueId());
        if (!removed) {
            Set<java.util.UUID> found=null;
            for (Set<java.util.UUID> s : duoQueue) if (s.contains(p.getUniqueId())) { found=s; break; }
            if (found!=null) {
                duoQueue.remove(found);
                for (java.util.UUID m : found) enqueuedAt.remove(m);
                Bukkit.broadcastMessage("§7[경쟁전] " + toNames(found) + " §7매칭 대기 취소");
                return true;
            }
        }
        enqueuedAt.remove(p.getUniqueId());
        if (removed) Bukkit.broadcastMessage("§7[경쟁전] " + p.getName() + " §7매칭 대기 취소");
        return removed;
    }

    private void tick() {
        // actionbar status
        int solo = soloQueue.size();
        int duoCount = duoQueue.size();
        long now = System.currentTimeMillis();
        for (java.util.UUID u : new java.util.ArrayList<java.util.UUID>(enqueuedAt.keySet())) {
            Long t = enqueuedAt.get(u); if (t==null) continue;
            long sec = Math.max(0, (now - t.longValue())/1000);
            Player p = Bukkit.getPlayer(u);
            if (p!=null) p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§d경쟁전 대기 §7| §f"+sec+"초 §7| 솔로:§f"+solo+" §7듀오:§f"+duoCount));
        }

        // duo vs duo
        while (duoQueue.size() >= 2) {
            Set<java.util.UUID> a = duoQueue.poll();
            Set<java.util.UUID> b = duoQueue.poll();
            startAnyMatch(a, b);
        }

        // solo vs solo
        while (soloQueue.size() >= 2) {
            java.util.UUID a = soloQueue.poll();
            java.util.UUID b = soloQueue.poll();
            Set<java.util.UUID> sa = new HashSet<java.util.UUID>(); sa.add(a);
            Set<java.util.UUID> sb = new HashSet<java.util.UUID>(); sb.add(b);
            startAnyMatch(sa, sb);
        }

        // 1v2 after wait
        if (!soloQueue.isEmpty() && !duoQueue.isEmpty()) {
            java.util.UUID soloP = soloQueue.peek();
            Long since = enqueuedAt.get(soloP);
            long waited = since==null?0:(System.currentTimeMillis()-since.longValue());
            int need = Main.get().getConfig().getInt("rating.mismatchWaitSeconds", 45)*1000;
            if (waited >= need) {
                soloQueue.poll();
                Set<java.util.UUID> sa = new HashSet<java.util.UUID>(); sa.add(soloP);
                Set<java.util.UUID> duoTeam = duoQueue.poll();
                startAnyMatch(sa, duoTeam);
            }
        }
    }

    private void startAnyMatch(Set<java.util.UUID> a, Set<java.util.UUID> b) {
        Arena arena = pickArena();
        if (arena==null || !arena.isReady()) {
            if (a.size()==1) soloQueue.addAll(a); else duoQueue.add(a);
            if (b.size()==1) soloQueue.addAll(b); else duoQueue.add(b);
            return;
        }
        for (java.util.UUID u: a) enqueuedAt.remove(u);
        for (java.util.UUID u: b) enqueuedAt.remove(u);
        matchManager.startMatch(arena, a, b);
    }

    private Arena pickArena() {
        java.util.List<Arena> list = arenaManager.all();
        if (list.isEmpty()) return null;
        int idx = new java.util.Random().nextInt(list.size());
        return list.get(idx);
    }

    private void broadcastJoin(Set<java.util.UUID> team) {
        Bukkit.broadcastMessage("§d[경쟁전] §f" + toNames(team) + " §7매칭 대기중 (듀오)");
    }

    private String toNames(Set<java.util.UUID> team) {
        StringBuilder sb = new StringBuilder(); boolean first=true;
        for (java.util.UUID u: team){ Player p=Bukkit.getPlayer(u); String n=(p==null?"오프라인":p.getName()); if(!first) sb.append(" & "); sb.append(n); first=false; }
        return sb.toString();
    }
}
