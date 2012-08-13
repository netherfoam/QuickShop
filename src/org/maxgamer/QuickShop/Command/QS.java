package org.maxgamer.QuickShop.Command;

import java.util.List;

import org.bukkit.ChatColor;
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
						Shop shop = plugin.getShop(b.getLocation());
						if(shop != null){
							shop.setUnlimited(true);
							shop.update();
							sender.sendMessage(ChatColor.GREEN + "Unlimited QuickShop created.");
							return true;
						}
					}
					sender.sendMessage(ChatColor.RED + "No QuickShop found.  You must be looking at one.");
					return true;
				}
				else{
					sender.sendMessage(ChatColor.RED + "You cannot do that.");
					return true;
				}
			}
			else if(subArg.equals("setowner")){
				if(sender instanceof Player && sender.hasPermission("quickshop.setowner")){
					if(args.length < 2){
						sender.sendMessage(ChatColor.RED + "No owner given.  /qs setowner <player>");
						return true;
					}
					BlockIterator bIt = new BlockIterator((LivingEntity) (Player) sender, 10);
					while(bIt.hasNext()){
						Block b = bIt.next();
						Shop shop = plugin.getShop(b.getLocation());
						if(shop != null){
							shop.setOwner(args[1]);
							shop.update();
							
							sender.sendMessage(ChatColor.GREEN + "New Owner: " + shop.getOwner());
							return true;
						}
					}
				}
				else{
					sender.sendMessage(ChatColor.RED + "You cannot do that.");
					return true;
				}
			}
			
			else if(subArg.startsWith("buy")){
				if(sender instanceof Player && sender.hasPermission("quickshop.create.buy")){
					BlockIterator bIt = new BlockIterator((LivingEntity) (Player) sender, 10);
					while(bIt.hasNext()){
						Block b = bIt.next();
						Shop shop = plugin.getShop(b.getLocation());
						if(shop != null && shop.getOwner().equalsIgnoreCase(((Player) sender).getName())){
							shop.setShopType(ShopType.BUYING);
							shop.setSignText();
							shop.update();
							
							sender.sendMessage(ChatColor.GREEN + "Now "+ChatColor.LIGHT_PURPLE + "BUYING" + ChatColor.GREEN+": " + shop.getDataName());
							return true;
						}
					}
					sender.sendMessage(ChatColor.RED + "No QuickShop found.  You must be looking at one.");
					return true;
				}
				sender.sendMessage(ChatColor.RED + "You cannot do that.");
				return true;
			}
			
			else if(subArg.startsWith("sell")){
				if(sender instanceof Player && sender.hasPermission("quickshop.create.sell")){
					BlockIterator bIt = new BlockIterator((LivingEntity) (Player) sender, 10);
					while(bIt.hasNext()){
						Block b = bIt.next();
						Shop shop = plugin.getShop(b.getLocation());
						if(shop != null && shop.getOwner().equalsIgnoreCase(((Player) sender).getName())){
							shop.setShopType(ShopType.SELLING);
							shop.setSignText();
							shop.update();
							
							sender.sendMessage(ChatColor.GREEN + "Now "+ChatColor.AQUA + "SELLING" + ChatColor.GREEN+": " + shop.getDataName());
							return true;
						}
					}
					sender.sendMessage(ChatColor.RED + "No QuickShop found.  You must be looking at one.");
					return true;
				}
				sender.sendMessage(ChatColor.RED + "You cannot do that.");
				return true;

			}/*
			else if(subArg.startsWith("debug")){
				for(List<Shop> shops : plugin.shopChunks.values()){
					plugin.getLogger().info("Size: " + shops.size());
				}
			}*/
		}
		sendHelp(sender);
		return true;
	}
	
	
	public void sendHelp(CommandSender s){
		s.sendMessage(ChatColor.GREEN + "QuickShop Help");
		if(s.hasPermission("quickshop.unlimited")) s.sendMessage(ChatColor.GREEN + "/qs unlimited" + ChatColor.YELLOW + " - Makes a shop unlimited");
		if(s.hasPermission("quickshop.setowner")) s.sendMessage(ChatColor.GREEN + "/qs setowner <player>" + ChatColor.YELLOW + " - Sets the owner of a shop");
		if(s.hasPermission("quickshop.create.buy")) s.sendMessage(ChatColor.GREEN + "/qs buy" + ChatColor.YELLOW + " - Changes a shop to BUY mode");
		if(s.hasPermission("quickshop.create.sell")) s.sendMessage(ChatColor.GREEN + "/qs sell" + ChatColor.YELLOW + " - Changes a shop to SELL mode");
	}
}