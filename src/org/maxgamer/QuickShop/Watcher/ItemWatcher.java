package org.maxgamer.QuickShop.Watcher;

import java.util.HashSet;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.maxgamer.QuickShop.QuickShop;
import org.maxgamer.QuickShop.Shop.DisplayItem;
import org.maxgamer.QuickShop.Shop.Shop;


/**
 * @author Netherfoam
 * Maintains the display items, restoring them when needed.
 * Also deletes invalid items.
 */
public class ItemWatcher implements Runnable{
	public void run(){
		QuickShop plugin = (QuickShop) Bukkit.getServer().getPluginManager().getPlugin("QuickShop");
		HashSet<Location> toRemove = new HashSet<Location>(5);
		for(Entry<Location, Shop> entry : plugin.getShops().entrySet()){
			DisplayItem disItem = entry.getValue().getDisplayItem();
			if(entry.getValue().getLocation().getBlock() != null && entry.getValue().getLocation().getBlock().getType() != Material.CHEST){
				/* Shop is invalid */
				Location loc = entry.getValue().getLocation();
				int x = loc.getBlockX();
				int y = loc.getBlockY();
				int z = loc.getBlockZ();
				
				plugin.getLogger().info("Shop is not a chest at: " + x + ", " + y + ", " + z + ".  Removing from DB.");
				plugin.getDB().writeToBuffer("DELETE FROM shops WHERE x = "+x+" AND y = "+y+" AND z = "+z+"");
				//We can't remove it yet, we're still iterating!
				toRemove.add(loc);
				//continue;
			}
			else if(entry.getKey().getChunk().isLoaded() && disItem.getItem().getTicksLived() >= 5000 || disItem.getItem().isDead() || disItem.getDisplayLocation().distanceSquared(disItem.getItem().getLocation()) > 1){
				disItem.removeDupe();
				disItem.respawn();
			}
		}
		for(Location loc : toRemove){
			plugin.getShops().remove(loc);
		}
	}
}