package com.minkang.ultimate.pvpmm.manager;
import com.minkang.ultimate.pvpmm.Main;
import com.minkang.ultimate.pvpmm.model.Arena;
import com.minkang.ultimate.pvpmm.model.Match;
import com.minkang.ultimate.pvpmm.util.Elo;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard; import org.bukkit.scoreboard.ScoreboardManager; import org.bukkit.scoreboard.Team;
import java.util.*;
public class MatchManager {
    private java.io.File rankFile;
    private FileConfiguration rankCfg;

    private void ensureRankConfig() {
        if (rankFile == null) {
            rankFile = new java.io.File(Main.get().getDataFolder(), "rankings.yml");
        }
        if (rankCfg == null) {
            try {
                if (!rankFile.getParentFile().exists()) rankFile.getParentFile().mkdirs();
            } catch (Throwable ignored) {}
            rankCfg = YamlConfiguration.loadConfiguration(rankFile);
            org.bukkit.configuration.ConfigurationSection sec = rankCfg.getConfigurationSection("players");
            if (sec != null) {
                for (String key : sec.getKeys(false)) {
                    try {
                        java.util.UUID id = java.util.UUID.fromString(key);
                        rating.put(id, sec.getInt(key + ".rating", Main.get().getConfig().getInt("rating.start", 1000)));
                        wins.put(id, sec.getInt(key + ".wins", 0));
                        losses.put(id, sec.getInt(key + ".losses", 0));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
    }

    public void save() {
        ensureRankConfig();
        if (rankCfg == null) return;
        rankCfg.set("players", null);
        for (java.util.Map.Entry<java.util.UUID, Integer> e : rating.entrySet()) {
            java.util.UUID id = e.getKey();
            String base = "players." + id.toString();
            rankCfg.set(base + ".rating", e.getValue());
            rankCfg.set(base + ".wins", wins.getOrDefault(id, 0));
            rankCfg.set(base + ".losses", losses.getOrDefault(id, 0));
        }
        try { rankCfg.save(rankFile); } catch (java.io.IOException ex) { ex.printStackTrace(); }
    }

    private final Plugin plugin;
    private final com.minkang.ultimate.pvpmm.service.TeamService teamService;
    private final Map<java.util.UUID, Integer> rating = new HashMap<java.util.UUID, Integer>();
    private final Map<java.util.UUID, Integer> wins = new HashMap<java.util.UUID, Integer>();
    private final Map<java.util.UUID, Integer> losses = new HashMap<java.util.UUID, Integer>();
    private final Map<String, Match> active = new HashMap<String, Match>();
    private final Map<java.util.UUID, String> playerToMatch = new HashMap<java.util.UUID, String>();
    public MatchManager(Plugin plugin, com.minkang.ultimate.pvpmm.service.TeamService teamService){ this.plugin=plugin; this.teamService=teamService; }
    public int getRating(java.util.UUID id){ ensureRankConfig();  return rating.computeIfAbsent(id, k -> Main.get().getConfig().getInt("rating.start", 1000)); }
    public void setRating(java.util.UUID id, int val){ rating.put(id, val); }
    public int getWins(java.util.UUID id){ return wins.getOrDefault(id, 0); }
    public int getLosses(java.util.UUID id){ return losses.getOrDefault(id, 0); }
    public boolean isInMatch(java.util.UUID id){ return playerToMatch.containsKey(id); }
    public Match getMatch(java.util.UUID id){ String mid=playerToMatch.get(id); return mid==null?null:active.get(mid); }

    public void startMatch(Arena arena, Set<java.util.UUID> teamA, Set<java.util.UUID> teamB){
        String id="M"+System.currentTimeMillis();
        Match m=new Match(id, arena, teamA, teamB);
        active.put(id, m);
        for(java.util.UUID u: teamA) playerToMatch.put(u, id);
        for(java.util.UUID u: teamB) playerToMatch.put(u, id);
        for(java.util.UUID u: teamA) snapshotPlayer(u, m);
        for(java.util.UUID u: teamB) snapshotPlayer(u, m);
        teleportTeam(teamA, arena.getPos(1), arena.getPos(2));
        teleportTeam(teamB, arena.getPos(3), arena.getPos(4));
        setupScoreboardTeams(m);
        toggleFly(teamA, false, m); toggleFly(teamB, false, m);
        m.setState(Match.State.PREP); m.setStartMillis(System.currentTimeMillis());
        Bukkit.broadcastMessage("§d[경쟁전] §f매치 매칭! §b"+arena.getName()+" §7- §a"+formatTeamWithTier(teamA)+" §7vs §c"+formatTeamWithTier(teamB));
        sendTitleToMatch(m,"§d경쟁전 매치","§7잠시 후 시작합니다",5,30,5);
        new BukkitRunnable(){ int n=3; @Override public void run(){ if(n==0){ sendTitleToMatch(m,"§aSTART!","",0,20,10); sendTitleToMatch(m,"§a시작!","",0,20,10); m.setState(Match.State.RUNNING); cancel(); return; } sendTitleToMatch(m,"§e"+n,"§7곧 시작",0,20,0); n--; } }.runTaskTimer(plugin,20L,20L);
    }
    private void snapshotPlayer(java.util.UUID id, Match m){ Player p=Bukkit.getPlayer(id); if(p==null) return; m.getReturnLocations().put(id, p.getLocation().clone()); m.getHadFlight().put(id, p.getAllowFlight()); }
    private void teleportTeam(Set<java.util.UUID> team, Location p1, Location p2){ int i=0; for(java.util.UUID u: team){ Player p=Bukkit.getPlayer(u); if(p!=null) p.teleport((i++==0)?p1:p2); } }
    private void setupScoreboardTeams(Match m){
        if(!Main.get().getConfig().getBoolean("match.disableFriendlyFire", true)) return;
        ScoreboardManager man=Bukkit.getScoreboardManager(); if(man==null) return; Scoreboard board=man.getMainScoreboard();
        String ta="pvpA_"+m.getId(), tb="pvpB_"+m.getId();
        Team t1=board.getTeam(ta); if(t1==null) t1=board.registerNewTeam(ta);
        Team t2=board.getTeam(tb); if(t2==null) t2=board.registerNewTeam(tb);
        t1.setAllowFriendlyFire(false); t2.setAllowFriendlyFire(false);
        m.setScoreboardTeamA(ta); m.setScoreboardTeamB(tb);
        for(java.util.UUID u:m.getTeamA()){ Player p=Bukkit.getPlayer(u); if(p!=null) t1.addEntry(p.getName()); }
        for(java.util.UUID u:m.getTeamB()){ Player p=Bukkit.getPlayer(u); if(p!=null) t2.addEntry(p.getName()); }
    }
    public void endMatch(Match m, boolean teamAWon){
        if(m.getState()==Match.State.ENDED) return;
        m.setState(Match.State.ENDED);
        double rA=averageRating(m.getTeamA()); double rB=averageRating(m.getTeamB());
        double expA=Elo.expectedScore(rA, rB); double expB=Elo.expectedScore(rB, rA);
        boolean solo=(m.getTeamA().size()==1 && m.getTeamB().size()==1);
        boolean underdogA=(rA+50.0<rB); boolean underdogB=(rB+50.0<rA);
        int kbase=Main.get().getConfig().getInt("rating.kFactorBase", 32);
        int kA=kbase + (solo?Main.get().getConfig().getInt("rating.kFactorSoloBonus",4):0) + (underdogA?Main.get().getConfig().getInt("rating.kFactorUnderdogBonus",8):0);
        int kB=kbase + (solo?Main.get().getConfig().getInt("rating.kFactorSoloBonus",4):0) + (underdogB?Main.get().getConfig().getInt("rating.kFactorUnderdogBonus",8):0);
        double sA=teamAWon?1.0:0.0, sB=teamAWon?0.0:1.0;
        int dA=(int)Math.round(kA*(sA-expA)); int dB=(int)Math.round(kB*(sB-expB));

        Bukkit.broadcastMessage("§d[경쟁전] §f매치 종료! §7승자: "+(teamAWon? "§a"+formatTeam(m.getTeamA()):"§c"+formatTeam(m.getTeamB())));

        for(java.util.UUID u:m.getTeamA()){
            int before=getRating(u);
            int after=before + dA; int min=Main.get().getConfig().getInt("rating.min",0); int max=Main.get().getConfig().getInt("rating.max",3000); if(after<min) after=min; if(after>max) after=max;
            setRating(u, after); if(teamAWon) wins.put(u, getWins(u)+1); else losses.put(u, getLosses(u)+1);
            Player p=Bukkit.getPlayer(u); if(p!=null) p.sendMessage("§d[경쟁전] §a"+(teamAWon?"승리":"패배")+"§7 | 레이팅 §f"+before+" §7→ §e"+after+" §7("+(after-before)+")");
        }
        for(java.util.UUID u:m.getTeamB()){
            int before=getRating(u);
            int after=before + dB; int min=Main.get().getConfig().getInt("rating.min",0); int max=Main.get().getConfig().getInt("rating.max",3000); if(after<min) after=min; if(after>max) after=max;
            setRating(u, after); if(!teamAWon) wins.put(u, getWins(u)+1); else losses.put(u, getLosses(u)+1);
            Player p=Bukkit.getPlayer(u); if(p!=null) p.sendMessage("§d[경쟁전] §a"+(!teamAWon?"승리":"패배")+"§7 | 레이팅 §f"+before+" §7→ §e"+after+" §7("+(after-before)+")");
        }
        for(java.util.UUID u:new java.util.HashSet<java.util.UUID>(m.getTeamA())) restoreAndUnmap(u, m);
        for(java.util.UUID u:new java.util.HashSet<java.util.UUID>(m.getTeamB())) restoreAndUnmap(u, m);

        StringBuilder sbTeamA=new StringBuilder(); for(java.util.UUID u:m.getTeamA()){ org.bukkit.OfflinePlayer op=Bukkit.getOfflinePlayer(u); sbTeamA.append(" ").append(op!=null?op.getName():"Unknown").append("§7(").append(getRating(u)).append(")"); }
        StringBuilder sbTeamB=new StringBuilder(); 
        // Persist rankings after match
        try { save(); } catch (Throwable ignored) {}
for(java.util.UUID u:m.getTeamB()){ org.bukkit.OfflinePlayer op=Bukkit.getOfflinePlayer(u); sbTeamB.append(" ").append(op!=null?op.getName():"Unknown").append("§7(").append(getRating(u)).append(")"); }
        Bukkit.broadcastMessage("§d[경쟁전] §f레이팅 변동§7 - §aA(" + (dA>=0? "+"+dA : String.valueOf(dA)) + "):" + sbTeamA.toString() + " §7| §cB(" + (dB>=0? "+"+dB : String.valueOf(dB)) + "):" + sbTeamB.toString());

        cleanupScoreboard(m);
        forceUnmapAll(m);
        if (Main.get().getConfig().getBoolean("match.clearTeamOnEnd", true)) clearTeams(m);
        active.remove(m.getId());
    }
    private void restoreAndUnmap(java.util.UUID id, Match m){ playerToMatch.remove(id); restorePlayer(id, m); }
    private void cleanupScoreboard(Match m){ ScoreboardManager man=Bukkit.getScoreboardManager(); if(man==null) return; Scoreboard board=man.getMainScoreboard(); if(m.getScoreboardTeamA()!=null){ Team t=board.getTeam(m.getScoreboardTeamA()); if(t!=null) t.unregister(); } if(m.getScoreboardTeamB()!=null){ Team t=board.getTeam(m.getScoreboardTeamB()); if(t!=null) t.unregister(); } }
    private void restorePlayer(java.util.UUID id, Match m){ Player p=Bukkit.getPlayer(id); if(p==null) return; org.bukkit.Location back=m.getReturnLocations().get(id); if(back!=null) p.teleport(back); Boolean had=m.getHadFlight().get(id); if(had!=null && Main.get().getConfig().getBoolean("match.restoreFlightAfterMatch", true)) p.setAllowFlight(had); }
    private double averageRating(Set<java.util.UUID> team){ if(team.isEmpty()) return 1000.0; double s=0; for(java.util.UUID u:team) s+=getRating(u); return s/team.size(); }
    private String formatTeam(Set<java.util.UUID> t){ StringBuilder sb=new StringBuilder(); boolean first=true; for(java.util.UUID u:t){ Player p=Bukkit.getPlayer(u); String n=(p==null?"오프라인":p.getName()); if(!first) sb.append(" & "); sb.append(n); first=false; } return sb.toString(); }
    private String formatTeamWithTier(Set<java.util.UUID> t){ StringBuilder sb=new StringBuilder(); boolean first=true; for(java.util.UUID u:t){ Player p=Bukkit.getPlayer(u); String n=(p==null?"오프라인":p.getName()); int r=getRating(u); String tier=getTierName(r); if(!first) sb.append(" & "); sb.append(n).append("§7(").append(tier).append("§7)"); first=false; } return sb.toString(); }
    private void sendTitleToMatch(Match m, String title, String sub, int fi, int st, int fo){ for(java.util.UUID u:m.getTeamA()){ Player p=Bukkit.getPlayer(u); if(p!=null) p.sendTitle(title, sub, fi, st, fo);} for(java.util.UUID u:m.getTeamB()){ Player p=Bukkit.getPlayer(u); if(p!=null) p.sendTitle(title, sub, fi, st, fo);} }
    private void toggleFly(java.util.Set<java.util.UUID> team, boolean allow, Match m){ for(java.util.UUID u:team){ Player p=Bukkit.getPlayer(u); if(p==null) continue; if(!allow){ if(p.isFlying()) p.setFlying(false); p.setAllowFlight(false);} else { Boolean had=m.getHadFlight().get(u); if(had!=null) p.setAllowFlight(had); } } }
    public void playerDiedOrQuit(java.util.UUID id){
        Match m=getMatch(id); if(m==null) return;
        boolean wasA=m.getTeamA().remove(id); if(!wasA) m.getTeamB().remove(id);
        playerToMatch.remove(id);
        new BukkitRunnable(){ @Override public void run(){ restorePlayer(id, m); } }.runTaskLater(plugin, 20L);
        if(m.getTeamA().isEmpty() && m.getTeamB().isEmpty()) { endMatch(m, true); return; }
        if(m.getTeamA().isEmpty()) { endMatch(m, false); return; }
        if(m.getTeamB().isEmpty()) { endMatch(m, true); return; }
    }
    public void shutdownAllMatches(){ for(Match m: new java.util.ArrayList<Match>(active.values())) endMatch(m, true); }
    public void startWeeklyResetTask(){ if(!Main.get().getConfig().getBoolean("weeklyReset.enabled", true)) return; new BukkitRunnable(){ @Override public void run(){ java.util.Calendar c=java.util.Calendar.getInstance(); int dow=Main.get().getConfig().getInt("weeklyReset.resetDayOfWeek",1); int h=Main.get().getConfig().getInt("weeklyReset.resetHour",0); int m=Main.get().getConfig().getInt("weeklyReset.resetMinute",0); if(c.get(java.util.Calendar.DAY_OF_WEEK)==dow && c.get(java.util.Calendar.HOUR_OF_DAY)==h && c.get(java.util.Calendar.MINUTE)==m) performWeeklyReset(); } }.runTaskTimer(plugin, 20L*60, 20L*60); }
    private void performWeeklyReset(){ org.bukkit.Bukkit.broadcastMessage("§6[경쟁전] §f주간 시즌 종료! 등급 보상을 지급합니다."); for(java.util.UUID u: new java.util.HashSet<java.util.UUID>(rating.keySet())){ rating.put(u, Main.get().getConfig().getInt("rating.start",1000)); wins.put(u,0); losses.put(u,0);
    try { save(); } catch (Throwable ignored) {}
} }
    public String getTierName(int rate){ org.bukkit.configuration.file.FileConfiguration c=Main.get().getConfig(); java.util.List<?> tiers=c.getList("tiers"); String last="UNRANKED"; if(tiers!=null){ for(Object o: tiers){ if(o instanceof java.util.Map){ java.util.Map<?,?> mm=(java.util.Map<?,?>)o; Object name=mm.get("name"); Object min=mm.get("min"); if(name!=null && min instanceof Number){ int mn=((Number)min).intValue(); if(rate>=mn) last=String.valueOf(name); } } } } return last; }
    public java.util.List<java.util.UUID> topRankings(int limit){ java.util.List<java.util.Map.Entry<java.util.UUID,Integer>> list=new java.util.ArrayList<java.util.Map.Entry<java.util.UUID,Integer>>(rating.entrySet()); java.util.Collections.sort(list, new java.util.Comparator<java.util.Map.Entry<java.util.UUID,Integer>>(){ public int compare(java.util.Map.Entry<java.util.UUID,Integer>a, java.util.Map.Entry<java.util.UUID,Integer>b){ return Integer.compare(b.getValue(), a.getValue()); }}); java.util.List<java.util.UUID> out=new java.util.ArrayList<java.util.UUID>(); int i=0; for(java.util.Map.Entry<java.util.UUID,Integer> e: list){ out.add(e.getKey()); if(++i>=limit) break; } return out; }
    private void forceUnmapAll(Match m){ java.util.Set<java.util.UUID> all=new java.util.HashSet<java.util.UUID>(); all.addAll(m.getTeamA()); all.addAll(m.getTeamB()); for(java.util.UUID u: all) playerToMatch.remove(u); }
    private void clearTeams(Match m){ java.util.Set<java.util.UUID> all=new java.util.HashSet<java.util.UUID>(); all.addAll(m.getTeamA()); all.addAll(m.getTeamB()); for(java.util.UUID u: all) teamService.clearWholeTeam(u); }
}
