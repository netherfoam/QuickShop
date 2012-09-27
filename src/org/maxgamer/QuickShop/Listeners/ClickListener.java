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
 * @author Netherfoam
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
		if(e.isCancelled()) return;
		if(e.getClickedBlock().getType() != Material.CHEST && e.getClickedBlock().getType() != Material.WALL_SIGN) return;
		
		if(e.getAction() == Action.RIGHT_CLICK_BLOCK){
			if(e.getClickedBlock().getType() == Material.CHEST && plugin.lock){
				Shop shop = plugin.getShopManager().getShop(e.getClickedBlock().getLocation());
				if(shop != null && !shop.getOwner().equalsIgnoreCase(e.getPlayer().getName())){
					if(e.getPlayer().hasPermission("quickshop.other.open")){
						e.getPlayer().sendMessage(plugin.getMessage("bypassing-lock"));
						return;
					}
					e.getPlayer().sendMessage(plugin.getMessage("that-is-locked"));
					e.setCancelled(true);
					return;
				}
			}
			return;
		}
		else if(e.getAction() != Action.LEFT_CLICK_BLOCK){
			return;
		}
		
		Player p = e.getPlayer();
		
		if(plugin.sneak && !p.isSneaking()){
			//Sneak only
			return;
		}
		
		Block b = e.getClickedBlock();
		Location loc = b.getLocation();
		ItemStack item = e.getItem();
		
		//Get the shop
		Shop shop = plugin.getShopManager().getShop(loc);
		//If that wasn't a shop, search nearby shops
		if(shop == null) shop = getShopNextTo(loc);

		/* 
		 * Purchase Handling
		 */
		if(shop != null && p.hasPermission("quickshop.use")){
			//Text menu
			sendShopInfo(p, shop);
			if(shop.isSelling()){
				p.sendMessage(plugin.getMessage("how-many-buy"));
			}
			else{
				p.sendMessage(plugin.getMessage("how-many-sell"));
			}
			
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
		else if(shop == null && item != null && item.getType() != Material.AIR && p.hasPermission("quickshop.create.sell") && b.getType() == Material.CHEST){
			if(!plugin.canBuildShop(p, b)){
				p.sendMessage(plugin.getMessage("not-allowed-to-create"));
				return;
			}
			if(plugin.getChestNextTo(b) != null){
				p.sendMessage(plugin.getMessage("no-double-chests"));
				return;
			}
			
			if(blacklist.contains(item.getType()) && !p.hasPermission("quickshop.bypass."+item.getTypeId())){
				p.sendMessage(plugin.getMessage("blacklisted-item"));
				return;
			}
			
			Block last = null;
			Location from = p.getLocation().clone();
			from.setY(b.getY());
			from.setPitch(0);
			BlockIterator bIt = new BlockIterator(from, 0, 7);
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
			p.sendMessage(plugin.getMessage("how-much-to-trade-for", plugin.getDataName(info.getItem().getType(), info.getItem().getDurability())));
		}
	}
	
	private void sendShopInfo(Player p, Shop shop){
		sendShopInfo(p, shop, shop.getRemainingStock());
	}
	private void sendShopInfo(Player p, Shop shop, int stock){
		//Potentially faster with an array?
		ItemStack items = shop.getItem();
		p.sendMessage("");
		p.sendMessage("");
		
		p.sendMessage(ChatColor.DARK_PURPLE + "+---------------------------------------------------+");
		p.sendMessage(ChatColor.DARK_PURPLE + "| " + plugin.getMessage("menu.shop-information"));
		p.sendMessage(ChatColor.DARK_PURPLE + "| " + plugin.getMessage("menu.owner", shop.getOwner()));
		p.sendMessage(ChatColor.DARK_PURPLE + "| " + plugin.getMessage("menu.item", plugin.getDataName(items.getType(), items.getDurability())));
		
		if(plugin.isTool(items.getType())){
			p.sendMessage(ChatColor.DARK_PURPLE + "| " + plugin.getMessage("menu.damage-percent-remaining", plugin.getToolPercentage(items)));
		}
		
		if(shop.isSelling()){
			//TODO: Can I send infinity chars?
			//if(stock == 10000){
			//	p.sendMessage(ChatColor.DARK_PURPLE + "| " + plugin.getMessage("menu.stock", "\u236A"));
			//}
			//else{
				p.sendMessage(ChatColor.DARK_PURPLE + "| " + plugin.getMessage("menu.stock", ""+stock));
			//}
		}
		else{
			int space = shop.getRemainingSpace(shop.getMaterial().getMaxStackSize());
			//if(space == 10000){
			//	p.sendMessage(ChatColor.DARK_PURPLE + "| " + plugin.getMessage("menu.space", "\u236A"));
			//}
			//else{
				p.sendMessage(ChatColor.DARK_PURPLE + "| " + plugin.getMessage("menu.space", ""+space));
			//}
		}
		
		p.sendMessage(ChatColor.DARK_PURPLE + "| " + plugin.getMessage("menu.price-per", plugin.getDataName(shop.getMaterial(), shop.getDurability()), plugin.getEcon().format(shop.getPrice())));
		
		p.sendMessage(ChatColor.DARK_PURPLE + "| " + plugin.getMessage("average-price-nearby", plugin.getEcon().format(shop.getAverage(48))));
		
		p.sendMessage(ChatColor.DARK_PURPLE + "| " + plugin.getMessage("menu.total-value-of-chest", plugin.getEcon().format(shop.getPrice() * stock)));
		
		
		
		if(shop.isBuying()){
			p.sendMessage(ChatColor.DARK_PURPLE + "| " + plugin.getMessage("menu.this-shop-is-buying"));
		}
		else{
			p.sendMessage(ChatColor.DARK_PURPLE + "| " + plugin.getMessage("menu.this-shop-is-selling"));
		}
			
		Map<Enchantment, Integer> enchs = items.getEnchantments();
		if(enchs != null && enchs.size() > 0){
			p.sendMessage(ChatColor.DARK_PURPLE + "+--------------------"+plugin.getMessage("menu.enchants")+"-----------------------+");
			for(Entry<Enchantment, Integer> entries : enchs.entrySet()){
				p.sendMessage(ChatColor.DARK_PURPLE + "| " + ChatColor.YELLOW + entries.getKey() .getName() + " " + entries.getValue() );
			}
		}
		p.sendMessage(ChatColor.DARK_PURPLE + "+---------------------------------------------------+");
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