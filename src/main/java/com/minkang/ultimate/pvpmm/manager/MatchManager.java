package com.minkang.ultimate.pvpmm.manager;

import com.minkang.ultimate.pvpmm.Main;
import com.minkang.ultimate.pvpmm.model.Arena;
import com.minkang.ultimate.pvpmm.model.Match;
import com.minkang.ultimate.pvpmm.util.Elo;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.*;

public class MatchManager {

    private final Plugin plugin;
    private final Map<java.util.UUID, Integer> rating = new HashMap<java.util.UUID, Integer>();
    private final Map<java.util.UUID, Integer> wins = new HashMap<java.util.UUID, Integer>();
    private final Map<java.util.UUID, Integer> losses = new HashMap<java.util.UUID, Integer>();

    private final Map<String, Match> active = new HashMap<String, Match>();
    private final Map<java.util.UUID, String> playerToMatch = new HashMap<java.util.UUID, String>();

    public MatchManager(Plugin plugin, com.minkang.ultimate.pvpmm.service.TeamService teamService) {
        this.plugin = plugin;
    }

    public int getRating(java.util.UUID id) { return rating.computeIfAbsent(id, k-> Main.get().getConfig().getInt("rating.start", 1000)); }
    public void setRating(java.util.UUID id, int val) { rating.put(id, val); }
    public int getWins(java.util.UUID id){ return wins.getOrDefault(id, 0); }
    public int getLosses(java.util.UUID id){ return losses.getOrDefault(id, 0); }

    public boolean isInMatch(java.util.UUID id){ return playerToMatch.containsKey(id); }
    public Match getMatch(java.util.UUID id){ String mid=playerToMatch.get(id); return mid==null?null:active.get(mid); }

    public void startMatch(Arena arena, Set<java.util.UUID> teamA, Set<java.util.UUID> teamB) {
        String id = java.lang.Long.toString(System.currentTimeMillis(), 36).toUpperCase(java.util.Locale.ROOT);
        Match m = new Match(id, arena, teamA, teamB);
        active.put(id, m);
        for (java.util.UUID u : teamA) playerToMatch.put(u, id);
        for (java.util.UUID u : teamB) playerToMatch.put(u, id);

        for (java.util.UUID u : teamA) snapshotPlayer(u, m);
        for (java.util.UUID u : teamB) snapshotPlayer(u, m);

        teleportTeam(teamA, arena.getPos(1), arena.getPos(2));
        teleportTeam(teamB, arena.getPos(3), arena.getPos(4));

        setupScoreboardTeams(m);

        toggleFly(teamA, false, m);
        toggleFly(teamB, false, m);

        m.setState(Match.State.PREP);
        m.setStartMillis(System.currentTimeMillis());

        Bukkit.broadcastMessage("§d[경쟁전] §f매치 매칭! §b" + arena.getName() + " §7- §a" + formatTeamWithTier(teamA) + " §7vs §c" + formatTeamWithTier(teamB));
        sendTitleToMatch(m, "§d경쟁전 매치", "§7잠시 후 시작합니다", 5, 30, 5);
        new BukkitRunnable(){ int n=3; @Override public void run(){
            if (n==0){ sendTitleToMatch(m,"§aSTART!","",0,20,10); m.setState(Match.State.RUNNING); cancel(); return; }
            sendTitleToMatch(m,"§e"+n,"§7곧 시작",0,20,0);
            n--;
        }}.runTaskTimer(plugin, 20L, 20L);
    }

    private void snapshotPlayer(java.util.UUID id, Match m) {
        Player p = Bukkit.getPlayer(id); if (p==null) return;
        m.getReturnLocations().put(id, p.getLocation().clone());
        m.getHadFlight().put(id, Boolean.valueOf(p.getAllowFlight()));
    }

    private void teleportTeam(Set<java.util.UUID> team, Location pos1, Location pos2) {
        int i=0;
        for (java.util.UUID u : team) {
            Player p = Bukkit.getPlayer(u); if (p==null) continue;
            p.teleport(i==0?pos1:pos2); i++;
        }
    }

    private void setupScoreboardTeams(Match m) {
        if (!Main.get().getConfig().getBoolean("match.disableFriendlyFire", true)) return;
        ScoreboardManager man = Bukkit.getScoreboardManager(); if (man==null) return;
        Scoreboard board = man.getMainScoreboard();
        String ta = "A_" + m.getId(); String tb = "B_" + m.getId();
        if (ta.length()>16) ta = ta.substring(0,16);
        if (tb.length()>16) tb = tb.substring(0,16);
        Team t1 = board.getTeam(ta); if (t1==null) t1 = board.registerNewTeam(ta);
        Team t2 = board.getTeam(tb); if (t2==null) t2 = board.registerNewTeam(tb);
        t1.setAllowFriendlyFire(false); t2.setAllowFriendlyFire(false);
        m.setScoreboardTeamA(ta); m.setScoreboardTeamB(tb);
        for (java.util.UUID u : m.getTeamA()) { Player p=Bukkit.getPlayer(u); if(p!=null) t1.addEntry(p.getName()); }
        for (java.util.UUID u : m.getTeamB()) { Player p=Bukkit.getPlayer(u); if(p!=null) t2.addEntry(p.getName()); }
    }

    public void endMatch(Match m, boolean teamAWon) {
        if (m.getState()==Match.State.ENDED) return;
        m.setState(Match.State.ENDED);

        double rA = averageRating(m.getTeamA());
        double rB = averageRating(m.getTeamB());
        double expA = Elo.expectedScore(rA, rB);
        double expB = Elo.expectedScore(rB, rA);
        boolean solo = (m.getTeamA().size()==1 && m.getTeamB().size()==1);
        boolean underdogA = (rA + 50.0 < rB);
        boolean underdogB = (rB + 50.0 < rA);
        int kbase = Main.get().getConfig().getInt("rating.kFactorBase", 32);
        int kA = Elo.kFactor(kbase, solo, underdogA);
        int kB = Elo.kFactor(kbase, solo, underdogB);
        double scoreA = teamAWon ? 1.0 : 0.0;
        double scoreB = teamAWon ? 0.0 : 1.0;
        int deltaA = (int)Math.round(kA * (scoreA - expA));
        int deltaB = (int)Math.round(kB * (scoreB - expB));

        Bukkit.broadcastMessage("§d[경쟁전] §f매치 종료! §7승자: " + (teamAWon? "§a"+formatTeam(m.getTeamA()):"§c"+formatTeam(m.getTeamB())));

        for (java.util.UUID u : m.getTeamA()) {
            int before = getRating(u);
            int after = Elo.clamp(before + deltaA);
            setRating(u, after);
            if (teamAWon) wins.put(u, getWins(u)+1); else losses.put(u, getLosses(u)+1);
            Player p = Bukkit.getPlayer(u);
            if (p!=null) p.sendMessage("§d[경쟁전] §a" + (teamAWon?"승리":"패배") + "§7 | 레이팅 §f" + before + " §7→ §e" + after + " §7(" + (after-before) + ")");
        }
        for (java.util.UUID u : m.getTeamB()) {
            int before = getRating(u);
            int after = Elo.clamp(before + deltaB);
            setRating(u, after);
            if (!teamAWon) wins.put(u, getWins(u)+1); else losses.put(u, getLosses(u)+1);
            Player p = Bukkit.getPlayer(u);
            if (p!=null) p.sendMessage("§d[경쟁전] §a" + (!teamAWon?"승리":"패배") + "§7 | 레이팅 §f" + before + " §7→ §e" + after + " §7(" + (after-before) + ")");
        }

        for (java.util.UUID u : new java.util.HashSet<java.util.UUID>(m.getTeamA())) restoreAndUnmap(u, m);
        for (java.util.UUID u : new java.util.HashSet<java.util.UUID>(m.getTeamB())) restoreAndUnmap(u, m);

        // Compact rating summary
        StringBuilder sA = new StringBuilder();
        for (java.util.UUID u : m.getTeamA()) {
            org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(u);
            String name = (op!=null?op.getName():"Unknown");
            sA.append(" ").append(name).append("§7(").append(getRating(u)).append(")");
        }
        StringBuilder sB = new StringBuilder();
        for (java.util.UUID u : m.getTeamB()) {
            org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(u);
            String name = (op!=null?op.getName():"Unknown");
            sB.append(" ").append(name).append("§7(").append(getRating(u)).append(")");
        }
        org.bukkit.Bukkit.broadcastMessage("§d[경쟁전] §f레이팅 변동§7 - §aA:" + sA.toString() + " §7| §cB:" + sB.toString());

        cleanupScoreboard(m);
        active.remove(m.getId());
    }

    private void restoreAndUnmap(java.util.UUID id, Match m) {
        playerToMatch.remove(id);
        restorePlayer(id, m);
    }

    private void cleanupScoreboard(Match m) {
        ScoreboardManager man = Bukkit.getScoreboardManager(); if (man==null) return;
        Scoreboard board = man.getMainScoreboard();
        if (m.getScoreboardTeamA()!=null) { Team t=board.getTeam(m.getScoreboardTeamA()); if (t!=null) t.unregister(); }
        if (m.getScoreboardTeamB()!=null) { Team t=board.getTeam(m.getScoreboardTeamB()); if (t!=null) t.unregister(); }
    }

    private void restorePlayer(java.util.UUID id, Match m) {
        Player p = Bukkit.getPlayer(id); if (p==null) return;
        Location back = m.getReturnLocations().get(id);
        if (back!=null) p.teleport(back);
        Boolean had = m.getHadFlight().get(id);
        if (had!=null && Main.get().getConfig().getBoolean("match.restoreFlightAfterMatch", true)) p.setAllowFlight(had.booleanValue());
    }

    private double averageRating(Set<java.util.UUID> team){ if (team.isEmpty()) return 1000.0; double s=0; for(java.util.UUID u:team) s+=getRating(u); return s/team.size(); }
    private String formatTeam(Set<java.util.UUID> team) {
        StringBuilder sb=new StringBuilder(); boolean first=true;
        for (java.util.UUID u:team){ Player p=Bukkit.getPlayer(u); String n=(p==null?"오프라인":p.getName()); if(!first) sb.append(" & "); sb.append(n); first=false; }
        return sb.toString();
    }
    private String formatTeamWithTier(Set<java.util.UUID> team) {
        StringBuilder sb=new StringBuilder(); boolean first=true;
        for (java.util.UUID u:team){ Player p=Bukkit.getPlayer(u); String n=(p==null?"오프라인":p.getName()); int r=getRating(u); String t=getTierName(r); if(!first) sb.append(" & "); sb.append(n).append("§7(").append(t).append("§7)"); first=false; }
        return sb.toString();
    }

    private void sendTitleToMatch(Match m, String title, String sub, int fi, int st, int fo) {
        for (java.util.UUID u : m.getTeamA()) { Player p=Bukkit.getPlayer(u); if (p!=null) p.sendTitle(title, sub, fi, st, fo); }
        for (java.util.UUID u : m.getTeamB()) { Player p=Bukkit.getPlayer(u); if (p!=null) p.sendTitle(title, sub, fi, st, fo); }
    }

    private void removeFromScoreboardTeam(Match m, java.util.UUID id) {
        ScoreboardManager man = Bukkit.getScoreboardManager(); if (man==null) return;
        Scoreboard board = man.getMainScoreboard();
        if (m.getScoreboardTeamA()!=null) { Team t=board.getTeam(m.getScoreboardTeamA()); Player p=Bukkit.getPlayer(id); if(t!=null&&p!=null) t.removeEntry(p.getName()); }
        if (m.getScoreboardTeamB()!=null) { Team t=board.getTeam(m.getScoreboardTeamB()); Player p=Bukkit.getPlayer(id); if(t!=null&&p!=null) t.removeEntry(p.getName()); }
    }

    private void toggleFly(java.util.Set<java.util.UUID> team, boolean allow, Match m) {
        for (java.util.UUID u : team) {
            Player p = Bukkit.getPlayer(u); if (p==null) continue;
            if (!allow) { if (p.isFlying()) p.setFlying(false); p.setAllowFlight(false); }
            else { Boolean had = m.getHadFlight().get(u); if (had!=null) p.setAllowFlight(had.booleanValue()); }
        }
    }

    public void playerDiedOrQuit(java.util.UUID id) {
        Match m = getMatch(id);
        if (m==null) return;
        boolean wasA = m.getTeamA().remove(id);
        if (!wasA) m.getTeamB().remove(id);
        playerToMatch.remove(id);
        removeFromScoreboardTeam(m, id);
        new BukkitRunnable(){ @Override public void run(){ restorePlayer(id, m); }}.runTaskLater(plugin, 20L);

        if (m.getTeamA().isEmpty() && m.getTeamB().isEmpty()) { endMatch(m, true); return; }
        if (m.getTeamA().isEmpty()) { endMatch(m, false); return; }
        if (m.getTeamB().isEmpty()) { endMatch(m, true); return; }
    }

    public void shutdownAllMatches(){
        for (Match m : new ArrayList<Match>(active.values())) endMatch(m, true);
    }

    public void startWeeklyResetTask() {
        if (!Main.get().getConfig().getBoolean("weeklyReset.enabled", true)) return;
        new BukkitRunnable(){ @Override public void run(){
            Calendar c = Calendar.getInstance();
            int dow = Main.get().getConfig().getInt("weeklyReset.resetDayOfWeek", 1);
            int hour = Main.get().getConfig().getInt("weeklyReset.resetHour", 0);
            int min = Main.get().getConfig().getInt("weeklyReset.resetMinute", 0);
            if (c.get(Calendar.DAY_OF_WEEK)==dow && c.get(Calendar.HOUR_OF_DAY)==hour && c.get(Calendar.MINUTE)==min) performWeeklyReset();
        }}.runTaskTimer(plugin, 20L*60, 20L*60);
    }

    private void performWeeklyReset() {
        Bukkit.broadcastMessage("§6[경쟁전] §f주간 시즌 종료! 등급 보상을 지급합니다.");
        for (java.util.UUID u : new HashSet<java.util.UUID>(rating.keySet())) {
            rating.put(u, Main.get().getConfig().getInt("rating.start", 1000));
            wins.put(u, 0); losses.put(u, 0);
        }
    }

    public String getTierName(int rate) {
        org.bukkit.configuration.file.FileConfiguration c = Main.get().getConfig();
        java.util.List<?> tiers = c.getList("tiers");
        String last = "UNRANKED";
        if (tiers != null) {
            for (Object o : tiers) {
                if (o instanceof java.util.Map) {
                    java.util.Map<?,?> m = (java.util.Map<?,?>)o;
                    Object name = m.get("name"); Object min = m.get("min");
                    if (name != null && min instanceof Number) {
                        int mn = ((Number)min).intValue();
                        if (rate >= mn) last = String.valueOf(name);
                    }
                }
            }
        }
        return last;
    }

    public java.util.List<java.util.UUID> topRankings(int limit) {
        java.util.List<java.util.Map.Entry<java.util.UUID, Integer>> list = new java.util.ArrayList<java.util.Map.Entry<java.util.UUID, Integer>>(rating.entrySet());
        java.util.Collections.sort(list, new java.util.Comparator<java.util.Map.Entry<java.util.UUID, Integer>>() {
            public int compare(java.util.Map.Entry<java.util.UUID, Integer> a, java.util.Map.Entry<java.util.UUID, Integer> b) {
                return Integer.compare(b.getValue().intValue(), a.getValue().intValue());
            }
        });
        java.util.List<java.util.UUID> out = new java.util.ArrayList<java.util.UUID>();
        int i=0; for (java.util.Map.Entry<java.util.UUID, Integer> e : list){ out.add(e.getKey()); i++; if(i>=limit) break; }
        return out;
    }
}
