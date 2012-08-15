package org.maxgamer.QuickShop.Listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.maxgamer.QuickShop.QuickShop;
import org.maxgamer.QuickShop.Shop.DisplayItem;
import org.maxgamer.QuickShop.Shop.Shop;


public class ChunkListener implements Listener{
	private QuickShop plugin;
	public HashMap<Chunk, List<Shop>> chunkMap = new HashMap<Chunk, List<Shop>>(10);
	
	public ChunkListener(QuickShop plugin){
		this.plugin = plugin;
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onChunkLoad(ChunkLoadEvent e){
		//Testing
		Chunk c = e.getChunk();
		if(plugin.getShops() == null) return;
		
		List<Shop> shops = new ArrayList<Shop>(5);
		
		for(Shop shop : plugin.getShops().values()){
			if(shop.getLocation().getChunk().isLoaded() && shop.getLocation().getChunk().equals(c)){
				shops.add(shop);
				
				DisplayItem disItem = shop.getDisplayItem();
				disItem.removeDupe();
				disItem.remove();
				disItem.spawn();
				plugin.debug("Chunk loading spawning item: " + disItem.getItem().getItemStack().getType());
			}
		}
		this.chunkMap.put(c, shops);
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void onChunkUnload(ChunkUnloadEvent e){
		Chunk c = e.getChunk();
		
		List<Shop> shops = this.chunkMap.get(c);
		
		for(Shop shop : shops){
			DisplayItem disItem = shop.getDisplayItem();
			disItem.removeDupe();
			disItem.remove();
			plugin.debug("Chunk loading spawning item: " + disItem.getItem().getItemStack().getType());
		}
		/*
		if(plugin.getShops() == null) return;
		for(Shop shop : plugin.getShops().values()){
			if(shop.getLocation().getChunk().equals(c)){
				DisplayItem disItem = shop.getDisplayItem();
				disItem.removeDupe();
				disItem.remove();
				disItem.spawn();
				
			}
		}*/
	}
}