package com.minkang.ultimate.pvpmm.service;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
public class InviteService implements Listener {
    private final Plugin plugin;
    private final TeamService teamService;
    private final Map<java.util.UUID, Long> credits = new HashMap<java.util.UUID, Long>();
    private final Map<java.util.UUID, java.util.UUID> pending = new HashMap<java.util.UUID, java.util.UUID>();
    public InviteService(Plugin plugin, TeamService teamService){ this.plugin=plugin; this.teamService=teamService; Bukkit.getPluginManager().registerEvents(this, plugin); }
    public ItemStack createInviteTicket(int amount){
        ItemStack it=new ItemStack(Material.PAPER, amount);
        ItemMeta m=it.getItemMeta();
        m.setDisplayName("§d경쟁전 팀 초대권");
        java.util.List<String> lore=new java.util.ArrayList<String>(); lore.add("§7우클릭 시 20초 동안"); lore.add("§f/경쟁전 팀 초대 <닉네임> §7사용 가능");
        m.setLore(lore); it.setItemMeta(m); return it;
    }
    @EventHandler public void onUse(PlayerInteractEvent e){
        if(e.getItem()==null) return; ItemStack it=e.getItem();
        if(!it.hasItemMeta() || it.getItemMeta().getDisplayName()==null) return;
        if(!"§d경쟁전 팀 초대권".equals(it.getItemMeta().getDisplayName())) return;
        Player p=e.getPlayer();
        if(it.getAmount()>1) it.setAmount(it.getAmount()-1); else p.getInventory().removeItem(it);
        credits.put(p.getUniqueId(), System.currentTimeMillis()+20000L);
        p.sendMessage("§d[경쟁전] §f초대권 사용! 20초 이내에 §a/경쟁전 팀 초대 <닉네임> §f입력");
        e.setCancelled(true);
        new BukkitRunnable(){ @Override public void run(){ Long until=credits.get(p.getUniqueId()); if(until!=null && System.currentTimeMillis()>until) { credits.remove(p.getUniqueId()); p.sendMessage("§7[경쟁전] 초대권 사용 시간이 만료되었습니다."); } } }.runTaskLater(plugin, 20L*21);
    }
    public boolean hasCredit(Player p){ Long until=credits.get(p.getUniqueId()); if(until==null) return false; if(System.currentTimeMillis()>until){ credits.remove(p.getUniqueId()); return false; } return true; }
    public boolean sendInvite(Player from, Player to){
        if(!hasCredit(from)){ from.sendMessage("§c[경쟁전] 초대권이 필요합니다."); return false; }
        if(from.getUniqueId().equals(to.getUniqueId())){ from.sendMessage("§c자기 자신에게는 초대할 수 없습니다."); return false; }
        pending.put(to.getUniqueId(), from.getUniqueId());
        from.sendMessage("§a[경쟁전] " + to.getName() + " 님에게 팀 초대를 보냈습니다. (20초)");
        to.sendMessage("§d[경쟁전] " + from.getName() + " §f님이 팀 초대를 보냈습니다. §a/경쟁전 팀 수락 §f또는 §c/경쟁전 팀 거절");
        new BukkitRunnable(){ @Override public void run(){ java.util.UUID inv=pending.remove(to.getUniqueId()); if(inv!=null && inv.equals(from.getUniqueId())){ from.sendMessage("§7[경쟁전] " + to.getName() + " 님이 초대를 응답하지 않았습니다."); to.sendMessage("§7[경쟁전] 초대가 만료되었습니다."); } } }.runTaskLater(plugin, 20L*20);
        return true;
    }
    public boolean accept(Player to){
        java.util.UUID inviter=pending.remove(to.getUniqueId());
        if(inviter==null){ to.sendMessage("§c[경쟁전] 대기 중인 초대가 없습니다."); return false; }
        Player from=Bukkit.getPlayer(inviter);
        if(from==null){ to.sendMessage("§c[경쟁전] 초대한 플레이어가 오프라인입니다."); return false; }
        boolean ok=teamService.createOrJoin(from, to);
        if(!ok){ to.sendMessage("§c[경쟁전] 팀 합류 실패(정원 초과 가능)."); return false; }
        to.sendMessage("§a[경쟁전] 팀에 합류했습니다.");
        from.sendMessage("§a[경쟁전] " + to.getName() + " 님이 팀에 합류했습니다.");
        Bukkit.broadcastMessage("§d[경쟁전] §f" + from.getName() + " §7와 §f" + to.getName() + " §7팀 결성!");
        return true;
    }
    public boolean deny(Player to){
        java.util.UUID inviter=pending.remove(to.getUniqueId());
        if(inviter==null){ to.sendMessage("§c[경쟁전] 대기 중인 초대가 없습니다."); return false; }
        Player from=Bukkit.getPlayer(inviter);
        if(from!=null) from.sendMessage("§c[경쟁전] " + to.getName() + " 님이 팀 초대를 거절했습니다.");
        to.sendMessage("§7[경쟁전] 초대를 거절했습니다.");
        return true;
    }
}
