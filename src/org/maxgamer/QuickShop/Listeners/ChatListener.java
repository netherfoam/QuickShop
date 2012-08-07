package org.maxgamer.QuickShop.Listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.maxgamer.QuickShop.QuickShop;
import org.maxgamer.QuickShop.Shop.Info;
import org.maxgamer.QuickShop.Shop.Shop;
import org.maxgamer.QuickShop.Shop.ShopAction;

/**
 * 
 * @author Netherfoam
 *
 */
public class ChatListener implements Listener{
	QuickShop plugin;
	
	public ChatListener(QuickShop plugin){
		this.plugin = plugin;
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onChat(final AsyncPlayerChatEvent e){
		if(!plugin.getActions().containsKey(e.getPlayer().getName())) return;
		
		//Use from the main thread, because Bukkit hates life
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable(){
			@Override
			public void run() {
				Player p = e.getPlayer();
				HashMap<String, Info> actions = plugin.getActions();
				//They wanted to do something.
				Info info = actions.get(p.getName());
				plugin.getActions().remove(p.getName());
				
				/*
				 * Creation handling
				 */
				if(info.getAction() == ShopAction.CREATE){
					try{
						if(plugin.getShop(info.getLocation()) != null){
							p.sendMessage(ChatColor.RED + "Someone else has claimed that shop.");
							return;
						}
						
						if(plugin.getChestNextTo(info.getLocation().getBlock()) != null){
							p.sendMessage(ChatColor.RED + "Double chest shops are disabled.");
							return;
						}
						
						if(info.getLocation().getBlock().getType() != Material.CHEST){
							p.sendMessage(ChatColor.RED + "That chest was removed.");
							return;
						}
						
						//Price per item
						double price = Double.parseDouble(e.getMessage());
						if(price < 0.01){
							p.sendMessage(ChatColor.RED + "Price must be greater than " + ChatColor.YELLOW + "$0.01");
							return;
						}
						double tax = plugin.getConfig().getDouble("shop.cost"); 
						
						if(tax != 0 && plugin.getEcon().getBalance(p.getName()) <= tax){
							p.sendMessage(ChatColor.RED + "It costs $" + tax + " to create a new shop.");
							return;
						}
						
						//Add the shop to the list.
						final Shop shop = new Shop(info.getLocation(), price, info.getItem(), p.getName());
						plugin.getShops().put(info.getLocation(), shop);
						
						if(tax == 0) p.sendMessage(ChatColor.GREEN + "Created a shop");
						else{
							plugin.getEcon().withdrawPlayer(p.getName(), tax);
							plugin.getEcon().depositPlayer(plugin.getConfig().getString("tax-account"), tax);
						}
						
						//Writes the shop to the database
						shop.update(true);
						
						if(!plugin.getConfig().getBoolean("shop.lock")){
							//Warn them if they haven't been warned since reboot
							if(!plugin.warnings.contains(p.getName())){
								p.sendMessage(ChatColor.DARK_RED + "[QuickShop] " +ChatColor.RED+"Remember, shops are NOT protected from theft! If you want to stop thieves, lock it!");
								plugin.warnings.add(p.getName());
							}
						}
						
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
					/*
					 * They didn't enter a number.
					 */
					catch(NumberFormatException ex){
						p.sendMessage(ChatColor.RED + "Cancelled Shop Creation");
						return;
					}
				}
				/*
				 * Purchase Handling
				 */
				else if(info.getAction() == ShopAction.BUY){
					int amount;
					try{
						amount = Integer.parseInt(e.getMessage());
					}
					catch(NumberFormatException e){
						p.sendMessage(ChatColor.RED + "Cancelled Shop Purchase");
						return;
					}
					
					Shop shop = plugin.getShops().get(info.getLocation());
					
					if(shop == null || info.getLocation().getBlock().getType() != Material.CHEST){
						p.sendMessage(ChatColor.RED + "That shop was removed.");
						return;
					}
					
					if(shop.isSelling()){
						int stock = shop.getRemainingStock();
						
						if(stock <  amount){
							p.sendMessage(ChatColor.RED + "The shop only has " + ChatColor.YELLOW + shop.getRemainingStock() + " " + shop.getMaterial().toString() + ChatColor.RED + " left.");
							return;
						}
						if(!plugin.getEcon().has(p.getName(), amount * shop.getPrice())){
							p.sendMessage(ChatColor.RED + "That costs " + ChatColor.YELLOW + amount * shop.getPrice() + ChatColor.RED + ", but you only have " + ChatColor.YELLOW + plugin.getEcon().getBalance(p.getName()));
							return;
						}
						if(amount == 0){
							//Dumb.
							sendPurchaseSuccess(p, shop, amount);
							return; 
						}
						else if(amount < 0){
							// & Dumber
							p.sendMessage(ChatColor.RED + "Derrrrp, Can't buy negative amounts.");
							return;
						}
						
						//Money handling
						if(!p.getName().equalsIgnoreCase(shop.getOwner())){
							//Don't tax them if they're purchasing from themselves.
							//Do charge an amount of tax though.
							double tax = plugin.getConfig().getDouble("tax");
							double total = amount * shop.getPrice();
							
							plugin.getEcon().withdrawPlayer(p.getName(), total);
							
							if(!shop.isUnlimited() || (shop.isUnlimited() && plugin.getConfig().getBoolean("shop.pay-unlimited-shop-owners"))){
								plugin.getEcon().depositPlayer(shop.getOwner(), total * (1 - tax));
								
								if(tax != 0){
									plugin.getEcon().depositPlayer(plugin.getConfig().getString("tax-account"), total * tax);
								}
							}
							
							Player owner = Bukkit.getPlayerExact(shop.getOwner());
							if(owner != null){
								owner.sendMessage(ChatColor.GREEN + p.getName() + " just purchased " + amount + " " + ChatColor.YELLOW + shop.getDataName() + ChatColor.GREEN + " from your store.");
								if(stock == amount) owner.sendMessage(ChatColor.DARK_PURPLE + "Your shop at " + shop.getLocation().getBlockX() + ", " + shop.getLocation().getBlockY() + ", " + shop.getLocation().getBlockZ() + " has run out of " + shop.getDataName());
							}
						}
						
						shop.sell(p, shop.getItem(), amount);
						sendPurchaseSuccess(p, shop, amount);
					}
					else if(shop.isBuying()){
						int space = shop.getRemainingSpace(shop.getMaterial().getMaxStackSize());
						
						if(space <  amount){
							p.sendMessage(ChatColor.RED + "The shop only has room for " +space+ " more " + shop.getDataName() +".");
							return;
						}
						
						int count = 0;
						for(ItemStack item : p.getInventory().getContents()){
							if(item != null && item.getType() == shop.getMaterial() && item.getDurability() == shop.getDurability() && item.getEnchantments().equals(shop.getEnchants())){
								count += item.getAmount();
							}
						}
						
						if(amount > count){
							p.sendMessage(ChatColor.RED + "You only have "+ count + " " + shop.getDataName() + ".");
							return;
						}
						
						
						if(!plugin.getEcon().has(shop.getOwner(), amount * shop.getPrice())){
							p.sendMessage(ChatColor.RED + "That costs $" + ChatColor.YELLOW + amount * shop.getPrice() + ChatColor.RED + ", but the owner only has $" + ChatColor.YELLOW + plugin.getEcon().getBalance(shop.getOwner()));
							return;
						}
						if(amount == 0){
							//Dumb.
							sendPurchaseSuccess(p, shop, amount);
							return; 
						}
						else if(amount < 0){
							// & Dumber
							p.sendMessage(ChatColor.RED + "Derrrrp, Can't sell negative amounts.");
							return;
						}
						
						//Money handling
						if(!p.getName().equalsIgnoreCase(shop.getOwner())){
							//Don't tax them if they're purchasing from themselves.
							//Do charge an amount of tax though.
							double tax = plugin.getConfig().getDouble("tax");
							double total = amount * shop.getPrice();
							
							plugin.getEcon().withdrawPlayer(shop.getOwner(), total);
							
							if(!shop.isUnlimited() || (shop.isUnlimited() && plugin.getConfig().getBoolean("shop.pay-unlimited-shop-owners"))){
								plugin.getEcon().depositPlayer(p.getName(), total * (1 - tax));
								
								if(tax != 0){
									plugin.getEcon().depositPlayer(plugin.getConfig().getString("tax-account"), total * tax);
								}
							}
							
							Player owner = Bukkit.getPlayerExact(shop.getOwner());
							if(owner != null){
								owner.sendMessage(ChatColor.GREEN + p.getName() + " just sold " + amount + " " + ChatColor.YELLOW + shop.getDataName() + ChatColor.GREEN + " to your store.");
								if(space == amount) owner.sendMessage(ChatColor.DARK_PURPLE + "Your shop at " + shop.getLocation().getBlockX() + ", " + shop.getLocation().getBlockY() + ", " + shop.getLocation().getBlockZ() + " has run out of space");
							}
						}
						
						shop.buy(p, shop.getItem(), amount);
						sendSellSuccess(p, shop, amount);
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
		
		e.setCancelled(true);
	}
	private void sendPurchaseSuccess(Player p, Shop shop, int amount){
		p.sendMessage(ChatColor.DARK_PURPLE + "+---------------------------------------------------+");
		p.sendMessage(ChatColor.DARK_PURPLE + "| " + ChatColor.GREEN + "Successfully purchased:");
		p.sendMessage(ChatColor.DARK_PURPLE + "| " + ChatColor.YELLOW + amount + " " + shop.getItem().getType() + ChatColor.GREEN + " for " + ChatColor.YELLOW + amount * shop.getPrice());

		Map<Enchantment, Integer> enchs = shop.getEnchants();
		if(enchs != null && enchs.size() > 0){
			p.sendMessage(ChatColor.DARK_PURPLE + "+--------------------ENCHANTS-----------------------+");
			for(Entry<Enchantment, Integer> entries : enchs.entrySet()){
				p.sendMessage(ChatColor.DARK_PURPLE + "| " + ChatColor.YELLOW + entries.getKey() .getName() + " " + entries.getValue() );
			}
		}
		p.sendMessage(ChatColor.DARK_PURPLE + "+---------------------------------------------------+");
	}
	
	private void sendSellSuccess(Player p, Shop shop, int amount){
		p.sendMessage(ChatColor.DARK_PURPLE + "+---------------------------------------------------+");
		p.sendMessage(ChatColor.DARK_PURPLE + "| " + ChatColor.GREEN + "Successfully Sold:");
		p.sendMessage(ChatColor.DARK_PURPLE + "| " + ChatColor.YELLOW + amount + " " + shop.getItem().getType() + ChatColor.GREEN + " for " + ChatColor.YELLOW + amount * shop.getPrice());

		Map<Enchantment, Integer> enchs = shop.getEnchants();
		if(enchs != null && enchs.size() > 0){
			p.sendMessage(ChatColor.DARK_PURPLE + "+--------------------ENCHANTS-----------------------+");
			for(Entry<Enchantment, Integer> entries : enchs.entrySet()){
				p.sendMessage(ChatColor.DARK_PURPLE + "| " + ChatColor.YELLOW + entries.getKey() .getName() + " " + entries.getValue() );
			}
		}
		p.sendMessage(ChatColor.DARK_PURPLE + "+---------------------------------------------------+");
	}
}