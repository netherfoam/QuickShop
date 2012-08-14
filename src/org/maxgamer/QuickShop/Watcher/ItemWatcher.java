package org.maxgamer.QuickShop.Watcher;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.maxgamer.QuickShop.QuickShop;
import org.maxgamer.QuickShop.Shop.DisplayItem;
import org.maxgamer.QuickShop.Shop.Shop;


/**
 * @author Netherfoam
 * Maintains the display items, restoring them when needed.
 * Also deletes invalid items.
 */
public class ItemWatcher implements Runnable{
	private QuickShop plugin;
	public ItemWatcher(QuickShop plugin){
		this.plugin = plugin;
	}
	
	public void run(){
		HashSet<Shop> toRemove = new HashSet<Shop>(1);
		
		//For each chunk
		for(Entry<Chunk, ConcurrentHashMap<Location, Shop>> chunkmap : plugin.getShopChunks().entrySet()){
			if(chunkmap.getKey().getWorld() == null) continue; //World not loaded
			if(!chunkmap.getKey().isLoaded()) continue; //Chunk not loaded
			
			//For each shop in the chunk
			for(Entry<Location, Shop> shopmap : chunkmap.getValue().entrySet()){
				Location loc = shopmap.getKey();
				DisplayItem disItem = shopmap.getValue().getDisplayItem();
				
				if(loc.getBlock() != null && loc.getBlock().getType() != Material.CHEST){
					//The block is nolonger a chest (Maybe WorldEdit or something?)
					shopmap.getValue().delete(false);
					
					//We can't remove it yet, we're still iterating over this!
					toRemove.add(shopmap.getValue());
				}
				else if(disItem.getItem().getTicksLived() >= 5000 || disItem.getItem().isDead()){
					//Needs respawning (its about to despawn)
					disItem.removeDupe();
					disItem.respawn();
				}
				else if(disItem.getDisplayLocation().distanceSquared(disItem.getItem().getLocation()) > 1){
					//Needs to be teleported back. (TODO: Despawn the item after 3 strikes OSTL? Necessary?)
					disItem.getItem().teleport(disItem.getDisplayLocation(), TeleportCause.PLUGIN);
				}
			}
		}
		
		//Now we can remove it.
		for(Shop shop : toRemove){
			plugin.removeShop(shop);
		}
	}
}