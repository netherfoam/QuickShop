package org.maxgamer.QuickShop.Listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
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
	
	@EventHandler
	public void onChat(final PlayerChatEvent e){
		if(e.isCancelled()) return;
		Player p = e.getPlayer();
		HashMap<String, Info> actions = plugin.getActions();
		if(actions.containsKey(p.getName())){
			//They wanted to do something.
			Info info = actions.get(p.getName());
			
			/*
			 * Creation handling
			 */
			if(info.getAction() == ShopAction.CREATE){
				try{
					if(plugin.getShop(info.getLocation()) != null){
						p.sendMessage(ChatColor.RED + "Someone else has claimed that shop.");
						e.setCancelled(true);
						actions.remove(info.getLocation());
						return;
					}
					
					if(plugin.getChestNextTo(info.getLocation().getBlock()) != null){
						p.sendMessage(ChatColor.RED + "Double chest shops are disabled.");
						e.setCancelled(true);
						actions.remove(p.getName());
						return;
					}
					
					if(info.getLocation().getBlock().getType() != Material.CHEST){
						p.sendMessage(ChatColor.RED + "That chest was removed.");
						e.setCancelled(true);
						actions.remove(p.getName());
						return;
					}
					
					//Price per item
					double price = Double.parseDouble(e.getMessage());
					if(price < 0.01){
						p.sendMessage(ChatColor.RED + "Price must be greater than " + ChatColor.YELLOW + "$0.01");
						actions.remove(p.getName());
						e.setCancelled(true);
						return;
					}
					double tax = plugin.getConfig().getDouble("shop.cost"); 
					
					if(tax != 0 && plugin.getEcon().getBalance(p.getName()) <= tax){
						p.sendMessage(ChatColor.RED + "It costs $" + tax + " to create a new shop.");
						actions.remove(p.getName());
						e.setCancelled(true);
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
					
					//Save it to the database.
					int x = shop.getLocation().getBlockX();
					int y = shop.getLocation().getBlockY();
					int z = shop.getLocation().getBlockZ();
					String world = shop.getLocation().getWorld().getName();
					String itemString = plugin.makeString(shop.getItem());
					plugin.getDB().writeToBuffer("INSERT INTO shops VALUES ('"+e.getPlayer().getName()+"', '"+price+"', '"+itemString+"', '"+x+"', '"+y+"', '"+z+"', '"+world+"')");
					
					e.setCancelled(true); //Don't send to chat.
					plugin.getActions().remove(p.getName());
				}
				/*
				 * They didn't enter a number.
				 */
				catch(NumberFormatException ex){
					actions.remove(p.getName());
					p.sendMessage(ChatColor.RED + "Cancelled Shop Creation");
					if(!plugin.getConfig().getBoolean("always-chat")){
						e.setCancelled(true);
					}
					return;
				}
			}
			/*
			 * Purchase Handling
			 */
			else if(info.getAction() == ShopAction.BUY){
				try{
					int amount = Integer.parseInt(e.getMessage());
					Shop shop = plugin.getShops().get(info.getLocation());
					
					if(info.getLocation().getBlock().getType() != Material.CHEST){
						p.sendMessage(ChatColor.RED + "That shop was removed.");
						e.setCancelled(true);
						actions.remove(p.getName());
						return;
					}
					
					if(shop.getRemainingStock() >=  amount){
						if(plugin.getEcon().has(p.getName(), amount * shop.getPrice())){
							ItemStack transfer = shop.getItem().clone();
							transfer.setAmount(amount);
							
							if(amount == 0){
								//Dumb.
								sendPurchaseSuccess(p, shop, amount);
								actions.remove(p.getName());
								e.setCancelled(true);
								return; 
							}
							else if(amount < 0){
								// & Dumber
								p.sendMessage(ChatColor.RED + "Derrrrp, Can't buy negative amounts.");
								actions.remove(p.getName());
								e.setCancelled(true);
								return;
							}
							
							//Money handling
							if(!p.getName().equalsIgnoreCase(shop.getOwner())){
								//Don't tax them if they're purchasing from themselves.
								//Do charge an amount of tax though.
								double tax = plugin.getConfig().getDouble("tax");
								double total = amount * shop.getPrice();
								
								plugin.getEcon().withdrawPlayer(p.getName(), total);
								plugin.getEcon().depositPlayer(shop.getOwner(), total * (1 - tax));
								
								if(tax != 0){
									plugin.getEcon().depositPlayer(plugin.getConfig().getString("tax-account"), total * tax);
								}
								
								Player owner = Bukkit.getPlayerExact(shop.getOwner());
								if(owner != null) owner.sendMessage(ChatColor.GREEN + p.getName() + " just purchased " + amount + " " + shop.getItem().getType().toString() + " from your store.");
								if(shop.getRemainingStock() == amount) owner.sendMessage(ChatColor.DARK_PURPLE + "Your shop at " + shop.getLocation().getBlockX() + ", " + shop.getLocation().getBlockY() + ", " + shop.getLocation().getBlockZ() + " has run out of " + shop.getItem().getType().toString());
							}
							
							//Items to drop on floor
							HashMap<Integer, ItemStack> floor = new HashMap<Integer, ItemStack>(30);
							int amt = amount;
							while(amt > 0){
								int temp = Math.min(amt, transfer.getMaxStackSize());
								if(temp == -1){
									//Uh oh, don't know stacksize.
									transfer.setAmount(amt);
									floor.putAll(p.getInventory().addItem(transfer));
									break;
								}
								transfer.setAmount(temp);
								floor.putAll(p.getInventory().addItem(transfer));
								amt = amt - temp;
							}
							//Give the player items
							
							//Drop the remainder on the floor.
							for(int i = 0; i < floor.size(); i++){
								p.getWorld().dropItem(p.getLocation(), floor.get(i));								
							}
							shop.remove(transfer, amount);
							
							sendPurchaseSuccess(p, shop, amount);
							
							//Don't send it to chat.
							e.setCancelled(true);
							actions.remove(p.getName());
						}
						else{
							p.sendMessage(ChatColor.RED + "That costs " + ChatColor.YELLOW + amount * shop.getPrice() + ChatColor.RED + ", but you only have " + ChatColor.YELLOW + plugin.getEcon().getBalance(p.getName()));
							actions.remove(p.getName());
							e.setCancelled(true);
							return;
						}
					}
					else{
						p.sendMessage(ChatColor.RED + "The shop only has " + ChatColor.YELLOW + shop.getRemainingStock() + " " + shop.getMaterial().toString() + ChatColor.RED + " left.");
						actions.remove(p.getName());
						if(!plugin.getConfig().getBoolean("always-chat")){
							e.setCancelled(true);
						}
						return;
					}
				}
				/*
				 * They didn't enter a number.
				 */
				catch(NumberFormatException ex){
					actions.remove(p.getName());
					p.sendMessage(ChatColor.RED + "Cancelled Shop Purchase");
					e.setCancelled(true);
					return;
				}
			}
			/*
			 * If it was already cancelled (from destroyed)
			 */
			else{
				return; //It was cancelled, go away.
			}
		}
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
}