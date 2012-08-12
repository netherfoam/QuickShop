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
			if(entry.getValue().getLocation().getWorld() == null) continue; //World not loaded
			
			DisplayItem disItem = entry.getValue().getDisplayItem();
			if(entry.getValue().getLocation().getBlock() != null && entry.getValue().getLocation().getBlock().getType() != Material.CHEST){
				/* Shop is invalid */
				Location loc = entry.getValue().getLocation();
				int x = loc.getBlockX();
				int y = loc.getBlockY();
				int z = loc.getBlockZ();
				String world = loc.getWorld().getName();
				
				plugin.getLogger().info("Shop is not a chest in " +world + " at: " + x + ", " + y + ", " + z + ".  Removing from DB.");
				plugin.getDB().writeToBuffer("DELETE FROM shops WHERE x = "+x+" AND y = "+y+" AND z = "+z+" AND world = '"+world+"'");
				
				//We can't remove it yet, we're still iterating over this!
				toRemove.add(loc);
				//But we can delete the shops display item
				entry.getValue().getDisplayItem().remove();
			}
			else if(entry.getKey().getChunk().isLoaded() && disItem.getItem().getTicksLived() >= 5000 || disItem.getItem().isDead() || disItem.getDisplayLocation().distanceSquared(disItem.getItem().getLocation()) > 1){
				disItem.removeDupe();
				disItem.respawn();
			}
		}
		//Now we can remove it.
		for(Location loc : toRemove){
			plugin.removeShop(loc);
		}
	}
}