package org.maxgamer.QuickShop.Watcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
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
		for(HashMap<ShopChunk, HashMap<Location, Shop>> inWorld : plugin.getShopManager().getShops().values()){
			for(HashMap<Location, Shop> inChunk : inWorld.values()){
				for(Shop shop : inChunk.values()){
					Location loc = shop.getLocation();
					DisplayItem disItem = shop.getDisplayItem();
					
					if(loc.getWorld() == null){
						//Unloaded world.
						break;
					}
					else if(!loc.getChunk().isLoaded()){
						//Unloaded chunk
						break;
					}
					else if(loc.getBlock() != null && loc.getBlock().getType() != Material.CHEST){
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