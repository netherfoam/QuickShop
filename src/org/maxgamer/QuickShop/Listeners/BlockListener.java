package org.maxgamer.QuickShop.Listeners;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
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
			Player p = e.getPlayer();
			if(plugin.getConfig().getBoolean("shop.lock")){
				if(!shop.getOwner().equalsIgnoreCase(p.getName()) && !p.hasPermission("quickshop.other.destroy")){
					e.setCancelled(true);
					p.sendMessage(ChatColor.RED + "You don't have permission to destroy " + shop.getOwner() + "'s shop");
					return;
				}
			}
			
			if(p.getGameMode() == GameMode.CREATIVE && !p.getName().equalsIgnoreCase(shop.getOwner())){
				e.setCancelled(true);
				p.sendMessage(ChatColor.RED + "You cannot break other players shops in creative mode.  Use survival instead.");
				return;
			}
			Info action = plugin.getActions().get(p.getName());
			if(action != null){
				action.setAction(ShopAction.CANCELLED);
			}
			shop.delete();
			p.sendMessage(ChatColor.GREEN + "Shop Removed");
		}
	}
	/**
	 * Listens for chest placement, so a doublechest shop can't be created.
	 */
	@EventHandler
	public void onPlace(BlockPlaceEvent e){
		if(e.isCancelled()) return;
		Block b = e.getBlock();
		Block chest = plugin.getChestNextTo(b);
		if(b.getType() == Material.CHEST && chest != null && plugin.getShop(chest.getLocation()) != null){
			e.setCancelled(true);
			e.getPlayer().sendMessage(ChatColor.RED + "Double Chest shops are disabled.");
		}
	}
	/**
	 * Handles shops breaking through explosions
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onExplode(EntityExplodeEvent e){
		if(e.isCancelled()) return;
		for(int i = 0; i < e.blockList().size(); i++){
			Block b = e.blockList().get(i);
			if(plugin.getShops().containsKey(b.getLocation())){
				if(plugin.getConfig().getBoolean("shops.lock")){
					e.blockList().remove(b);
					DisplayItem disItem = plugin.getShop(b.getLocation()).getDisplayItem();
					disItem.remove();
				}
				else{
					Shop shop = plugin.getShop(b.getLocation());
					shop.delete();
				}
			}
		}
	}
}