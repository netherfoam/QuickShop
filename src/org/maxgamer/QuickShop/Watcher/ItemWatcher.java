package org.maxgamer.QuickShop.Watcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.maxgamer.QuickShop.QuickShop;
import org.maxgamer.QuickShop.Shop.DisplayItem;
import org.maxgamer.QuickShop.Shop.Shop;
import org.maxgamer.QuickShop.Shop.ShopChunk;


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
		List<Shop> toRemove = new ArrayList<Shop>(1);
		for(Entry<String, HashMap<ShopChunk, HashMap<Location, Shop>>> inWorld : plugin.getShopManager().getShops().entrySet()){
			//This world
			World world = Bukkit.getWorld(inWorld.getKey());
			
			for(Entry<ShopChunk, HashMap<Location, Shop>> inChunk : inWorld.getValue().entrySet()){
				if(!world.isChunkLoaded(inChunk.getKey().getX(), inChunk.getKey().getZ())){
					//If the chunk is not loaded, next chunk!
					continue;
				}
				
				for(Shop shop : inChunk.getValue().values()){
					Location loc = shop.getLocation();
					DisplayItem disItem = shop.getDisplayItem();
					/* This should no longer be needed
					if(loc.getWorld() == null){
						//Unloaded world.
						break;
					}
					else if(!loc.getChunk().isLoaded()){
						System.out.println("Chunk isnt loaded.");
						//Unloaded chunk
						break;
					}*/
					if(loc.getBlock() != null && loc.getBlock().getType() != Material.CHEST){
						//The block is nolonger a chest (Maybe WorldEdit or something?)
						shop.delete(false);
						
						//We can't remove it yet, we're still iterating over this!
						toRemove.add(shop);
					}
					else if(shop.getLocation().getBlock().getRelative(0, 1, 0).getType() == Material.WATER){
						//Testing - Don't teleport the item back if they're trying to water dupe.
						disItem.remove();
					}
					else if(/*disItem != null && */(disItem.getItem() == null || disItem.getItem().getTicksLived() >= 5000 || disItem.getItem().isDead())){
						//Needs respawning (its about to despawn)
						if(disItem.removeDupe()){
							plugin.log("[Debug] Item watcher was forced to remove that!");
						}
						disItem.respawn();
					}
					else if(disItem.getDisplayLocation().distanceSquared(disItem.getItem().getLocation()) > 1){
						//Needs to be teleported back. (TODO: Despawn the item after 3 strikes OSTL? Necessary?)
						disItem.getItem().teleport(disItem.getDisplayLocation(), TeleportCause.PLUGIN);
					}
				}
			}
		}
		
		//Now we can remove it.
		for(Shop shop : toRemove){
			plugin.getShopManager().removeShop(shop);
		}
	}
}