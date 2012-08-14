package org.maxgamer.QuickShop.Listeners;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
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
		ConcurrentHashMap<Location, Shop> inChunk = plugin.getShopsInChunk(e.getChunk());
		//If theres no shops in the chunk, return
		if(inChunk == null) return;
		
		//For each shop in the chunk, respawn the item (Is this necessary?)
		for(Entry<Location, Shop> entry : inChunk.entrySet()){
			DisplayItem disItem = entry.getValue().getDisplayItem();
			disItem.removeDupe();
			disItem.remove();
			disItem.spawn();
		}
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void onChunkUnload(ChunkUnloadEvent e){
		ConcurrentHashMap<Location, Shop> inChunk = plugin.getShopsInChunk(e.getChunk());
		//If theres no shop in the chunk, return
		if(inChunk == null) return;
		
		//For each shop in the chunk, delete the item.
		for(Entry<Location, Shop> entry : inChunk.entrySet()){
			DisplayItem disItem = entry.getValue().getDisplayItem();
			disItem.removeDupe();
			disItem.remove();
		}
	}
}