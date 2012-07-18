package org.maxgamer.QuickShop.Listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.maxgamer.QuickShop.QuickShop;
import org.maxgamer.QuickShop.Shop.DisplayItem;
import org.maxgamer.QuickShop.Shop.Info;
import org.maxgamer.QuickShop.Shop.Shop;
import org.maxgamer.QuickShop.Shop.ShopAction;


public class BlockListener implements Listener{
	QuickShop plugin;
	public BlockListener(QuickShop plugin){
		this.plugin = plugin;
	}
	/**
	 * Removes chests when they're destroyed.
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBreak(final BlockBreakEvent e){
		if(e.isCancelled() || e.getBlock().getType() != Material.CHEST) return;
		Shop shop = plugin.getShops().get(e.getBlock().getLocation());
		//If the chest was a shop
		if(shop != null){
			for(Info info : plugin.getActions().values()){
				info.setAction(ShopAction.CANCELLED);
			}
			//Refunding the shop
			if(plugin.getConfig().getBoolean("shop.refund")){
				double cost = plugin.getConfig().getDouble("shop.cost");
				plugin.getEcon().depositPlayer(shop.getOwner(), cost);
				plugin.getEcon().withdrawPlayer(plugin.getConfig().getString("tax-account"), cost);
			}
			
			//Remove the item on top
			shop.getDisplayItem().remove();
			
			//Remove the shop from the cache
			plugin.getShops().remove(e.getBlock().getLocation());
			e.getPlayer().sendMessage(ChatColor.GREEN + "Shop Removed");
			
			//Insert it into the buffer for the database.
			Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, new Runnable(){
				@Override
				public void run() {
					String world = e.getBlock().getWorld().getName();
					int x = e.getBlock().getX();
					int y = e.getBlock().getY();
					int z = e.getBlock().getZ();
					
					while(plugin.queriesInUse){
						//Wait
					}
					
					plugin.queriesInUse = true;
					plugin.queries.add("DELETE FROM shops WHERE x = '"+x+"' AND y = '"+y+"' AND z = '"+z+"' AND world = '"+world+"'");
					plugin.queriesInUse = false;
				}
				
			}, 0);
		}
	}
	/**
	 * Listens for chest placement, so a doublechest shop can't be created.
	 */
	@EventHandler
	public void onPlace(BlockPlaceEvent e){
		if(e.isCancelled() || e.getBlock().getType() != Material.CHEST) return;
		
		Block b = e.getBlock();
		if(plugin.getChestNextTo(b) != null && plugin.getShop(plugin.getChestNextTo(b).getLocation()) != null){
			e.setCancelled(true);
			e.getPlayer().sendMessage(ChatColor.RED + "Double Chest shops are disabled.");
		}
	}
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onExplode(EntityExplodeEvent e){
		if(e.isCancelled()) return;
		for(int i = 0; i < e.blockList().size(); i++){
			Block b = e.blockList().get(i);
			if(plugin.getShops().containsKey(b.getLocation())){
				if(plugin.getConfig().getBoolean("lock-shops")){
					e.blockList().remove(b);
					DisplayItem disItem = plugin.getShop(b.getLocation()).getDisplayItem();
					disItem.remove();
				}
				else{
					Shop shop = plugin.getShop(b.getLocation());
					shop.getDisplayItem().remove();
					int x = shop.getLocation().getBlockX();
					int y = shop.getLocation().getBlockY();
					int z = shop.getLocation().getBlockZ();
					String world = shop.getLocation().getWorld().getName();
					
					if(plugin.getConfig().getBoolean("shop.refund")){
						plugin.getEcon().depositPlayer(shop.getOwner(), plugin.getConfig().getDouble("shop.cost"));
					}
					
					plugin.queries.add("DELETE FROM shops WHERE x = '"+x+"' AND y = '"+y+"' AND z = '"+z+"' AND world = '"+world+"'");
				}
			}
		}
	}
}