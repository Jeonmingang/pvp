package com.minkang.ultimate.pvpmm;

import com.minkang.ultimate.pvpmm.commands.CompetitionCommand;
import com.minkang.ultimate.pvpmm.manager.ArenaManager;
import com.minkang.ultimate.pvpmm.manager.MatchManager;
import com.minkang.ultimate.pvpmm.matchmaking.Matchmaker;
import com.minkang.ultimate.pvpmm.rewards.RewardManager;
import com.minkang.ultimate.pvpmm.listeners.DamageListener;
import com.minkang.ultimate.pvpmm.listeners.DeathQuitListener;
import com.minkang.ultimate.pvpmm.listeners.CommandBlockListener;
import com.minkang.ultimate.pvpmm.listeners.JoinKickListener;
import com.minkang.ultimate.pvpmm.service.InviteService;
import com.minkang.ultimate.pvpmm.service.TeamService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private static Main instance;
    public static Main get(){ return instance; }

    private ArenaManager arenaManager;
    private MatchManager matchManager;
    private Matchmaker matchmaker;
    private InviteService inviteService;
    private TeamService teamService;
    private RewardManager rewardManager;

    @Override
    public void onEnable() {
        instance = this;
        getDataFolder().mkdirs();
        saveDefaultConfig();
        this.arenaManager = new ArenaManager(this);
        this.rewardManager = new RewardManager(this);
        this.teamService = new TeamService(this);
        this.inviteService = new InviteService(this, teamService);
        this.matchManager = new MatchManager(this, teamService);
        this.matchmaker = new Matchmaker(this, arenaManager, matchManager, teamService);

        getCommand("경쟁전").setExecutor(new CompetitionCommand(this, arenaManager, matchmaker, matchManager, teamService, inviteService, rewardManager));

        Bukkit.getPluginManager().registerEvents(new DamageListener(matchManager), this);
        Bukkit.getPluginManager().registerEvents(new JoinKickListener(matchManager), this);
        Bukkit.getPluginManager().registerEvents(new DeathQuitListener(matchManager), this);
        Bukkit.getPluginManager().registerEvents(new CommandBlockListener(matchManager, this), this);
        Bukkit.getPluginManager().registerEvents(new com.minkang.ultimate.pvpmm.listeners.MoveListener(matchManager), this);

        matchmaker.start();
        // periodic autosave for arenas (once per minute)
        org.bukkit.Bukkit.getScheduler().runTaskTimer(this, () -> arenaManager.save(), 20L*60, 20L*60);
        org.bukkit.Bukkit.getScheduler().runTaskTimer(this, () -> matchManager.save(), 20L*60, 20L*60);
        // Ensure arenas load after all worlds are ready
        org.bukkit.Bukkit.getScheduler().runTaskLater(this, () -> arenaManager.load(), 40L);
        matchManager.startWeeklyResetTask();
        getLogger().info("PvPCompetition enabled v1.0.13");
    }

    @Override
    public void onDisable() {
        if (matchmaker!=null) matchmaker.stop();
        if (matchManager!=null) matchManager.shutdownAllMatches();
        arenaManager.save();
        if (matchManager!=null) matchManager.save();
        rewardManager.save();
    }
}
