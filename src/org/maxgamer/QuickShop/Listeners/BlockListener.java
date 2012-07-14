package org.maxgamer.QuickShop.Listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.maxgamer.QuickShop.Info;
import org.maxgamer.QuickShop.QuickShop;
import org.maxgamer.QuickShop.Shop;
import org.maxgamer.QuickShop.ShopAction;

public class BlockListener implements Listener{
	QuickShop plugin;
	public BlockListener(QuickShop plugin){
		this.plugin = plugin;
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onBreak(BlockBreakEvent e){
		if(e.isCancelled() || e.getBlock().getType() != Material.CHEST) return;
		if(plugin.getShops().containsKey(e.getBlock().getLocation())){
			for(Info info : plugin.getActions().values()){
				info.setAction(ShopAction.CANCELLED);
			}
			Shop shop = plugin.getShop(e.getBlock().getLocation());
			shop.deleteDisplayItem();
			plugin.getShops().remove(e.getBlock().getLocation());
			e.getPlayer().sendMessage(ChatColor.GREEN + "Shop Removed");
		}
	}
}