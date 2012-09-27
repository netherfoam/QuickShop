package org.maxgamer.QuickShop.Listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.milkbowl.vault.economy.EconomyResponse;

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
						
						if(plugin.getChestNextTo(info.getLocation().getBlock()) != null){
							p.sendMessage(plugin.getMessage("no-double-chests"));
							return;
						}
						
						if(info.getLocation().getBlock().getType() != Material.CHEST){
							p.sendMessage(plugin.getMessage("chest-was-removed"));
							return;
						}
						
						//Price per item
						double price = Double.parseDouble(e.getMessage());
						if(price < 0.01){
							p.sendMessage(plugin.getMessage("price-too-cheap"));
							return;
						}
						double tax = plugin.getConfig().getDouble("shop.cost"); 
						
						if(tax != 0 && plugin.getEcon().getBalance(p.getName()) <= tax){
							p.sendMessage(plugin.getMessage("you-cant-afford-a-new-shop", plugin.getEcon().format(tax)));
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
						amount = Integer.parseInt(e.getMessage());
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
							if(owner != null){
								owner.sendMessage(plugin.getMessage("player-just-bought-from-your-store", p.getName(), ""+amount, shop.getDataName()));
								if(stock == amount) owner.sendMessage(plugin.getMessage("shop-out-of-stock", ""+shop.getLocation().getBlockX(), ""+shop.getLocation().getBlockY(), ""+shop.getLocation().getBlockZ(), shop.getDataName()));
							}
							else{
								//TODO: Log this, spit it to them when they log in.
							}
						}
						//Transfers the item from A to B
						shop.sell(p, shop.getItem(), amount);
						sendPurchaseSuccess(p, shop, amount);
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
									p.sendMessage(plugin.getMessage("the-owner-cant-afford-to-buy-from-you", plugin.getEcon().format(amount * shop.getPrice()), plugin.getEcon().format(plugin.getEcon().getBalance(shop.getOwner()))));
									return;
								}
								
								EconomyResponse r = plugin.getEcon().withdrawPlayer(shop.getOwner(), total);
								
								//Check for plugins faking econ.has(amount)
								if(!r.transactionSuccess()){
									//p.sendMessage(ChatColor.RED + "[QuickShop] Transaction failed.  Does the owner have enough cash?");
									p.sendMessage(plugin.getMessage("the-owner-cant-afford-to-buy-from-you", plugin.getEcon().format(amount * shop.getPrice()), plugin.getEcon().format(plugin.getEcon().getBalance(shop.getOwner()))));
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
							if(owner != null){
								//owner.sendMessage(ChatColor.GREEN + p.getName() + " just sold " + amount + " " + ChatColor.YELLOW + shop.getDataName() + ChatColor.GREEN + " to your store.");
								owner.sendMessage(plugin.getMessage("player-just-sold-to-your-store", p.getName(), ""+amount, shop.getDataName()));
								if(space == amount) owner.sendMessage(plugin.getMessage("shop-out-of-space", ""+shop.getLocation().getBlockX(), ""+shop.getLocation().getBlockY(), ""+shop.getLocation().getBlockZ())); 
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
		p.sendMessage(ChatColor.DARK_PURPLE + "| " + plugin.getMessage("menu.successful-purchase"));
		p.sendMessage(ChatColor.DARK_PURPLE + "| " + plugin.getMessage("menu.item-name-and-price", ""+amount, shop.getDataName(), ""+(amount * shop.getPrice())));

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
		p.sendMessage(ChatColor.DARK_PURPLE + "| " + plugin.getMessage("menu.item-name-and-price", ""+amount, shop.getDataName(), ""+(amount * shop.getPrice())));

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