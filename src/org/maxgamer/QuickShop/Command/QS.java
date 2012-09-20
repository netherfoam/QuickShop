package org.maxgamer.QuickShop.Command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.maxgamer.QuickShop.QuickShop;
import org.maxgamer.QuickShop.Shop.Shop;
import org.maxgamer.QuickShop.Shop.Shop.ShopType;
import org.maxgamer.QuickShop.Shop.ShopChunk;

public class QS implements CommandExecutor{
	QuickShop plugin;
	public QS(QuickShop plugin){
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
		if(args.length > 0){
			String subArg = args[0].toLowerCase();
			
			if(subArg.equals("unlimited")){
				if(sender instanceof Player && sender.hasPermission("quickshop.unlimited")){
					BlockIterator bIt = new BlockIterator((LivingEntity) (Player) sender, 10);
					while(bIt.hasNext()){
						Block b = bIt.next();
						Shop shop = plugin.getShopManager().getShop(b.getLocation());
						if(shop != null){
							shop.setUnlimited(true);
							shop.update();
							sender.sendMessage(plugin.getMessage("command.success-created-unlimited"));
							return true;
						}
					}
					sender.sendMessage(plugin.getMessage("not-looking-at-shop"));
					return true;
				}
				else{
					sender.sendMessage(plugin.getMessage("no-permission"));
					return true;
				}
			}
			else if(subArg.equals("setowner")){
				if(sender instanceof Player && sender.hasPermission("quickshop.setowner")){
					if(args.length < 2){
						sender.sendMessage(plugin.getMessage("command.no-owner-given"));
						return true;
					}
					BlockIterator bIt = new BlockIterator((LivingEntity) (Player) sender, 10);
					while(bIt.hasNext()){
						Block b = bIt.next();
						Shop shop = plugin.getShopManager().getShop(b.getLocation());
						if(shop != null){
							shop.setOwner(args[1]);
							shop.update();
							
							sender.sendMessage(ChatColor.GREEN + "New Owner: " + shop.getOwner());
							return true;
						}
					}
					sender.sendMessage(plugin.getMessage("not-looking-at-shop"));
					return true;
				}
				else{
					sender.sendMessage(plugin.getMessage("no-permission"));
					return true;
				}
			}
			
			else if(subArg.startsWith("buy")){
				if(sender instanceof Player && sender.hasPermission("quickshop.create.buy")){
					BlockIterator bIt = new BlockIterator((LivingEntity) (Player) sender, 10);
					while(bIt.hasNext()){
						Block b = bIt.next();
						Shop shop = plugin.getShopManager().getShop(b.getLocation());
						if(shop != null && shop.getOwner().equalsIgnoreCase(((Player) sender).getName())){
							shop.setShopType(ShopType.BUYING);
							shop.setSignText();
							shop.update();
							
							sender.sendMessage(plugin.getMessage("command.now-buying", shop.getDataName()));
							return true;
						}
					}
					sender.sendMessage(plugin.getMessage("not-looking-at-shop"));
					return true;
				}
				sender.sendMessage(plugin.getMessage("no-permission"));
				return true;
			}
			
			else if(subArg.startsWith("sell")){
				if(sender instanceof Player && sender.hasPermission("quickshop.create.sell")){
					BlockIterator bIt = new BlockIterator((LivingEntity) (Player) sender, 10);
					while(bIt.hasNext()){
						Block b = bIt.next();
						Shop shop = plugin.getShopManager().getShop(b.getLocation());
						if(shop != null && shop.getOwner().equalsIgnoreCase(((Player) sender).getName())){
							shop.setShopType(ShopType.SELLING);
							shop.setSignText();
							shop.update();
							sender.sendMessage(plugin.getMessage("command.now-selling", shop.getDataName()));
							return true;
						}
					}
					sender.sendMessage(plugin.getMessage("not-looking-at-shop"));
					return true;
				}
				sender.sendMessage(plugin.getMessage("no-permission"));
				return true;
			}
			
			else if(subArg.startsWith("price")){
				if(sender instanceof Player && sender.hasPermission("quickshop.create.changeprice")){
					if(args.length < 2){
						sender.sendMessage(plugin.getMessage("no-price-given"));
						return true;
					}
					double price;
					try{
						price = Double.parseDouble(args[1]);
					}
					catch(NumberFormatException e){
						sender.sendMessage(plugin.getMessage("thats-not-a-number"));
						return true;
					}
					
					BlockIterator bIt = new BlockIterator((LivingEntity) (Player) sender, 10);
					//Loop through every block they're looking at upto 10 blocks away
					while(bIt.hasNext()){
						Block b = bIt.next();
						Shop shop = plugin.getShopManager().getShop(b.getLocation());
						
						if(shop != null && shop.getOwner().equalsIgnoreCase(((Player) sender).getName())){
							//Update the shop
							shop.setPrice(price);
							shop.setSignText();
							shop.update();
							sender.sendMessage(plugin.getMessage("price-is-now", plugin.getEcon().format(shop.getPrice())));
							return true;
						}
					}
					sender.sendMessage(plugin.getMessage("not-looking-at-shop"));
					return true;
				}
				sender.sendMessage(plugin.getMessage("no-permission"));
				return true;
			}
			
			else if(subArg.equals("clean")){
				if(sender.hasPermission("quickshop.clean")){
					sender.sendMessage(plugin.getMessage("command.cleaning"));
					int i = 0;
					List<Shop> toRemove = new ArrayList<Shop>(10);
					for(Entry<String, HashMap<ShopChunk, HashMap<Location, Shop>>> worlds : plugin.getShopManager().getShops().entrySet()){
						if(Bukkit.getWorld(worlds.getKey()) == null) continue;
						for(HashMap<Location, Shop> inChunk : worlds.getValue().values()){
							for(Shop shop : inChunk.values()){
								if(shop.getLocation().getWorld() != null && shop.getLocation().getChunk().isLoaded() && shop.isSelling() && shop.getRemainingStock() == 0){
									shop.delete(false);
									toRemove.add(shop);
									i++;
								}
							}
						}
					}
					for(Shop shop : toRemove){
						plugin.getShopManager().removeShop(shop);
					}
					sender.sendMessage(plugin.getMessage("command.cleaned", ""+i));
					return true;
				}
				sender.sendMessage(plugin.getMessage("no-permission"));
				return true;
			}
			else if(subArg.equals("debug")){
				if(sender.hasPermission("quickshop.debug")){
					plugin.debug = !plugin.debug;
					sender.sendMessage(ChatColor.RED + "[QuickShop] Debug is now " + plugin.debug + ". Pfft - As if there's bugs.");
					return true;
				}
				sender.sendMessage(plugin.getMessage("no-permission"));
				return true;
			}
			
			else if(subArg.equals("info")){
				if(sender.hasPermission("quickshop.info")){
					Player p = (Player) sender;
					Chunk c = p.getLocation().getChunk();
					
					for(Shop shop : plugin.getShopManager().getShops(c).values()){
						String reply = "";
						
						Location loc = shop.getLocation();
						reply += ChatColor.GREEN + shop.getDataName() + " at " + loc.getX() + "," + loc.getY() + "," + loc.getZ();
						
						p.sendMessage(reply);
					}
					
					return true;
				}
				sender.sendMessage(plugin.getMessage("no-permission"));
				return true;
			}
		}
		sendHelp(sender);
		return true;
	}
	
	public void sendHelp(CommandSender s){
		s.sendMessage(plugin.getMessage("command.description.title"));
		if(s.hasPermission("quickshop.unlimited")) s.sendMessage(ChatColor.GREEN + "/qs unlimited" + ChatColor.YELLOW + " - "+plugin.getMessage("command.description.unlimited"));
		if(s.hasPermission("quickshop.setowner")) s.sendMessage(ChatColor.GREEN + "/qs setowner <player>" + ChatColor.YELLOW + " - "+plugin.getMessage("command.description.setowner"));
		if(s.hasPermission("quickshop.create.buy")) s.sendMessage(ChatColor.GREEN + "/qs buy" + ChatColor.YELLOW + " - "+plugin.getMessage("command.description.buy"));
		if(s.hasPermission("quickshop.create.sell")) s.sendMessage(ChatColor.GREEN + "/qs sell" + ChatColor.YELLOW + " - "+plugin.getMessage("command.description.sell"));
		if(s.hasPermission("quickshop.create.changeprice")) s.sendMessage(ChatColor.GREEN + "/qs price" + ChatColor.YELLOW + " - "+plugin.getMessage("command.description.price"));
		if(s.hasPermission("quickshop.clean")) s.sendMessage(ChatColor.GREEN + "/qs clean" + ChatColor.YELLOW + " - "+plugin.getMessage("command.description.clean"));
		
	}
}