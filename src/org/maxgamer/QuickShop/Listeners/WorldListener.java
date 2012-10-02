package org.maxgamer.QuickShop.Listeners;

import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
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
		/**
		 * Update all the references to worlds, if any.
		 * 
		 * Method:
		 * 		for one instance of world=>
		 *		For each chunk in the world
		 *			for each shop in the chunk
		 *				update the location
		 *				fix item/display items
		 *				store it in the new chunk
		 *			store the new chunk in the new world
		 *		store the new world in the world list
		 */
		
		plugin.debug("Fixing depreciated world references");
		World world = e.getWorld();
		
		//New world data
		HashMap<ShopChunk, HashMap<Location, Shop>> inWorld = new HashMap<ShopChunk, HashMap<Location, Shop>>(1); 
		
		//Old world data
		HashMap<ShopChunk, HashMap<Location, Shop>> oldInWorld = plugin.getShopManager().getShops(world.getName());
		
		//Nothing in the old world, therefore we don't care.  No locations to update.
		if(oldInWorld == null) return;
		
		for(Entry<ShopChunk, HashMap<Location, Shop>> oldInChunk : oldInWorld.entrySet()){
			plugin.debug("Inspecting chunk");
			HashMap<Location, Shop> inChunk = new HashMap<Location, Shop>(1);
			
			//Put the new chunk were the old chunk was
			inWorld.put(oldInChunk.getKey(), inChunk);
			
			for(Entry<Location, Shop> entry : oldInChunk.getValue().entrySet()){
				/**
				 * 1. Fix each shops world
				 * 2. Fix each shops display item's world
				 * 3. Fix each shops display item's item's world
				 * 4. Put the shops back in the new inChunk
				 */

				plugin.debug("Inspecting shop");
				Shop shop = entry.getValue();
				shop.getLocation().setWorld(world);
				
				//TODO: Check display items are enabled
				shop.getDisplayItem().getDisplayLocation().setWorld(world);
				if(shop.getDisplayItem().getItem() != null){
					shop.getDisplayItem().getItem().getLocation().setWorld(world);
				}
				
				inChunk.put(shop.getLocation(), shop);
			}
		}
		//Done - Now we can store the new world dataz!
		plugin.getShopManager().getShops().put(world.getName(), inWorld);
	}
}