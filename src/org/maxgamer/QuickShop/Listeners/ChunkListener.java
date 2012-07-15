package org.maxgamer.QuickShop.Listeners;

import java.util.Map.Entry;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.maxgamer.QuickShop.QuickShop;
import org.maxgamer.QuickShop.Shop;

public class ChunkListener implements Listener{
	QuickShop plugin;
	public ChunkListener(QuickShop plugin){
		this.plugin = plugin;
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onChunkLoad(ChunkLoadEvent e){
		for(Entry<Location, Shop> map:plugin.getShops().entrySet()){
			Location loc = map.getKey();
			Shop shop = map.getValue();
			
			if(loc.getChunk().equals(e.getChunk())){
				//This is a shop chunk.
				Entity[] entities = e.getChunk().getEntities();
				for(Entity entity : entities){
					if(entity instanceof Item){
						Item item = (Item) entity;
						if(item.getLocation().getBlock().equals(loc)){
							ItemStack itemstack = item.getItemStack(); 
							if(itemstack.getType() == shop.getMaterial() && itemstack.getDurability() == shop.getItem().getDurability() && itemstack.getAmount() == 1){
								//Same item, same type, only 1... Probably right.
								shop.deleteDisplayItem();
								item.remove();
								shop.spawnDisplayItem();
							}
						}
					}
				}
			}
		}
	}
}