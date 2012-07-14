package org.maxgamer.QuickShop.Listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.maxgamer.QuickShop.QuickShop;

public class PickupListener implements Listener{
	QuickShop plugin;
	public PickupListener(QuickShop plugin){
		this.plugin = plugin;
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onBreak(PlayerPickupItemEvent e){
		if(e.isCancelled()) return;
		if(plugin.isProtectedItem(e.getItem())){
			e.setCancelled(true);
			return;
		}
	}
}