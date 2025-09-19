package com.minkang.ultimate.pvpmm;

import com.minkang.ultimate.pvpmm.commands.CompetitionCommand;
import com.minkang.ultimate.pvpmm.manager.ArenaManager;
import com.minkang.ultimate.pvpmm.manager.MatchManager;
import com.minkang.ultimate.pvpmm.matchmaking.Matchmaker;
import com.minkang.ultimate.pvpmm.rewards.RewardManager;
import com.minkang.ultimate.pvpmm.listeners.DamageListener;
import com.minkang.ultimate.pvpmm.listeners.DeathQuitListener;
import com.minkang.ultimate.pvpmm.listeners.CommandBlockListener;
import com.minkang.ultimate.pvpmm.service.InviteService;
import com.minkang.ultimate.pvpmm.service.TeamService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private static Main instance;

    private ArenaManager arenaManager;
    private MatchManager matchManager;
    private Matchmaker matchmaker;
    private InviteService inviteService;
    private TeamService teamService;
    private RewardManager rewardManager;

    public static Main get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.arenaManager = new ArenaManager(this);
        this.rewardManager = new RewardManager(this);
        this.teamService = new TeamService(this);
        this.inviteService = new InviteService(this, teamService);
        this.matchManager = new MatchManager(this, teamService);
        this.matchmaker = new Matchmaker(this, arenaManager, matchManager, teamService);

        getCommand("경쟁전").setExecutor(new CompetitionCommand(this, arenaManager, matchmaker, teamService, inviteService, rewardManager));

        Bukkit.getPluginManager().registerEvents(new DamageListener(matchManager), this);
        Bukkit.getPluginManager().registerEvents(new DeathQuitListener(matchManager), this);
        Bukkit.getPluginManager().registerEvents(new CommandBlockListener(matchManager, this), this);

        matchmaker.start();
        matchManager.startWeeklyResetTask();
        getLogger().info("PvPCompetition enabled.");
    }

    @Override
    public void onDisable() {
        if (matchmaker != null) {
            matchmaker.stop();
        }
        if (matchManager != null) {
            matchManager.shutdownAllMatches();
        }
        arenaManager.save();
        rewardManager.save();
        getLogger().info("PvPCompetition disabled.");
    }
}
