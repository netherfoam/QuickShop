package org.maxgamer.QuickShop.Listeners;

import java.util.Map.Entry;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.maxgamer.QuickShop.QuickShop;
import org.maxgamer.QuickShop.Shop.Shop;


public class ChunkListener implements Listener{
	QuickShop plugin;
	public ChunkListener(QuickShop plugin){
		this.plugin = plugin;
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onChunkLoad(ChunkLoadEvent e){
		for(Entry<Location, Shop> map:plugin.getShops().entrySet()){
			Location loc = map.getKey();
			Shop shop = map.getValue();
			if(loc.getWorld() == null) continue;
			if(e.getChunk().equals(loc.getChunk())){
				//This is a shop chunk.
				
				shop.getDisplayItem().removeDupe();
				shop.getDisplayItem().remove();
				shop.getDisplayItem().spawn();
			}
		}
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void onChunkUnload(ChunkUnloadEvent e){
		for(Entry<Location, Shop> map:plugin.getShops().entrySet()){
			Location loc = map.getKey();
			if(loc.getWorld() == null) continue;
			if(e.getChunk().equals(loc.getChunk())){
				//This is a shop chunk.
				Shop shop = map.getValue();
				shop.getDisplayItem().removeDupe();
				shop.getDisplayItem().remove();
			}
		}
	}
}