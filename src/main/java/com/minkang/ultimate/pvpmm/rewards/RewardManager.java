package com.minkang.ultimate.pvpmm.rewards;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RewardManager implements Listener {

    private final Plugin plugin;
    private final File file;
    private final FileConfiguration cfg;

    public RewardManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "rewards.yml");
        if (!file.exists()) plugin.saveResource("rewards.yml", false);
        this.cfg = YamlConfiguration.loadConfiguration(file);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openTierEditor(Player admin, String tier) {
        Inventory inv = Bukkit.createInventory(null, 27, "보상 설정: " + tier.toUpperCase(java.util.Locale.ROOT));
        List<?> list = cfg.getList("tiers." + tier.toUpperCase(java.util.Locale.ROOT));
        if (list != null) {
            for (int i=0; i<list.size() && i<inv.getSize(); i++) {
                Object o = list.get(i);
                if (o instanceof ItemStack) inv.setItem(i, (ItemStack)o);
            }
        }
        admin.openInventory(inv);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        String title = e.getView().getTitle();
        if (title==null || !title.startsWith("보상 설정: ")) return;
        String tier = title.substring("보상 설정: ".length()).toUpperCase(java.util.Locale.ROOT);
        ItemStack[] contents = e.getInventory().getContents();
        java.util.List<ItemStack> list = new java.util.ArrayList<ItemStack>();
        for (ItemStack it : contents) list.add(it);
        cfg.set("tiers." + tier, list);
        try { cfg.save(file); } catch (IOException ex) { ex.printStackTrace(); }
        e.getPlayer().sendMessage("§a[경쟁전] " + tier + " 보상이 저장되었습니다.");
    }

    public void save() { try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); } }
    public java.util.List<ItemStack> getRewards(String tier) {
        List<?> list = cfg.getList("tiers." + tier.toUpperCase(java.util.Locale.ROOT));
        java.util.List<ItemStack> items = new java.util.ArrayList<ItemStack>();
        if (list==null) return items;
        for (Object o : list) if (o instanceof ItemStack) items.add((ItemStack)o);
        return items;
    }
}
