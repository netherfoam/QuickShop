package org.maxgamer.QuickShop.Listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.maxgamer.QuickShop.QuickShop;

/**
 * 
 * @author Netherfoam
 *
 */
public class ChatListener implements Listener{
	QuickShop plugin;
	
	public ChatListener(QuickShop plugin){
		this.plugin = plugin;
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onChat(final AsyncPlayerChatEvent e){
		if(!plugin.getShopManager().getActions().containsKey(e.getPlayer().getName())) return;
		plugin.getShopManager().handleChat(e.getPlayer(), e.getMessage());
		e.setCancelled(true);
	}
}