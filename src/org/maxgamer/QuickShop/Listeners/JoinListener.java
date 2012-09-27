package org.maxgamer.QuickShop.Listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.maxgamer.QuickShop.Messenger;
import org.maxgamer.QuickShop.QuickShop;

public class JoinListener implements Listener{
	private QuickShop plugin;
	
	public JoinListener(QuickShop plugin){
		this.plugin = plugin;
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e){
		Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, new Messenger(plugin, e.getPlayer().getName()), 60);
	}
}