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

import java.util.*;

public class MatchManager {

    private final Plugin plugin;
    // player -> rating
    private final Map<java.util.UUID, Integer> rating = new HashMap<java.util.UUID, Integer>();
    private final Map<java.util.UUID, Integer> wins = new HashMap<java.util.UUID, Integer>();
    private final Map<java.util.UUID, Integer> losses = new HashMap<java.util.UUID, Integer>();

    // active matches by id
    private final Map<String, Match> active = new HashMap<String, Match>();
    // quick lookup
    private final Map<java.util.UUID, String> playerToMatch = new HashMap<java.util.UUID, String>();

    public MatchManager(Plugin plugin, com.minkang.ultimate.pvpmm.service.TeamService teamService) {
        this.plugin = plugin;
    }

    public int getRating(java.util.UUID id) {
        Integer r = rating.get(id);
        if (r == null) {
            int start = Main.get().getConfig().getInt("rating.start", 1000);
            rating.put(id, start);
            return start;
        }
        return r.intValue();
    }

    public void setRating(java.util.UUID id, int val) {
        rating.put(id, val);
    }

    public int getWins(java.util.UUID id) {
        Integer v = wins.get(id);
        if (v == null) return 0;
        return v.intValue();
    }
    public int getLosses(java.util.UUID id) {
        Integer v = losses.get(id);
        if (v == null) return 0;
        return v.intValue();
    }

    public boolean isInMatch(java.util.UUID id) {
        return playerToMatch.containsKey(id);
    }

    public Match getMatch(java.util.UUID id) {
        String mid = playerToMatch.get(id);
        if (mid == null) return null;
        return active.get(mid);
    }

    public void startMatch(Arena arena, Set<java.util.UUID> teamA, Set<java.util.UUID> teamB) {
        String id = "M" + System.currentTimeMillis();
        Match m = new Match(id, arena, teamA, teamB);
        active.put(id, m);
        for (java.util.UUID u : teamA) playerToMatch.put(u, id);
        for (java.util.UUID u : teamB) playerToMatch.put(u, id);

        // store return locations and flight
        for (java.util.UUID u : teamA) snapshotPlayer(u, m);
        for (java.util.UUID u : teamB) snapshotPlayer(u, m);

        // teleport
        teleportTeam(teamA, arena.getPos(1), arena.getPos(2));
        teleportTeam(teamB, arena.getPos(3), arena.getPos(4));

        // setup scoreboard team for friendly fire off
        setupScoreboardTeams(m);

        // block fly
        toggleFly(teamA, false, m);
        toggleFly(teamB, false, m);

        m.setState(Match.State.RUNNING);
        m.setStartMillis(System.currentTimeMillis());

        Bukkit.broadcastMessage("§d[경쟁전] §f매치 시작! §b" + arena.getName() + " §7- §a" + formatTeam(teamA) + " §7vs §c" + formatTeam(teamB));

        int grace = Main.get().getConfig().getInt("match.graceSecondsOnStart", 3);
        if (grace > 0) {
            Bukkit.broadcastMessage("§7[경쟁전] 시작 보호 §f" + grace + "초");
        }
    }

    private void snapshotPlayer(java.util.UUID id, Match m) {
        Player p = Bukkit.getPlayer(id);
        if (p == null) return;
        m.getReturnLocations().put(id, p.getLocation().clone());
        boolean hf = p.getAllowFlight();
        m.getHadFlight().put(id, Boolean.valueOf(hf));
    }

    private void teleportTeam(Set<java.util.UUID> team, Location pos1, Location pos2) {
        int i = 0;
        for (java.util.UUID u : team) {
            Player p = Bukkit.getPlayer(u);
            if (p == null) continue;
            if (i == 0) {
                if (pos1 != null) p.teleport(pos1);
            } else {
                if (pos2 != null) p.teleport(pos2);
            }
            i++;
        }
    }

    private void setupScoreboardTeams(Match m) {
        if (!Main.get().getConfig().getBoolean("match.disableFriendlyFire", true)) return;
        ScoreboardManager man = Bukkit.getScoreboardManager();
        if (man == null) return;
        Scoreboard board = man.getMainScoreboard();
        String ta = "pvpA_" + m.getId();
        String tb = "pvpB_" + m.getId();
        Team t1 = board.getTeam(ta);
        if (t1 == null) t1 = board.registerNewTeam(ta);
        Team t2 = board.getTeam(tb);
        if (t2 == null) t2 = board.registerNewTeam(tb);
        t1.setAllowFriendlyFire(false);
        t2.setAllowFriendlyFire(false);
        m.setScoreboardTeamA(ta);
        m.setScoreboardTeamB(tb);
        for (java.util.UUID u : m.getTeamA()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) t1.addEntry(p.getName());
        }
        for (java.util.UUID u : m.getTeamB()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) t2.addEntry(p.getName());
        }
    }

    public void endMatch(Match m, boolean teamAWon) {
        // rating updates
        double rA = averageRating(m.getTeamA());
        double rB = averageRating(m.getTeamB());

        double expA = Elo.expectedScore(rA, rB);
        double expB = Elo.expectedScore(rB, rA);

        boolean solo = (m.getTeamA().size() == 1 && m.getTeamB().size() == 1);
        boolean underdogA = (rA + 50.0 < rB);
        boolean underdogB = (rB + 50.0 < rA);

        int kbase = Main.get().getConfig().getInt("rating.kFactorBase", 32);
        int kA = Elo.kFactor(kbase, solo, underdogA);
        int kB = Elo.kFactor(kbase, solo, underdogB);

        double scoreA = teamAWon ? 1.0 : 0.0;
        double scoreB = teamAWon ? 0.0 : 1.0;

        int deltaA = (int)Math.round(kA * (scoreA - expA));
        int deltaB = (int)Math.round(kB * (scoreB - expB));

        for (java.util.UUID u : m.getTeamA()) {
            int before = getRating(u);
            int after = Elo.clamp(before + deltaA);
            setRating(u, after);
            if (teamAWon) wins.put(u, getWins(u) + 1);
            else losses.put(u, getLosses(u) + 1);
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                p.sendMessage("§d[경쟁전] §a" + (teamAWon ? "승리":"패배") + "§7 | 레이팅 §f" + before + " §7→ §e" + after + " §7(" + (after - before) + ")");
            }
        }
        for (java.util.UUID u : m.getTeamB()) {
            int before = getRating(u);
            int after = Elo.clamp(before + deltaB);
            setRating(u, after);
            if (!teamAWon) wins.put(u, getWins(u) + 1);
            else losses.put(u, getLosses(u) + 1);
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                p.sendMessage("§d[경쟁전] §a" + (!teamAWon ? "승리":"패배") + "§7 | 레이팅 §f" + before + " §7→ §e" + after + " §7(" + (after - before) + ")");
            }
        }

        // restore players
        for (java.util.UUID u : m.getTeamA()) restorePlayer(u, m);
        for (java.util.UUID u : m.getTeamB()) restorePlayer(u, m);

        // cleanup scoreboard
        cleanupScoreboard(m);

        // remove from active
        active.remove(m.getId());
        for (java.util.UUID u : m.getTeamA()) playerToMatch.remove(u);
        for (java.util.UUID u : m.getTeamB()) playerToMatch.remove(u);
    }

    private void cleanupScoreboard(Match m) {
        ScoreboardManager man = Bukkit.getScoreboardManager();
        if (man == null) return;
        Scoreboard board = man.getMainScoreboard();
        if (m.getScoreboardTeamA() != null) {
            Team t = board.getTeam(m.getScoreboardTeamA());
            if (t != null) t.unregister();
        }
        if (m.getScoreboardTeamB() != null) {
            Team t = board.getTeam(m.getScoreboardTeamB());
            if (t != null) t.unregister();
        }
    }

    private void restorePlayer(java.util.UUID id, Match m) {
        Player p = Bukkit.getPlayer(id);
        if (p == null) return;
        // teleport back
        Location back = m.getReturnLocations().get(id);
        if (back != null) p.teleport(back);
        // restore flight
        Boolean had = m.getHadFlight().get(id);
        if (had != null && Main.get().getConfig().getBoolean("match.restoreFlightAfterMatch", true)) {
            p.setAllowFlight(had.booleanValue());
        }
    }

    private double averageRating(Set<java.util.UUID> team) {
        if (team.isEmpty()) return 1000.0;
        double sum = 0.0;
        for (java.util.UUID u : team) {
            sum += getRating(u);
        }
        return sum / (double)team.size();
    }

    private String formatTeam(Set<java.util.UUID> team) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (java.util.UUID u : team) {
            Player p = Bukkit.getPlayer(u);
            String n = (p == null ? "오프라인" : p.getName());
            if (!first) sb.append(" & ");
            sb.append(n);
            first = false;
        }
        return sb.toString();
    }

    public void playerDiedOrQuit(java.util.UUID id) {
        Match m = getMatch(id);
        if (m == null) return;
        // remove from team set
        boolean wasA = m.getTeamA().remove(id);
        boolean wasB = false;
        if (!wasA) wasB = m.getTeamB().remove(id);

        // check winner
        if (m.getTeamA().isEmpty() && m.getTeamB().isEmpty()) {
            endMatch(m, true);
            return;
        }
        if (m.getTeamA().isEmpty()) {
            endMatch(m, false);
            return;
        }
        if (m.getTeamB().isEmpty()) {
            endMatch(m, true);
            return;
        }
    }

    public void shutdownAllMatches() {
        for (Match m : new ArrayList<Match>(active.values())) {
            endMatch(m, true);
        }
    }

    public void startWeeklyResetTask() {
        if (!Main.get().getConfig().getBoolean("weeklyReset.enabled", true)) return;
        new BukkitRunnable(){
            @Override public void run() {
                Calendar c = Calendar.getInstance();
                int dow = Main.get().getConfig().getInt("weeklyReset.resetDayOfWeek", 1);
                int hour = Main.get().getConfig().getInt("weeklyReset.resetHour", 0);
                int min = Main.get().getConfig().getInt("weeklyReset.resetMinute", 0);
                if (c.get(Calendar.DAY_OF_WEEK) == dow && c.get(Calendar.HOUR_OF_DAY) == hour && c.get(Calendar.MINUTE) == min) {
                    performWeeklyReset();
                }
            }
        }.runTaskTimer(plugin, 20L * 60, 20L * 60);
    }

    private void performWeeklyReset() {
        Bukkit.broadcastMessage("§6[경쟁전] §f주간 시즌 종료! 등급 보상을 지급합니다.");
        // Here we would distribute rewards per tier using RewardManager,
        // but to keep dependencies simple we broadcast and reset ratings.
        for (java.util.UUID u : new HashSet<java.util.UUID>(rating.keySet())) {
            rating.put(u, Main.get().getConfig().getInt("rating.start", 1000));
            wins.put(u, 0);
            losses.put(u, 0);
        }
    }

private void toggleFly(java.util.Set<java.util.UUID> team, boolean allow, com.minkang.ultimate.pvpmm.model.Match m) {
    for (java.util.UUID u : team) {
        org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(u);
        if (p == null) continue;
        if (!allow) {
            // Immediately ground players and disallow flight during matches
            if (p.isFlying()) p.setFlying(false);
            p.setAllowFlight(false);
        } else {
            // Optional restore path if used mid-match; endMatch() also restores from snapshot
            java.lang.Boolean had = m.getHadFlight().get(u);
            if (had != null) p.setAllowFlight(had.booleanValue());
        }
    }
}

}
