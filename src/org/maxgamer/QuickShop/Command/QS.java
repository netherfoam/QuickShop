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
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
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
	
	private void setUnlimited(CommandSender sender){
		if(sender instanceof Player && sender.hasPermission("quickshop.unlimited")){
			BlockIterator bIt = new BlockIterator((LivingEntity) (Player) sender, 10);
			while(bIt.hasNext()){
				Block b = bIt.next();
				Shop shop = plugin.getShopManager().getShop(b.getLocation());
				if(shop != null){
					shop.setUnlimited(true);
					shop.update();
					sender.sendMessage(plugin.getMessage("command.success-created-unlimited"));
					return;
				}
			}
			sender.sendMessage(plugin.getMessage("not-looking-at-shop"));
			return;
		}
		else{
			sender.sendMessage(plugin.getMessage("no-permission"));
			return;
		}
	}
	
	private void setOwner(CommandSender sender, String[] args){
		if(sender instanceof Player && sender.hasPermission("quickshop.setowner")){
			if(args.length < 2){
				sender.sendMessage(plugin.getMessage("command.no-owner-given"));
				return;
			}
			BlockIterator bIt = new BlockIterator((LivingEntity) (Player) sender, 10);
			while(bIt.hasNext()){
				Block b = bIt.next();
				Shop shop = plugin.getShopManager().getShop(b.getLocation());
				if(shop != null){
					shop.setOwner(args[1]);
					shop.update();
					
					sender.sendMessage(ChatColor.GREEN + "New Owner: " + shop.getOwner());
					return;
				}
			}
			sender.sendMessage(plugin.getMessage("not-looking-at-shop"));
			return;
		}
		else{
			sender.sendMessage(plugin.getMessage("no-permission"));
			return;
		}
	}
	
	private void find(CommandSender sender, String[] args){
		if(sender instanceof Player && sender.hasPermission("quickshop.find")){
			if(args.length < 2){
				sender.sendMessage(plugin.getMessage("command.no-type-given"));
				return;
			}
			String lookFor = "";
			for(int i = 1; i < args.length; i++){
				lookFor += "_" + args[i];
			}
			lookFor = lookFor.toUpperCase();
			Player p = (Player) sender;
			Location loc = p.getLocation().clone().add(0, 1.62, 0);
			
			//double minDistanceSquared = plugin.getConfig().getInt("shop.find-distance") * plugin.getConfig().getInt("shop-find);
			double minDistance = plugin.getConfig().getInt("shop.find-distance");
			double minDistanceSquared = minDistance * minDistance;
			int chunkRadius = (int) minDistance/16 + 1;
			Shop closest = null;
			
			Chunk c = loc.getChunk();
			
			for(int x = -chunkRadius + c.getX(); x < chunkRadius + c.getX(); x++){
				for(int z = -chunkRadius + c.getZ(); z < chunkRadius + c.getZ(); z++){
					Chunk d = c.getWorld().getChunkAt(x, z);
					HashMap<Location, Shop> inChunk = plugin.getShopManager().getShops(d);
					if(inChunk == null) continue;
					for(Shop shop : inChunk.values()){
						if(shop.getDataName().contains(lookFor) && shop.getLocation().distanceSquared(loc) < minDistanceSquared){
							closest = shop;
							minDistanceSquared = shop.getLocation().distanceSquared(loc);
						}
					}
				}
			}
			if(closest == null){
				sender.sendMessage(plugin.getMessage("no-nearby-shop", args[1]));
				return;
			}
			Location lookat = closest.getLocation().clone().add(0.5, 0.5, 0.5);
			
			p.teleport(this.lookAt(loc, lookat).add(0, -1.62, 0), TeleportCause.COMMAND);
			p.sendMessage(plugin.getMessage("nearby-shop-this-way", ""+(int) Math.floor(Math.sqrt(minDistanceSquared))));
			
			return;
		}
		else{
			sender.sendMessage(plugin.getMessage("no-permission"));
			return;
		}
	}
	
	private void setBuy(CommandSender sender){
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
					return;
				}
			}
			sender.sendMessage(plugin.getMessage("not-looking-at-shop"));
			return;
		}
		sender.sendMessage(plugin.getMessage("no-permission"));
		return;
	}
	
	private void setSell(CommandSender sender){
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
					return;
				}
			}
			sender.sendMessage(plugin.getMessage("not-looking-at-shop"));
			return;
		}
		sender.sendMessage(plugin.getMessage("no-permission"));
		return;
	}
	
	private void setPrice(CommandSender sender, String[] args){
		if(sender instanceof Player && sender.hasPermission("quickshop.create.changeprice")){
			if(args.length < 2){
				sender.sendMessage(plugin.getMessage("no-price-given"));
				return;
			}
			double price;
			try{
				price = Double.parseDouble(args[1]);
			}
			catch(NumberFormatException e){
				sender.sendMessage(plugin.getMessage("thats-not-a-number"));
				return;
			}
			
			if(price < 0.01){
				sender.sendMessage(plugin.getMessage("price-too-cheap"));
				return;
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
					
					if(shop.isDoubleShop()){
						Shop nextTo = shop.getAttachedShop();
						
						if(shop.isSelling()){
							if(shop.getPrice() < nextTo.getPrice()){
								sender.sendMessage(plugin.getMessage("buying-more-than-selling"));
							}
						}
						else{
							//Buying
							if(shop.getPrice() > nextTo.getPrice()){
								sender.sendMessage(plugin.getMessage("buying-more-than-selling"));
							}
						}
					}
					
					return;
				}
			}
			sender.sendMessage(plugin.getMessage("not-looking-at-shop"));
			return;
		}
		sender.sendMessage(plugin.getMessage("no-permission"));
		return;
	}
	
	private void clean(CommandSender sender){
		if(sender.hasPermission("quickshop.clean")){
			sender.sendMessage(plugin.getMessage("command.cleaning"));
			int i = 0;
			List<Shop> toRemove = new ArrayList<Shop>(10);
			for(Entry<String, HashMap<ShopChunk, HashMap<Location, Shop>>> worlds : plugin.getShopManager().getShops().entrySet()){
				if(Bukkit.getWorld(worlds.getKey()) == null) continue;
				for(HashMap<Location, Shop> inChunk : worlds.getValue().values()){
					for(Shop shop : inChunk.values()){
						if(shop.getLocation().getWorld() != null && shop.isSelling() && shop.getRemainingStock() == 0){
							if(shop.isDoubleShop()) continue; //Dont delete double shops
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
			return;
		}
		sender.sendMessage(plugin.getMessage("no-permission"));
		return;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
		if(args.length > 0){
			String subArg = args[0].toLowerCase();
			
			if(subArg.equals("unlimited")){
				setUnlimited(sender);
				return true;
			}
			else if(subArg.equals("setowner")){
				setOwner(sender, args);
			}
			else if(subArg.equals("find")){
				find(sender, args);
			}
			else if(subArg.startsWith("buy")){
				setBuy(sender);
			}
			else if(subArg.startsWith("sell")){
				setSell(sender);
			}
			
			else if(subArg.startsWith("price")){
				setPrice(sender, args);
			}
			
			else if(subArg.equals("clean")){
				clean(sender);
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
					int buying, selling, doubles, chunks, worlds;
					buying = selling = doubles = chunks = worlds = 0;
					
					int nostock = 0;
					
					for(HashMap<ShopChunk, HashMap<Location, Shop>> inWorld : plugin.getShopManager().getShops().values()){
						worlds++;
						for(HashMap<Location, Shop> inChunk : inWorld.values()){
							chunks++;
							for(Shop shop : inChunk.values()){
								if(shop.isBuying()){
									buying++;
								}
								else if(shop.isSelling()){
									selling++;
								}
								
								if(shop.isDoubleShop()){
									doubles++;
								}
								
								if(shop.isSelling() && !shop.isDoubleShop() && shop.getRemainingStock() == 0){
									nostock++;
								}
							}
						}
					}
					
					sender.sendMessage(ChatColor.RED + "QuickShop Statistics...");
					sender.sendMessage(ChatColor.GREEN + "" + (buying + selling) + " shops in " + chunks + " chunks spread over " + worlds + " worlds.");
					sender.sendMessage(ChatColor.GREEN + "" + doubles + " double shops. ");
					sender.sendMessage(ChatColor.GREEN + "" + nostock + " selling shops (excluding doubles) which will be removed by /qs clean.");
					
					
					return true;
				}
				sender.sendMessage(plugin.getMessage("no-permission"));
				return true;
			}
		}
		else{
			//Invalid arg given
			sendHelp(sender);
			return true;
		}
		//No args given
		sendHelp(sender);
		return true;
	}
	
	/**
	 * Returns loc with modified pitch/yaw angles so it faces lookat
	 * @param loc The location a players head is
	 * @param lookat The location they should be looking
	 * @return The location the player should be facing to have their crosshairs on the location lookAt
	 * Kudos to bergerkiller for most of this function
	 */
	public Location lookAt(Location loc, Location lookat) {
        //Clone the loc to prevent applied changes to the input loc
        loc = loc.clone();

        // Values of change in distance (make it relative)
        double dx = lookat.getX() - loc.getX();
        double dy = lookat.getY() - loc.getY();
        double dz = lookat.getZ() - loc.getZ();

        // Set yaw
        if (dx != 0) {
            // Set yaw start value based on dx
            if (dx < 0) {
                loc.setYaw((float) (1.5 * Math.PI));
            } else {
                loc.setYaw((float) (0.5 * Math.PI));
            }
            loc.setYaw((float) loc.getYaw() - (float) Math.atan(dz / dx));
        } else if (dz < 0) {
            loc.setYaw((float) Math.PI);
        }

        // Get the distance from dx/dz
        double dxz = Math.sqrt(Math.pow(dx, 2) + Math.pow(dz, 2));

        float pitch = (float) -Math.atan(dy/dxz);

        // Set values, convert to degrees
        // Minecraft yaw (vertical) angles are inverted (negative)
        loc.setYaw(-loc.getYaw() * 180f / (float) Math.PI + 360);
        // But pitch angles are normal
        loc.setPitch(pitch * 180f / (float) Math.PI);
        
        return loc;
    }
	
	public void sendHelp(CommandSender s){
		s.sendMessage(plugin.getMessage("command.description.title"));
		if(s.hasPermission("quickshop.unlimited")) s.sendMessage(ChatColor.GREEN + "/qs unlimited" + ChatColor.YELLOW + " - "+plugin.getMessage("command.description.unlimited"));
		if(s.hasPermission("quickshop.setowner")) s.sendMessage(ChatColor.GREEN + "/qs setowner <player>" + ChatColor.YELLOW + " - "+plugin.getMessage("command.description.setowner"));
		if(s.hasPermission("quickshop.create.buy")) s.sendMessage(ChatColor.GREEN + "/qs buy" + ChatColor.YELLOW + " - "+plugin.getMessage("command.description.buy"));
		if(s.hasPermission("quickshop.create.sell")) s.sendMessage(ChatColor.GREEN + "/qs sell" + ChatColor.YELLOW + " - "+plugin.getMessage("command.description.sell"));
		if(s.hasPermission("quickshop.create.changeprice")) s.sendMessage(ChatColor.GREEN + "/qs price" + ChatColor.YELLOW + " - "+plugin.getMessage("command.description.price"));
		if(s.hasPermission("quickshop.clean")) s.sendMessage(ChatColor.GREEN + "/qs clean" + ChatColor.YELLOW + " - "+plugin.getMessage("command.description.clean"));
		if(s.hasPermission("quickshop.find")) s.sendMessage(ChatColor.GREEN + "/qs find <item>" + ChatColor.YELLOW + " - "+plugin.getMessage("command.description.find"));
		
	}
}