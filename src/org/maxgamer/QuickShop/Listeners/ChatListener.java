package org.maxgamer.QuickShop.Listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.maxgamer.QuickShop.Info;
import org.maxgamer.QuickShop.QuickShop;
import org.maxgamer.QuickShop.Shop;
import org.maxgamer.QuickShop.ShopAction;
/**
 * HashMap<Name, Info>
 * 
 * 
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
	public void onChat(PlayerChatEvent e){
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
					//Price per item
					double price = Double.parseDouble(e.getMessage());
					if(price < 0.01){
						p.sendMessage("Price must be greater than $0.01");
						e.setCancelled(true);
						return;
					}
					//Add the shop to the list.
					Shop shop = new Shop(info.getLocation(), price, info.getItem(), p.getName());
					plugin.getShops().put(info.getLocation(), shop);
					p.sendMessage("Created a shop");
					
					//ToDo: save it to database
					
					e.setCancelled(true); //Don't send to chat.
					plugin.getActions().remove(p.getName());
				}
				/*
				 * They didn't enter a number.
				 */
				catch(NumberFormatException ex){
					actions.remove(p.getName());
					p.sendMessage("Cancelled Shop Creation");
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
					
					if(shop.getRemainingStock() >=  amount){
						if(plugin.getEcon().has(p.getName(), amount * shop.getPrice())){
							ItemStack transfer = shop.getItem().clone();
							transfer.setAmount(amount);
							
							if(amount == 0){
								//Dumb.
								sendPurchaseSuccess(p, transfer, amount);
								return; 
							}
							else if(amount < 0){
								// & Dumber
								p.sendMessage(ChatColor.RED + "Derrrrp, Can't buy negative amounts.");
								return;
							}
							
							plugin.getEcon().withdrawPlayer(p.getName(), amount * shop.getPrice());
							plugin.getEcon().depositPlayer(shop.getOwner(), amount * shop.getPrice());	
							
							HashMap<Integer, ItemStack> floor = p.getInventory().addItem(transfer);
							
							//Drop the remainder on the floor.
							for(int i = 0; i < floor.size(); i++){
								p.getWorld().dropItem(p.getLocation(), floor.get(i));								
							}
							shop.remove(transfer, amount);
							
							sendPurchaseSuccess(p, transfer, amount);
							
							//Don't send it to chat.
							e.setCancelled(true);
						}
						else{
							p.sendMessage("That costs " + amount * shop.getPrice() + ", but you only have " + plugin.getEcon().getBalance(p.getName()));
							e.setCancelled(true);
							return;
						}
					}
					else{
						p.sendMessage("The shop only has " + shop.getRemainingStock() + " " + shop.getMaterial().toString() + " left.");
						e.setCancelled(true);
						return;
					}
				}
				/*
				 * They didn't enter a number.
				 */
				catch(NumberFormatException ex){
					actions.remove(p.getName());
					p.sendMessage("Cancelled Shop Purchase");
					return;
				}
			}
			/*
			 * If it was already cancelled (from destroyed)
			 */
			else if(info.getAction() == ShopAction.CANCELLED){
				return; //It was cancelled, go away.
			}
			/*
			 * Debug: Should never happen.
			 */
			else{
				//Impossibru
				p.sendMessage("Impossibru");
				return;
			}
		}
	}
	private void sendPurchaseSuccess(Player p, ItemStack items, int amount){
		p.sendMessage(ChatColor.DARK_PURPLE + "+---------------------------------------------------+");
		p.sendMessage(ChatColor.DARK_PURPLE + "| " + ChatColor.GREEN + "Successfully purchased:");
		p.sendMessage(ChatColor.DARK_PURPLE + "| " + ChatColor.YELLOW + amount + " " + items.getType());
		Map<Enchantment, Integer> enchs = items.getEnchantments();
		if(enchs!= null){
			for(Entry<Enchantment, Integer> entries : enchs.entrySet()){
				p.sendMessage(ChatColor.DARK_PURPLE + "| " + ChatColor.YELLOW + entries.getKey() .getName() + " " + entries.getValue() );
			}
		}
		p.sendMessage(ChatColor.DARK_PURPLE + "+---------------------------------------------------+");
	}
}