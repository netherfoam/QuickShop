package org.maxgamer.QuickShop.Listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.maxgamer.QuickShop.QuickShop;

public class QuitListener implements Listener{
	QuickShop plugin;
	public QuitListener(QuickShop plugin){
		this.plugin = plugin;
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e){
		//Remove them from the menu
		plugin.getActions().remove(e.getPlayer().getName());
	}
}