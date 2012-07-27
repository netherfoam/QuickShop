package org.maxgamer.QuickShop.Listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;
import org.maxgamer.QuickShop.QuickShop;
import org.maxgamer.QuickShop.Shop.Info;
import org.maxgamer.QuickShop.Shop.Shop;
import org.maxgamer.QuickShop.Shop.ShopAction;


/**
 * Handles players clicking on chests:
 * Left click on chest with item	: 	Ask for price
 * Left click on shop		 		: 	Send price
 * Right click on others shop		: 	Send sell menu
 * Right click on own shop	 		: 	Open chest normally
 * @author Netherfoam
 *
 */
public class ClickListener implements Listener{
	QuickShop plugin;
	HashSet<Material> blacklist = new HashSet<Material>(10);
	
	public ClickListener(QuickShop plugin){
		this.plugin = plugin;
		List<String> configBlacklist = plugin.getConfig().getStringList("blacklist");
		
		for(String s : configBlacklist){
			Material mat = Material.getMaterial(s);
			if(mat == null){
				mat = Material.getMaterial(Integer.parseInt(s));
				if(mat == null){
					plugin.getLogger().info(s + " is not a valid material.  Check your spelling or ID");
					continue;
				}
			}
			this.blacklist.add(mat);
		}
		
	}
	@EventHandler
	/**
	 * Handles players left clicking a chest.
	 * Left click a NORMAL chest with item	: Send creation menu
	 * Left click a SHOP   chest			: Send purchase menu
	 */
	public void onClick(PlayerInteractEvent e){
		if(e.isCancelled() || e.getAction() != Action.LEFT_CLICK_BLOCK || (e.getClickedBlock().getType() != Material.CHEST && e.getClickedBlock().getType() != Material.WALL_SIGN)) return;
		
		Block b = e.getClickedBlock();
		Player p = e.getPlayer();
		ItemStack item = e.getItem();
		Location loc = b.getLocation();
		
		//Get the shop
		Shop shop = plugin.getShop(loc);
		//If that wasn't a shop, search nearby shops
		if(shop == null) shop = getShopNextTo(loc);

		/* 
		 * Purchase Handling
		 */
		if(shop != null && p.hasPermission("quickshop.buy")){
			if(plugin.getConfig().getBoolean("shop.sneak-only") && !p.isSneaking()){
				//Sneak only
				return;
			}
			//Text menu
			sendShopInfo(p, shop);
			p.sendMessage(ChatColor.GREEN + "Enter how many you wish to purchase in chat.");
			
			//Add the new action
			HashMap<String, Info> actions = plugin.getActions();
			actions.remove(p.getName());
			Info info = new Info(shop.getLocation(), ShopAction.BUY, null, null);
			actions.put(p.getName(), info);
			
			return;
		}
		/*
		 * Creation handling
		 */
		else if(item != null && item.getType() != Material.AIR && p.hasPermission("quickshop.create") && b.getType() == Material.CHEST && shop == null){
			if(!plugin.canBuildShop(p, b)){
				p.sendMessage(ChatColor.RED + "You may not create a shop here.");
				//e.setCancelled(true);
				return;
			}
			if(plugin.getChestNextTo(b) != null){
				p.sendMessage(ChatColor.RED + "Double chest shops are disabled.");
				//e.setCancelled(true);
				return;
			}
			
			if(blacklist.contains(item.getType()) && !p.hasPermission("quickshop.bypass."+item.getTypeId())){
				p.sendMessage(ChatColor.RED + "That item is blacklisted. You may not sell it.");
				return;
			}
			
			Block last = null;
			Location from = p.getLocation().clone();
			from.setY(b.getY());
			from.setPitch(0);
			BlockIterator bIt = new BlockIterator(from, 7);
			while(bIt.hasNext()){
				Block n = bIt.next();
				if(n.getLocation().distanceSquared(b.getLocation()) < 0.1){
					break;
				}
				last = n;
			}
			
			
			//Send creation menu.
			Info info = new Info(b.getLocation(), ShopAction.CREATE, e.getItem(), last);
			plugin.getActions().put(p.getName(), info);
			p.sendMessage(ChatColor.GREEN + "Enter how much you wish to sell one "+ ChatColor.YELLOW  + item.getType().toString() + ChatColor.GREEN + " for.");
		}
	}
	
	private void sendShopInfo(Player p, Shop shop){
		sendShopInfo(p, shop, shop.getRemainingStock());
	}
	private void sendShopInfo(Player p, Shop shop, int stock){
		ItemStack items = shop.getItem();
		p.sendMessage("");
		p.sendMessage("");
		p.sendMessage(ChatColor.DARK_PURPLE + "+---------------------------------------------------+");
		p.sendMessage(ChatColor.DARK_PURPLE + "| " + ChatColor.GREEN + "Shop Information:");
		p.sendMessage(ChatColor.DARK_PURPLE + "| " + ChatColor.GREEN + "Owner: " + shop.getOwner());
		p.sendMessage(ChatColor.DARK_PURPLE + "| " + ChatColor.GREEN + "Item: " + ChatColor.YELLOW + plugin.getDataName(items.getType(), items.getDurability()));
		p.sendMessage(ChatColor.DARK_PURPLE + "| " + ChatColor.GREEN + "Stock: " + ChatColor.YELLOW + stock);
		p.sendMessage(ChatColor.DARK_PURPLE + "| " + ChatColor.GREEN + "Price per "+ChatColor.YELLOW + items.getType() + ChatColor.GREEN + " - " + ChatColor.YELLOW + shop.getPrice() + ChatColor.GREEN + " credits");
		p.sendMessage(ChatColor.DARK_PURPLE + "| " + ChatColor.GREEN + "Total Value of Chest: " + ChatColor.YELLOW + shop.getPrice() * stock + ChatColor.GREEN + " credits");
			
		if(plugin.isTool(items.getType())){
			p.sendMessage(ChatColor.DARK_PURPLE + "| " + ChatColor.GREEN + plugin.getToolPercentage(items) + "% Remaining"); 
		}
		
		Map<Enchantment, Integer> enchs = items.getEnchantments();
		if(enchs != null && enchs.size() > 0){
			p.sendMessage(ChatColor.DARK_PURPLE + "+--------------------ENCHANTS-----------------------+");
			for(Entry<Enchantment, Integer> entries : enchs.entrySet()){
				p.sendMessage(ChatColor.DARK_PURPLE + "| " + ChatColor.YELLOW + entries.getKey() .getName() + " " + entries.getValue() );
			}
		}
		p.sendMessage(ChatColor.DARK_PURPLE + "+---------------------------------------------------+");
	}

	@EventHandler(priority = EventPriority.HIGH)
	/**
	 * Locks chests if enabled in config.
	 */
	public void onChestUse(PlayerInteractEvent e){
		if(e.isCancelled() || e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock().getType() != Material.CHEST) return;
		if(plugin.getConfig().getBoolean("shops.lock")){
			Shop shop = plugin.getShop(e.getClickedBlock().getLocation());
			if(shop != null && !shop.getOwner().equalsIgnoreCase(e.getPlayer().getName())){
				e.getPlayer().sendMessage(ChatColor.RED + "[QuickShop] That shop is locked.  Left click if you wish to buy!");
				e.setCancelled(true);
				return;
			}
		}
	}
	
	private Shop getShopNextTo(Location loc){
		Block[] blocks = new Block[4];
		blocks[0] = loc.getBlock().getRelative(1, 0, 0);
		blocks[1] = loc.getBlock().getRelative(-1, 0, 0);
		blocks[2] = loc.getBlock().getRelative(0, 0, 1);
		blocks[3] = loc.getBlock().getRelative(0, 0, -1);
		
		for(Block b : blocks){
			if(b.getType() != Material.CHEST) continue;
			Shop shop = plugin.getShop(b.getLocation());
			if(shop != null) return shop;
		}
		return null;
	}
}