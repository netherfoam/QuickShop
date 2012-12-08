package org.maxgamer.QuickShop.Listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.maxgamer.QuickShop.MsgUtil;

public class JoinListener implements Listener{
	public JoinListener(){
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e){
		//Notify the player any messages they were sent
		MsgUtil.flush(e.getPlayer(), 60);
	}
}