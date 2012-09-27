package org.maxgamer.QuickShop;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Messenger implements Runnable{
	private QuickShop plugin;
	private String player;
	
	public Messenger(QuickShop plugin, String player){
		this.plugin = plugin;
		this.player = player;
	}

	@Override
	public void run() {
		List<String> messages = plugin.getMessages(player);
		
		if(messages == null){
			return;
		}
		Player p = Bukkit.getPlayerExact(player);
		if(p == null){
			//Gets thrown when a player joins for the first time, believe it or not.
			return;
		}
		
		for(String s : messages){
			p.sendMessage(s);
		}
		plugin.deleteMessages(player);
	}
}