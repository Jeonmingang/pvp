package com.minkang.ultimate.pvpmm.commands;

import com.minkang.ultimate.pvpmm.Main;
import com.minkang.ultimate.pvpmm.manager.ArenaManager;
import com.minkang.ultimate.pvpmm.matchmaking.Matchmaker;
import com.minkang.ultimate.pvpmm.manager.MatchManager;
import com.minkang.ultimate.pvpmm.rewards.RewardManager;
import com.minkang.ultimate.pvpmm.service.InviteService;
import com.minkang.ultimate.pvpmm.service.TeamService;
import com.minkang.ultimate.pvpmm.model.Arena;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.Locale;

public class CompetitionCommand implements CommandExecutor {

    private final Main plugin;
    private final ArenaManager arenaManager;
    private final Matchmaker matchmaker;
    private final MatchManager matchManager;
    private final TeamService teamService;
    private final InviteService inviteService;
    private final RewardManager rewardManager;

    public CompetitionCommand(Main plugin, ArenaManager arenaManager, Matchmaker matchmaker, MatchManager matchManager, TeamService teamService, InviteService inviteService, RewardManager rewardManager) {
        this.plugin = plugin; this.arenaManager = arenaManager; this.matchmaker = matchmaker; this.matchManager = matchManager; this.teamService = teamService; this.inviteService = inviteService; this.rewardManager = rewardManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }
        if ("설정".equals(args[0])) {
            if (!sender.hasPermission("pvpmm.admin")) { sender.sendMessage("§c권한이 없습니다."); return true; }
            return handleAdmin(sender, args);
        }
        if ("랭킹".equals(args[0])) {
            java.util.List<java.util.UUID> top = matchManager.topRankings(10);
            if (top.isEmpty()) { sender.sendMessage("§7[경쟁전] 랭킹 데이터가 없습니다."); return true; }

            java.util.Calendar now = java.util.Calendar.getInstance();
            int dow = Main.get().getConfig().getInt("weeklyReset.resetDayOfWeek", 1);
            int hour = Main.get().getConfig().getInt("weeklyReset.resetHour", 0);
            int minute = Main.get().getConfig().getInt("weeklyReset.resetMinute", 0);
            java.util.Calendar next = (java.util.Calendar)now.clone();
            next.set(java.util.Calendar.HOUR_OF_DAY, hour);
            next.set(java.util.Calendar.MINUTE, minute);
            next.set(java.util.Calendar.SECOND, 0);
            next.set(java.util.Calendar.MILLISECOND, 0);
            while (next.get(java.util.Calendar.DAY_OF_WEEK) != dow || !next.after(now)) {
                next.add(java.util.Calendar.DAY_OF_MONTH, 1);
                next.set(java.util.Calendar.HOUR_OF_DAY, hour);
                next.set(java.util.Calendar.MINUTE, minute);
                next.set(java.util.Calendar.SECOND, 0);
                next.set(java.util.Calendar.MILLISECOND, 0);
            }
            long ms = next.getTimeInMillis() - now.getTimeInMillis();
            long sec = ms / 1000L;
            long d = sec / 86400L; sec %= 86400L;
            long h = sec / 3600L; sec %= 3600L;
            long m = sec / 60L; sec %= 60L;
            String remain = (d>0? d+"일 ":"") + String.format(java.util.Locale.US, "%02d:%02d:%02d", h, m, sec);

            sender.sendMessage("§d§m========================================");
            sender.sendMessage("    §d§l경쟁전 랭킹 TOP 10    §7(초기화까지 §f" + remain + "§7)");
            sender.sendMessage("§d§m========================================");

            int i=1;
            for (java.util.UUID u : top) {
                org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(u);
                String name = (op!=null && op.getName()!=null) ? op.getName() : "Unknown";
                int r = matchManager.getRating(u);
                String t = matchManager.getTierName(r);
                String badge = (i==1? "§6★" : i==2? "§7★" : i==3? "§c★" : "§f" + i + ".");
                sender.sendMessage(String.format(" %s §f%-16s §7| §e%4d §7(%s)", badge, name, r, t));
                i++;
            }
            sender.sendMessage("§d§m========================================");
            return true;
        }
        if ("정보".equals(args[0])) {
            if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 가능합니다."); return true; }
            Player p = (Player)sender;
            int r = matchManager.getRating(p.getUniqueId());
            int w = matchManager.getWins(p.getUniqueId());
            int l = matchManager.getLosses(p.getUniqueId());
            String t = matchManager.getTierName(r);
            int total = w+l; double wr = total>0 ? (w*100.0/total) : 0.0;
            p.sendMessage("§d==== §f경쟁전 정보 §d====");
            p.sendMessage(" §7티어: §e" + t + " §7| 레이팅: §f" + r);
            p.sendMessage(" §7전적: §a" + w + "승 §c" + l + "패 §7(승률 " + String.format(java.util.Locale.US, "%.1f", wr) + "%)");
            return true;
        }
        if ("목록".equals(args[0])) {
            List<Arena> list = arenaManager.all();
            if (list.isEmpty()) sender.sendMessage("§7[경쟁전] 등록된 경기장이 없습니다.");
            else { sender.sendMessage("§a[경쟁전] 경기장 목록 (번호 / 이름):"); java.util.Collections.sort(list, new java.util.Comparator<Arena>(){public int compare(Arena x, Arena y){return x.getName().compareToIgnoreCase(y.getName());}}); int idx=1; for (Arena a : list) sender.sendMessage(String.format(" §e#%d §f%s %s", idx++, a.getName(), (a.isReady() ? "§7(준비완료)" : "§c(미완성)"))); }
            return true;
        }

        if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 사용 가능합니다."); return true; }
        Player p = (Player)sender;

        if ("팀".equals(args[0])) return handleTeam(p, args);
        if ("매치".equals(args[0])) {
            if (args.length >= 2 && "취소".equals(args[1])) {
                boolean ok = matchmaker.cancel(p);
                if (!ok) p.sendMessage("§c[경쟁전] 대기 중이 아닙니다.");
                return true;
            }
            boolean ok = matchmaker.enqueue(p);
            if (ok) p.sendMessage("§d[경쟁전] 매칭 대기열에 등록되었습니다.");
            return true;
        }
        sendHelp(sender);
        return true;
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§e/경쟁전 설정 경기장 생성 <이름>");
            sender.sendMessage("§e/경쟁전 설정 경기장 위치 <이름> <1~4>");
            sender.sendMessage("§e/경쟁전 설정 경기장 삭제 <번호>");
            sender.sendMessage("§e/경쟁전 설정 초대권");
            sender.sendMessage("§e/경쟁전 설정 보상 <티어>");
            return true;
        }
        if ("경기장".equals(args[1])) {
            if (args.length >= 3 && "생성".equals(args[2])) {
                if (args.length < 4) { sender.sendMessage("§e/경쟁전 설정 경기장 생성 <이름>"); return true; }
                Arena a = arenaManager.create(args[3]);
                sender.sendMessage("§a[경쟁전] 경기장 생성: " + a.getName());
                return true;
            }
            if (args.length >= 3 && "삭제".equals(args[2])) {
                if (args.length < 4) { sender.sendMessage("§e/경쟁전 설정 경기장 삭제 <번호>"); return true; }
                int idx; try { idx = Integer.parseInt(args[3]); } catch (Exception e) { sender.sendMessage("§c숫자를 입력하세요."); return true; }
                boolean ok = arenaManager.deleteByIndex(idx);
                if (ok) sender.sendMessage("§a[경쟁전] 경기장 #" + idx + " 삭제 완료."); else sender.sendMessage("§c[경쟁전] 해당 번호의 경기장이 없습니다.");
                return true;
            }
            if (args.length >= 3 && "위치".equals(args[2])) {
                if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 가능합니다."); return true; }
                if (args.length < 5) { sender.sendMessage("§e/경쟁전 설정 경기장 위치 <이름> <1~4>"); return true; }
                Player p = (Player)sender;
                String name = args[3];
                int idx; try { idx = Integer.parseInt(args[4]); } catch (Exception e) { sender.sendMessage("§c숫자를 입력하세요 (1~4)"); return true; }
                if (idx < 1 || idx > 4) { sender.sendMessage("§c1~4 범위만 허용됩니다."); return true; }
                boolean ok = arenaManager.setPos(name, idx, p.getLocation().clone());
                if (!ok) sender.sendMessage("§c[경쟁전] 해당 이름의 경기장이 없습니다.");
                else sender.sendMessage("§a[경쟁전] " + name + " 위치 " + idx + " 저장 완료.");
                return true;
            }
        }
        if ("초대권".equals(args[1])) {
            if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 가능합니다."); return true; }
            Player p = (Player)sender;
            p.getInventory().addItem(inviteService.createInviteTicket(1));
            p.sendMessage("§d[경쟁전] 초대권 1장을 지급했습니다.");
            return true;
        }
        if ("보상".equals(args[1])) {
            if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 가능합니다."); return true; }
            if (args.length < 3) { sender.sendMessage("§e/경쟁전 설정 보상 <티어>"); return true; }
            String tier = args[2].toUpperCase(Locale.ROOT);
            rewardManager.openTierEditor((Player)sender, tier);
            return true;
        }
        sender.sendMessage("§c알 수 없는 하위 명령입니다.");
        return true;
    }

    private boolean handleTeam(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage("§e/경쟁전 팀 초대 <닉네임>");
            p.sendMessage("§e/경쟁전 팀 수락");
            p.sendMessage("§e/경쟁전 팀 거절");
            p.sendMessage("§e/경쟁전 팀 탈퇴");
            return true;
        }
        if ("초대".equals(args[1])) {
            if (args.length < 3) { p.sendMessage("§e/경쟁전 팀 초대 <닉네임>"); return true; }
            Player t = Bukkit.getPlayer(args[2]);
            if (t==null) { p.sendMessage("§c해당 플레이어를 찾을 수 없습니다."); return true; }
            inviteService.sendInvite(p, t);
            return true;
        }
        if ("수락".equals(args[1])) { inviteService.accept(p); return true; }
        if ("거절".equals(args[1])) { inviteService.deny(p); return true; }
        if ("탈퇴".equals(args[1])) {
            boolean ok = teamService.leaveTeam(p);
            if (ok) p.sendMessage("§7[경쟁전] 팀을 탈퇴했습니다."); else p.sendMessage("§7[경쟁전] 소속된 팀이 없습니다.");
            return true;
        }
        p.sendMessage("§c알 수 없는 하위 명령입니다.");
        return true;
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage("§d==== §f경쟁전 도움말 §d====");
        s.sendMessage("§e/경쟁전 매치 §7- 매칭 대기열 등록");
        s.sendMessage("§e/경쟁전 매치 취소 §7- 매칭 대기 취소");
        s.sendMessage("§e/경쟁전 팀 초대 <닉네임> §7- 초대권 필요");
        s.sendMessage("§e/경쟁전 팀 수락 | /경쟁전 팀 거절 | /경쟁전 팀 탈퇴");
        s.sendMessage("§e/경쟁전 목록 §7- 경기장 목록(번호 포함)");
        s.sendMessage("§e/경쟁전 랭킹 §7- TOP10 + 초기화 남은시간");
        s.sendMessage("§e/경쟁전 정보 §7- 내 전적/티어");
        if (s.hasPermission("pvpmm.admin")) {
            s.sendMessage("§e/경쟁전 설정 경기장 생성 <이름>");
            s.sendMessage("§e/경쟁전 설정 경기장 위치 <이름> <1~4>");
            s.sendMessage("§e/경쟁전 설정 경기장 삭제 <번호>");
            s.sendMessage("§e/경쟁전 설정 초대권");
            s.sendMessage("§e/경쟁전 설정 보상 <티어>");
        }
    }
}
