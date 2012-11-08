package org.maxgamer.QuickShop;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.maxgamer.QuickShop.Shop.Info;
import org.maxgamer.QuickShop.Shop.Shop;
import org.maxgamer.QuickShop.Shop.ShopAction;
import org.maxgamer.QuickShop.Shop.ShopChunk;

public class ShopManager{
	private QuickShop plugin;
	
	private HashMap<String, HashMap<ShopChunk, HashMap<Location, Shop>>> shops = new HashMap<String, HashMap<ShopChunk, HashMap<Location, Shop>>>(3);
	
	public ShopManager(QuickShop plugin){
		this.plugin = plugin;
	}
	
	/**
	 * Returns a hashmap of World -> Chunk -> Shop
	 * @return a hashmap of World -> Chunk -> Shop
	 */
	public HashMap<String, HashMap<ShopChunk, HashMap<Location, Shop>>> getShops(){
		return this.shops;
	}
	/**
	 * Returns a hashmap of Chunk -> Shop
	 * @param world The name of the world (case sensitive) to get the list of shops from
	 * @return a hashmap of Chunk -> Shop
	 */
	public HashMap<ShopChunk, HashMap<Location, Shop>> getShops(String world){
		return this.shops.get(world);
	}
	/**
	 * Returns a hashmap of Shops
	 * @param c The chunk to search. Referencing doesn't matter, only coordinates and world are used.
	 * @return
	 */
	public HashMap<Location, Shop> getShops(Chunk c){
		return getShops(c.getWorld().getName(), c.getX(), c.getZ());
	}
	
	public HashMap<Location, Shop> getShops(String world, int chunkX, int chunkZ){
		HashMap<ShopChunk, HashMap<Location, Shop>> inWorld = this.getShops(world);
		
		if(inWorld == null){
			return null;
		}
		
		ShopChunk shopChunk = new ShopChunk(world, chunkX, chunkZ);
		return inWorld.get(shopChunk);
	}
	
	/**
	 * Gets a shop in a specific location
	 * @param loc The location to get the shop from
	 * @return The shop at that location
	 */
	public Shop getShop(Location loc){
		HashMap<Location, Shop> inChunk = getShops(loc.getChunk());
		if(inChunk == null){
			return null;
		}
		//We can do this because WorldListener updates the world reference so the world in loc is the same as world in inChunk.get(loc)
		return inChunk.get(loc);
	}
	
	/**
	 * Adds a shop to the world.  Does NOT require the chunk or world to be loaded
	 * @param world The name of the world
	 * @param shop The shop to add
	 */
	public void addShop(String world, Shop shop){
		HashMap<ShopChunk, HashMap<Location, Shop>> inWorld = this.getShops().get(world);
		
		//There's no world storage yet. We need to create that hashmap.
		if(inWorld == null){
			inWorld = new HashMap<ShopChunk, HashMap<Location, Shop>>(3);
			//Put it in the data universe
			this.getShops().put(world, inWorld);
		}
		
		//Calculate the chunks coordinates.  These are 1,2,3 for each chunk, NOT location rounded to the nearest 16.
		int x = (int) Math.floor((shop.getLocation().getBlockX()) / 16.0);
		int z = (int) Math.floor((shop.getLocation().getBlockZ()) / 16.0);
		
		//Get the chunk set from the world info
		ShopChunk shopChunk = new ShopChunk(world, x, z);
		HashMap<Location, Shop> inChunk = inWorld.get(shopChunk);
		
		//That chunk data hasn't been created yet - Create it!
		if(inChunk == null){
			inChunk = new HashMap<Location, Shop>(1);
			//Put it in the world
			inWorld.put(shopChunk, inChunk);
		}
		
		//Put the shop in its location in the chunk list.
		inChunk.put(shop.getLocation(), shop);
	}
	
	/**
	 * Removes a shop from the world. Does NOT remove it from the database. 
	 * * REQUIRES * the world to be loaded 
	 * @param shop The shop to remove
	 */
	public void removeShop(Shop shop){
		Location loc = shop.getLocation();
		String world = loc.getWorld().getName();
		HashMap<ShopChunk, HashMap<Location, Shop>> inWorld = this.getShops().get(world);
		
		int x = (int) Math.floor((shop.getLocation().getBlockX()) / 16.0);
		int z = (int) Math.floor((shop.getLocation().getBlockZ()) / 16.0);
		
		ShopChunk shopChunk = new ShopChunk(world, x, z);
		HashMap<Location, Shop> inChunk = inWorld.get(shopChunk);
		
		inChunk.remove(loc);
	}
	
	/**
	 * Removes all shops from memory and the world. Does not delete them from the database.
	 * Call this on plugin disable ONLY.
	 */
	public void clear(){
		if(plugin.display){
			for(HashMap<ShopChunk, HashMap<Location, Shop>> inWorld : this.getShops().values()){
				for(HashMap<Location, Shop> inChunk : inWorld.values()){
					for(Shop shop : inChunk.values()){
						shop.getDisplayItem().removeDupe();
						shop.getDisplayItem().remove();
					}
				}
			}
		}
		
		this.shops.clear();
	}
	
	public void handleChat(final Player p, final String message){
		//Use from the main thread, because Bukkit hates life
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable(){
			@Override
			public void run() {
				HashMap<String, Info> actions = plugin.getActions();
				//They wanted to do something.
				Info info = actions.get(p.getName());
				plugin.getActions().remove(p.getName());
				if(info == null) return;
				/*
				 * Creation handling
				 */
				if(info.getAction() == ShopAction.CREATE){
					try{
						if(plugin.getShopManager().getShop(info.getLocation()) != null){
							p.sendMessage(plugin.getMessage("shop-already-owned"));
							return;
						}
						
						if(plugin.getChestNextTo(info.getLocation().getBlock()) != null && !p.hasPermission("quickshop.create.double")){
							p.sendMessage(plugin.getMessage("no-double-chests"));
							return;
						}
						
						if(info.getLocation().getBlock().getType() != Material.CHEST){
							p.sendMessage(plugin.getMessage("chest-was-removed"));
							return;
						}
						
						//Price per item
						double price = Double.parseDouble(message);
						if(price < 0.01){
							p.sendMessage(plugin.getMessage("price-too-cheap"));
							return;
						}
						double tax = plugin.getConfig().getDouble("shop.cost"); 
						
						if(tax != 0 && plugin.getEcon().getBalance(p.getName()) <= tax){
							p.sendMessage(plugin.getMessage("you-cant-afford-a-new-shop", plugin.format(tax)));
							return;
						}
						
						//Add the shop to the list.
						Shop shop = new Shop(info.getLocation(), price, info.getItem(), p.getName());

						plugin.getShopManager().addShop(shop.getLocation().getWorld().getName(), shop);
						
						if(tax == 0) p.sendMessage(plugin.getMessage("success-created-shop"));
						else{
							plugin.getEcon().withdrawPlayer(p.getName(), tax);
							plugin.getEcon().depositPlayer(plugin.getConfig().getString("tax-account"), tax);
						}
						
						Location loc = shop.getLocation();
						plugin.log(p.getName() + " created a "+shop.getDataName()+" shop at ("+loc.getWorld().getName()+" - "+loc.getX()+","+loc.getY()+","+loc.getZ()+")");
						
						//Writes the shop to the database
						shop.update(true);
						
						if(!plugin.getConfig().getBoolean("shop.lock")){
							//Warn them if they haven't been warned since reboot
							if(!plugin.warnings.contains(p.getName())){
								p.sendMessage(plugin.getMessage("shops-arent-locked"));
								plugin.warnings.add(p.getName());
							}
						}
						
						//Figures out which way we should put the sign on and sets its text.
						if(info.getSignBlock() != null && info.getSignBlock().getType() == Material.AIR && plugin.getConfig().getBoolean("shop.auto-sign")){
							BlockState bs = info.getSignBlock().getState();
							BlockFace bf = info.getLocation().getBlock().getFace(info.getSignBlock());
							bs.setType(Material.WALL_SIGN);
							
							if(bf == BlockFace.NORTH){
								bs.setRawData((byte)4);
							}
							else if(bf == BlockFace.SOUTH){
								bs.setRawData((byte) 5);
							}
							else if(bf == BlockFace.EAST){
								bs.setRawData((byte) 2);
							}
							else if(bf == BlockFace.WEST){
								bs.setRawData((byte) 3);
							}
							
							bs.update(true);
							
							shop.setSignText();
						}
					}
					/* They didn't enter a number. */
					catch(NumberFormatException ex){
						p.sendMessage(plugin.getMessage("shop-creation-cancelled"));
						return;
					}
				}
				/* Purchase Handling */
				else if(info.getAction() == ShopAction.BUY){
					int amount = 0;
					try{
						amount = Integer.parseInt(message);
					}
					catch(NumberFormatException e){
						p.sendMessage(plugin.getMessage("shop-purchase-cancelled"));
						return;
					}
					
					//Get the shop they interacted with
					Shop shop = plugin.getShopManager().getShop(info.getLocation());
					
					//It's not valid anymore
					if(shop == null || info.getLocation().getBlock().getType() != Material.CHEST){
						p.sendMessage(plugin.getMessage("chest-was-removed"));
						return;
					}
					
					if(info.hasChanged(shop)){
						p.sendMessage(plugin.getMessage("shop-has-changed"));
						return;
					}
					
					if(shop.isSelling()){
						int stock = shop.getRemainingStock();
						
						if(stock <  amount){
							p.sendMessage(plugin.getMessage("shop-stock-too-low", ""+shop.getRemainingStock(), shop.getDataName()));
							return;
						}
						if(amount == 0){
							//Dumb.
							sendPurchaseSuccess(p, shop, amount);
							return; 
						}
						else if(amount < 0){
							// & Dumber
							p.sendMessage(plugin.getMessage("negative-amount"));
							return;
						}
						
						//Money handling
						if(!p.getName().equalsIgnoreCase(shop.getOwner())){
							//Check their balance.  Works with *most* economy plugins*
							if(!plugin.getEcon().has(p.getName(), amount * shop.getPrice())){
								p.sendMessage(plugin.getMessage("you-cant-afford-to-buy", ""+amount * shop.getPrice(), ""+plugin.getEcon().getBalance(p.getName())));
								return;
							}
							
							//Don't tax them if they're purchasing from themselves.
							//Do charge an amount of tax though.
							double tax = plugin.getConfig().getDouble("tax");
							double total = amount * shop.getPrice();
							
							EconomyResponse r = plugin.getEcon().withdrawPlayer(p.getName(), total);
							if(!r.transactionSuccess()){
								p.sendMessage(plugin.getMessage("you-cant-afford-to-buy", ""+amount * shop.getPrice(), ""+plugin.getEcon().getBalance(p.getName())));
								return;
							}
							
							if(!shop.isUnlimited() || plugin.getConfig().getBoolean("shop.pay-unlimited-shop-owners")){
								plugin.getEcon().depositPlayer(shop.getOwner(), total * (1 - tax));
								
								if(tax != 0){
									plugin.getEcon().depositPlayer(plugin.getConfig().getString("tax-account"), total * tax);
								}
							}
							
							//Notify the shop owner
							Player owner = Bukkit.getPlayerExact(shop.getOwner());
							
							String msg = plugin.getMessage("player-bought-from-your-store", p.getName(), ""+amount, shop.getDataName());
							
							if(stock == amount) msg += "\n" + plugin.getMessage("shop-out-of-stock", ""+shop.getLocation().getBlockX(), ""+shop.getLocation().getBlockY(), ""+shop.getLocation().getBlockZ(), shop.getDataName());
							
							if(owner != null && owner.isOnline()){
								owner.sendMessage(msg);
							}
							else{
								plugin.addMessage(shop.getOwner(), msg);
							}
						}
						//Transfers the item from A to B
						shop.sell(p, amount);
						sendPurchaseSuccess(p, shop, amount);
						Location loc = shop.getLocation();
						plugin.log(p.getName() + " bought " + amount + " " +shop.getDataName()+" from shop at ("+loc.getWorld().getName()+" - "+loc.getX()+","+loc.getY()+","+loc.getZ()+") for " + (shop.getPrice() * amount));
					}
					else if(shop.isBuying()){
						int space = shop.getRemainingSpace(shop.getMaterial().getMaxStackSize());
						
						if(space <  amount){
							p.sendMessage(plugin.getMessage("shop-has-no-space", ""+space, shop.getDataName()));
							return;
						}
						
						int count = 0;
						for(ItemStack item : p.getInventory().getContents()){
							if(shop.matches(item)){
								count += item.getAmount();
							}
						}
						
						//Broke
						if(amount > count){
							p.sendMessage(plugin.getMessage("you-dont-have-that-many-items", ""+count, shop.getDataName()));
							return;
						}
						
						if(amount == 0){
							//Dumb.
							sendPurchaseSuccess(p, shop, amount);
							return; 
						}
						else if(amount < 0){
							// & Dumber
							p.sendMessage(plugin.getMessage("negative-amount"));
							return;
						}
						
						//Money handling
						if(!p.getName().equalsIgnoreCase(shop.getOwner())){
							//Don't tax them if they're purchasing from themselves.
							//Do charge an amount of tax though.
							double tax = plugin.getConfig().getDouble("tax");
							double total = amount * shop.getPrice();
							
							if(!shop.isUnlimited() || plugin.getConfig().getBoolean("shop.pay-unlimited-shop-owners")){
								//Tries to check their balance nicely to see if they can afford it.
								if(!plugin.getEcon().has(shop.getOwner(), amount * shop.getPrice())){
									p.sendMessage(plugin.getMessage("the-owner-cant-afford-to-buy-from-you", plugin.format(amount * shop.getPrice()), plugin.format(plugin.getEcon().getBalance(shop.getOwner()))));
									return;
								}
								
								EconomyResponse r = plugin.getEcon().withdrawPlayer(shop.getOwner(), total);
								
								//Check for plugins faking econ.has(amount)
								if(!r.transactionSuccess()){
									p.sendMessage(plugin.getMessage("the-owner-cant-afford-to-buy-from-you", plugin.format(amount * shop.getPrice()), plugin.format(plugin.getEcon().getBalance(shop.getOwner()))));
									return;
								}
								
								if(tax != 0){
									plugin.getEcon().depositPlayer(plugin.getConfig().getString("tax-account"), total * tax);
								}
							}
							//Give them the money after we know we succeeded
							plugin.getEcon().depositPlayer(p.getName(), total * (1 - tax));
							
							//Notify the owner of the purchase.
							Player owner = Bukkit.getPlayerExact(shop.getOwner());
							String msg = plugin.getMessage("player-sold-to-your-store", p.getName(), ""+amount, shop.getDataName());
							
							if(space == amount) msg += "\n" + plugin.getMessage("shop-out-of-space", ""+shop.getLocation().getBlockX(), ""+shop.getLocation().getBlockY(), ""+shop.getLocation().getBlockZ());
							
							if(owner != null && owner.isOnline()){
								owner.sendMessage(msg);
							}
							else{
								plugin.addMessage(shop.getOwner(), msg);
							}
						}
						
						shop.buy(p, amount);
						sendSellSuccess(p, shop, amount);
						
						Location loc = shop.getLocation();
						plugin.log(p.getName() + " sold " + amount + " " +shop.getDataName()+" to shop at ("+loc.getWorld().getName()+" - "+loc.getX()+","+loc.getY()+","+loc.getZ()+") for " + (shop.getPrice() * amount));
					}
				}
				/*
				 * If it was already cancelled (from destroyed)
				 */
				else{
					return; //It was cancelled, go away.
				}
			}
		});
	}
	private void sendPurchaseSuccess(Player p, Shop shop, int amount){
		p.sendMessage(ChatColor.DARK_PURPLE + "+---------------------------------------------------+");
		p.sendMessage(ChatColor.DARK_PURPLE + "| " + plugin.getMessage("menu.successful-purchase"));
		
		p.sendMessage(ChatColor.DARK_PURPLE + "| " + plugin.getMessage("menu.item-name-and-price", ""+amount, shop.getDataName(), plugin.format((amount * shop.getPrice()))));
		

		Map<Enchantment, Integer> enchs = shop.getEnchants();
		if(enchs != null && enchs.size() > 0){
			p.sendMessage(ChatColor.DARK_PURPLE + "+--------------------"+plugin.getMessage("menu.enchants")+"-----------------------+");
			for(Entry<Enchantment, Integer> entries : enchs.entrySet()){
				p.sendMessage(ChatColor.DARK_PURPLE + "| " + ChatColor.YELLOW + entries.getKey() .getName() + " " + entries.getValue() );
			}
		}
		p.sendMessage(ChatColor.DARK_PURPLE + "+---------------------------------------------------+");
	}
	
	private void sendSellSuccess(Player p, Shop shop, int amount){
		p.sendMessage(ChatColor.DARK_PURPLE + "+---------------------------------------------------+");
		p.sendMessage(ChatColor.DARK_PURPLE + "| " + plugin.getMessage("menu.successfully-sold"));
		p.sendMessage(ChatColor.DARK_PURPLE + "| " + plugin.getMessage("menu.item-name-and-price", ""+amount, shop.getDataName(), plugin.format((amount * shop.getPrice()))));

		Map<Enchantment, Integer> enchs = shop.getEnchants();
		if(enchs != null && enchs.size() > 0){
			p.sendMessage(ChatColor.DARK_PURPLE + "+--------------------"+plugin.getMessage("menu.enchants")+"-----------------------+");
			for(Entry<Enchantment, Integer> entries : enchs.entrySet()){
				p.sendMessage(ChatColor.DARK_PURPLE + "| " + ChatColor.YELLOW + entries.getKey() .getName() + " " + entries.getValue() );
			}
		}
		p.sendMessage(ChatColor.DARK_PURPLE + "+---------------------------------------------------+");
	}
}