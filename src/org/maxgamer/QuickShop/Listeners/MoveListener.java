package org.maxgamer.QuickShop.Listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.maxgamer.QuickShop.Info;
import org.maxgamer.QuickShop.QuickShop;
import org.maxgamer.QuickShop.ShopAction;

public class MoveListener implements Listener{
	QuickShop plugin;
	public MoveListener(QuickShop plugin){
		this.plugin = plugin;
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onMove(PlayerMoveEvent e){
		if(e.isCancelled()) return;
		Info info = plugin.getActions().get(e.getPlayer().getName());
		if(info != null){
			Player p = e.getPlayer();
			Location loc1 = info.getLocation();
			Location loc2 = p.getLocation();
			
			
			if(loc1.distanceSquared(loc2) > 25){
				if(info.getAction() == ShopAction.CREATE){
					p.sendMessage("Shop creation cancelled");
				}
				else if(info.getAction() == ShopAction.BUY){
					p.sendMessage("Shop purchase cancelled");
				}
				plugin.getActions().remove(p.getName());
				return;
			}
		}
	}
}