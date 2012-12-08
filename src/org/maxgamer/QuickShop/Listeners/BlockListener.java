package org.maxgamer.QuickShop.Listeners;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.maxgamer.QuickShop.MsgUtil;
import org.maxgamer.QuickShop.QuickShop;
import org.maxgamer.QuickShop.Util;
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
		if(e.isCancelled()) return;
		
		Block b = e.getBlock();
		Player p = e.getPlayer();
		
		//If the chest was a chest
		if(b.getType() == Material.CHEST){
			Shop shop = plugin.getShopManager().getShop(b.getLocation());
			//If it was a shop
			if(shop != null){
				if(plugin.lock){
					//If they owned it or have bypass perms, they can destroy it
					if(!shop.getOwner().equalsIgnoreCase(p.getName()) && !p.hasPermission("quickshop.other.destroy")){
						e.setCancelled(true);
						p.sendMessage(MsgUtil.getMessage("no-permission"));
						return;
					}
				}
				
				//If they're either survival or the owner, they can break it
				if(p.getGameMode() == GameMode.CREATIVE && !p.getName().equalsIgnoreCase(shop.getOwner())){
					e.setCancelled(true);
					p.sendMessage(MsgUtil.getMessage("no-creative-break"));
					return;
				}
				
				//Cancel their current menu... Doesnt cancel other's menu's.
				Info action = plugin.getActions().get(p.getName());
				if(action != null){
					action.setAction(ShopAction.CANCELLED);
				}
				shop.delete();
				p.sendMessage(MsgUtil.getMessage("success-removed-shop"));
			}
		}
		else if(b.getType() == Material.WALL_SIGN){
			Shop shop = getShopNextTo(e.getBlock().getLocation());
			if(shop != null){ //It is a shop sign we're dealing with.
				if(plugin.lock){
					//If they're the shop owner or have bypass perms, they can destroy it.
					if(!shop.getOwner().equalsIgnoreCase(p.getName()) && !e.getPlayer().hasPermission("quickshop.other.destroy")){
						e.setCancelled(true);
						p.sendMessage(MsgUtil.getMessage("no-permission"));
						return;
					}
				}
				//If they're in creative and not the owner, don't let them (accidents happen)
				if(p.getGameMode() == GameMode.CREATIVE && !p.getName().equalsIgnoreCase(shop.getOwner())){
					e.setCancelled(true);
					p.sendMessage(MsgUtil.getMessage("no-creative-break"));
					return;
				}
			}
		}
		
	}
	/**
	 * Listens for chest placement, so a doublechest shop can't be created.
	 */
	@EventHandler
	public void onPlace(BlockPlaceEvent e){
		if(e.isCancelled() || e.getBlock().getType() != Material.CHEST) return;
		Block b = e.getBlock();
		
		Block chest = Util.getSecondHalf(b);
		if(chest != null && plugin.getShopManager().getShop(chest.getLocation()) != null && !e.getPlayer().hasPermission("quickshop.create.double")){
			e.setCancelled(true);
			e.getPlayer().sendMessage(MsgUtil.getMessage("no-double-chests"));
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
	
	/**
	 * Gets the shop a sign is attached to
	 * @param loc The location of the sign
	 * @return The shop
	 */
	private Shop getShopNextTo(Location loc){
		Block[] blocks = new Block[4];
		blocks[0] = loc.getBlock().getRelative(1, 0, 0);
		blocks[1] = loc.getBlock().getRelative(-1, 0, 0);
		blocks[2] = loc.getBlock().getRelative(0, 0, 1);
		blocks[3] = loc.getBlock().getRelative(0, 0, -1);
		
		for(Block b : blocks){
			if(b.getType() != Material.CHEST) continue;
			Shop shop = plugin.getShopManager().getShop(b.getLocation());
			if(shop != null && shop.isAttached(loc.getBlock())) return shop;
		}
		return null;
	}
}