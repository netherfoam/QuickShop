package org.maxgamer.QuickShop;

import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class ItemWatcher implements Runnable{
	public void run(){
		QuickShop plugin = (QuickShop) Bukkit.getServer().getPluginManager().getPlugin("QuickShop");
		for(Entry<Location, Shop> entry : plugin.getShops().entrySet()){
			DisplayItem disItem = entry.getValue().getDisplayItem();
			if(entry.getKey().getChunk().isLoaded() && disItem.getItem().getTicksLived() >= 5000 || disItem.getItem().isDead()){
				
				disItem.removeDupe();
				disItem.respawn();
			}
		}
	}
}