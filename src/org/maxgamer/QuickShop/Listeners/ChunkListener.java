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
		/*
		for(Entry<Location, Shop> map:plugin.getShops().entrySet()){
			Location loc = map.getKey();
			Shop shop = map.getValue();
			if(loc.getWorld() == null) continue;
			if(chunkMatches(e.getChunk(), loc.getChunk())){
				//This is a shop chunk.
				
				shop.getDisplayItem().removeDupe();
				shop.getDisplayItem().remove();
				shop.getDisplayItem().spawn();
			}
		}*/
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
		/*
		for(Entry<Location, Shop> map:plugin.getShops().entrySet()){
			Location loc = map.getKey();
			if(loc.getWorld() == null) continue;
			if(e.getChunk().equals(loc.getChunk())){
				//This is a shop chunk.
				Shop shop = map.getValue();
				shop.getDisplayItem().removeDupe();
				shop.getDisplayItem().remove();
			}
		}*/
	}
}