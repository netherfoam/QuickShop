package org.maxgamer.QuickShop.Listeners;

import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.maxgamer.QuickShop.QuickShop;
import org.maxgamer.QuickShop.Shop.DisplayItem;
import org.maxgamer.QuickShop.Shop.Shop;


public class ChunkListener implements Listener{
	QuickShop plugin;
	public ChunkListener(QuickShop plugin){
		this.plugin = plugin;
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onChunkLoad(ChunkLoadEvent e){
		List<Shop> inChunk = plugin.getShopsInChunk(e.getChunk());
		if(inChunk == null) return;
		
		for(Shop shop : inChunk){
			DisplayItem disItem = shop.getDisplayItem();
			disItem.removeDupe();
			disItem.remove();
			disItem.spawn();
		}
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void onChunkUnload(ChunkUnloadEvent e){
		List<Shop> inChunk = plugin.getShopsInChunk(e.getChunk());
		if(inChunk == null) return;
		
		for(Shop shop : inChunk){
			DisplayItem disItem = shop.getDisplayItem();
			disItem.removeDupe();
			disItem.remove();
		}
	}
}