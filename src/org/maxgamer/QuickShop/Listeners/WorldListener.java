package org.maxgamer.QuickShop.Listeners;

import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.maxgamer.QuickShop.QuickShop;
import org.maxgamer.QuickShop.Shop.Shop;
import org.maxgamer.QuickShop.Shop.ShopChunk;

public class WorldListener implements Listener{
	QuickShop plugin;
	
	public WorldListener(QuickShop plugin){
		this.plugin = plugin;
	}
	
	@EventHandler
	public void onWorldLoad(WorldLoadEvent e){
		/* *************************************
		 * This listener fixes any broken world 
		 * references.  Such as hashmap lookups
		 * will fail, because the World reference
		 * is different, but the world value is
		 * the same.
		 * *************************************/
		World world = e.getWorld();
		
		//New world data
		HashMap<ShopChunk, HashMap<Location, Shop>> inWorld = new HashMap<ShopChunk, HashMap<Location, Shop>>(1); 
		
		//Old world data
		HashMap<ShopChunk, HashMap<Location, Shop>> oldInWorld = plugin.getShopManager().getShops(world.getName());
		
		//Nothing in the old world, therefore we don't care.  No locations to update.
		if(oldInWorld == null) return;
		
		for(Entry<ShopChunk, HashMap<Location, Shop>> oldInChunk : oldInWorld.entrySet()){
			HashMap<Location, Shop> inChunk = new HashMap<Location, Shop>(1);
			
			//Put the new chunk were the old chunk was
			inWorld.put(oldInChunk.getKey(), inChunk);
			
			for(Entry<Location, Shop> entry : oldInChunk.getValue().entrySet()){
				Shop shop = entry.getValue();
				shop.getLocation().setWorld(world);

				inChunk.put(shop.getLocation(), shop);
			}
		}
		//Done - Now we can store the new world dataz!
		plugin.getShopManager().getShops().put(world.getName(), inWorld);
		
		//This is a workaround, because I don't get parsed chunk events when a world first loads....
		//So manually tell all of these shops they're loaded.
		for(Chunk chunk : world.getLoadedChunks()){
			HashMap<Location, Shop> inChunk = plugin.getShopManager().getShops(chunk);
			if(inChunk == null) continue;
			
			for(Shop shop : inChunk.values()){
				shop.onLoad();
			}
		}
	}
	
	@EventHandler
	public void onWorldUnload(WorldUnloadEvent e){
		//This is a workaround, because I don't get parsed chunk events when a world unloads, I think...
		//So manually tell all of these shops they're unloaded.
		for(Chunk chunk : e.getWorld().getLoadedChunks()){
			HashMap<Location, Shop> inChunk = plugin.getShopManager().getShops(chunk);
			if(inChunk == null) continue;
			
			for(Shop shop : inChunk.values()){
				shop.onUnload();
			}
		}
	}
}