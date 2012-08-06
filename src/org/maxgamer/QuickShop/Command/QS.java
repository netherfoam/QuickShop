package org.maxgamer.QuickShop.Command;

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

public class QS implements CommandExecutor{
	QuickShop plugin;
	public QS(QuickShop plugin){
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
		if(args.length > 0){
			if(args[0].equalsIgnoreCase("unlimited")){
				if(sender instanceof Player && sender.hasPermission("quickshop.unlimited")){
					BlockIterator bIt = new BlockIterator((LivingEntity) (Player) sender, 10);
					while(bIt.hasNext()){
						Block b = bIt.next();
						Shop shop = plugin.getShop(b.getLocation());
						if(shop != null){
							shop.setUnlimited(true);
							int x = shop.getLocation().getBlockX();
							int y = shop.getLocation().getBlockY();
							int z = shop.getLocation().getBlockZ();
							String world = shop.getLocation().getWorld().getName();
							plugin.getDB().writeToBuffer("UPDATE shops SET unlimited = '1' WHERE x = "+x+" AND y="+y+" AND z="+z+" AND world='"+world+"'");
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
			else if(args[0].equalsIgnoreCase("setowner")){
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
							
							int x = shop.getLocation().getBlockX();
							int y = shop.getLocation().getBlockY();
							int z = shop.getLocation().getBlockZ();
							String world = shop.getLocation().getWorld().getName();
							
							plugin.getDB().writeToBuffer("UPDATE shops SET owner = '"+args[1]+"' WHERE x = "+x+" AND y="+y+" AND z="+z+" AND world='"+world+"'");
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
		}
		return false;
	}
	
}