package org.maxgamer.QuickShop.Listeners;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.maxgamer.QuickShop.Info;
import org.maxgamer.QuickShop.QuickShop;
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
			e.getPlayer().sendMessage("Shop Removed");
			for(Info info : plugin.getActions().values()){
				info.setAction(ShopAction.CANCELLED);
			}
			plugin.getShops().remove(e.getBlock().getLocation());
		}
	}
}