package org.maxgamer.QuickShop.Watcher;

import java.util.HashSet;
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
		
		for(Shop shop : plugin.getShops().values()){
			Location loc = shop.getLocation();
			DisplayItem disItem = shop.getDisplayItem();
			
			if(loc.getBlock() != null && loc.getBlock().getType() != Material.CHEST){
				//The block is nolonger a chest (Maybe WorldEdit or something?)
				shop.delete(false);
				
				//We can't remove it yet, we're still iterating over this!
				toRemove.add(shop);
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
		
		//Now we can remove it.
		for(Shop shop : toRemove){
			plugin.removeShop(shop);
		}
	}
}