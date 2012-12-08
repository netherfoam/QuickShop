package org.maxgamer.QuickShop;

import java.util.List;

import org.bukkit.entity.Player;

public class Messenger implements Runnable{
	private Player player;
	
	public Messenger(Player player){
		this.player = player;
	}

	@Override
	public void run() {
		List<String> messages = MsgUtil.getMessages(player.getName());
		
		for(String s : messages){
			player.sendMessage(s);
		}
		MsgUtil.deleteMessages(player.getName());
	}
}