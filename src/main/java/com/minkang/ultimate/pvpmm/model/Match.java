package com.minkang.ultimate.pvpmm.model;
import org.bukkit.Location; import java.util.*;
public class Match {
    public enum State { PREP, RUNNING, ENDED }
    private final String id; private final Arena arena;
    private final Set<java.util.UUID> teamA = new HashSet<java.util.UUID>();
    private final Set<java.util.UUID> teamB = new HashSet<java.util.UUID>();
    private final Map<java.util.UUID, Location> returnLocations = new HashMap<java.util.UUID, Location>();
    private final Map<java.util.UUID, Boolean> hadFlight = new HashMap<java.util.UUID, Boolean>();
    private State state = State.PREP; private long startMillis;
    private String scoreboardTeamA; private String scoreboardTeamB;
    public Match(String id, Arena arena, Set<java.util.UUID> a, Set<java.util.UUID> b){ this.id=id; this.arena=arena; teamA.addAll(a); teamB.addAll(b); }
    public String getId(){ return id; } public Arena getArena(){ return arena; }
    public Set<java.util.UUID> getTeamA(){ return teamA; } public Set<java.util.UUID> getTeamB(){ return teamB; }
    public Map<java.util.UUID, Location> getReturnLocations(){ return returnLocations; } public Map<java.util.UUID, Boolean> getHadFlight(){ return hadFlight; }
    public State getState(){ return state; } public void setState(State s){ state=s; }
    public long getStartMillis(){ return startMillis; } public void setStartMillis(long m){ startMillis=m; }
    public String getScoreboardTeamA(){ return scoreboardTeamA; } public void setScoreboardTeamA(String s){ scoreboardTeamA=s; }
    public String getScoreboardTeamB(){ return scoreboardTeamB; } public void setScoreboardTeamB(String s){ scoreboardTeamB=s; }
    public boolean isInTeamA(java.util.UUID id){ return teamA.contains(id); }
    public boolean isInTeamB(java.util.UUID id){ return teamB.contains(id); }
}
