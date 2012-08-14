package org.maxgamer.QuickShop.Listeners;

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
	QuickShop plugin;
	public ChunkListener(QuickShop plugin){
		this.plugin = plugin;
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onChunkLoad(ChunkLoadEvent e){
		Chunk c = e.getChunk();
		for(Shop shop : plugin.getShops().values()){
			if(shop.getLocation().getChunk().equals(c)){
				DisplayItem disItem = shop.getDisplayItem();
				disItem.removeDupe();
				disItem.remove();
				disItem.spawn();
				plugin.getLogger().info("Chunk loading spawning item: " + disItem.getItem().getItemStack().getType());
			}
		}
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void onChunkUnload(ChunkUnloadEvent e){
		Chunk c = e.getChunk();
		for(Shop shop : plugin.getShops().values()){
			if(shop.getLocation().getChunk().equals(c)){
				DisplayItem disItem = shop.getDisplayItem();
				disItem.removeDupe();
				disItem.remove();
				disItem.spawn();
				plugin.getLogger().info("Chunk loading spawning item: " + disItem.getItem().getItemStack().getType());
			}
		}
	}
}