package org.maxgamer.QuickShop.Listeners;

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
	private QuickShop plugin;
	public BlockListener(QuickShop plugin){
		this.plugin = plugin;
	}
	/**
	 * Removes chests when they're destroyed.
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBreak(final BlockBreakEvent e){
		if(e.isCancelled() || e.getBlock().getType() != Material.CHEST) return;
		Shop shop = plugin.getShopManager().getShop(e.getBlock().getLocation());
		
		//If the chest was a shop
		if(shop != null){
			Player p = e.getPlayer();
			if(plugin.lock){
				if(!shop.getOwner().equalsIgnoreCase(p.getName()) && !p.hasPermission("quickshop.other.destroy")){
					e.setCancelled(true);
					p.sendMessage(plugin.getMessage("no-permission"));
					return;
				}
			}
			
			if(p.getGameMode() == GameMode.CREATIVE && !p.getName().equalsIgnoreCase(shop.getOwner())){
				e.setCancelled(true);
				p.sendMessage(plugin.getMessage("no-creative-break"));
				return;
			}
			Info action = plugin.getActions().get(p.getName());
			if(action != null){
				action.setAction(ShopAction.CANCELLED);
			}
			shop.delete();
			p.sendMessage(plugin.getMessage("success-removed-shop"));
		}
	}
	/**
	 * Listens for chest placement, so a doublechest shop can't be created.
	 */
	@EventHandler
	public void onPlace(BlockPlaceEvent e){
		if(e.isCancelled() || e.getBlock().getType() != Material.CHEST) return;
		Block b = e.getBlock();
		Block chest = plugin.getChestNextTo(b);
		if(chest != null && plugin.getShopManager().getShop(chest.getLocation()) != null){
			e.setCancelled(true);
			e.getPlayer().sendMessage(plugin.getMessage("no-double-chests"));
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
			if(plugin.getShopManager().getShop(b.getLocation()) != null){
				if(plugin.lock){
					//ToDo: Shouldn't I be decrementing 1 here? Concurrency and all..
					e.blockList().remove(b);
					DisplayItem disItem = plugin.getShopManager().getShop(b.getLocation()).getDisplayItem();
					disItem.remove();
				}
				else{
					Shop shop = plugin.getShopManager().getShop(b.getLocation());
					shop.delete();
				}
			}
		}
	}
}