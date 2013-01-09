package org.maxgamer.QuickShop.Auction;

import java.util.LinkedList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.maxgamer.QuickShop.QuickShop;
import org.maxgamer.QuickShop.Util;

public class Auction{
	private Player seller;
	private Player bidder;
	
	private ItemStack iStack;
	private int amount;
	private double price;
	
	private int ticks = 1200;
	
	private BukkitTask timer;
	
	/**
	 * Represents a new auction.
	 * @param seller The player selling the items
	 * @param iStack The items they're selling
	 * @param amount The amount they're selling
	 * @param price The price they're selling for in total
	 */
	public Auction(Player seller, ItemStack iStack, int amount, double price){
		this.seller = seller;
		this.iStack = iStack.clone();
		this.amount = amount;
		this.price = price;
	}
	
	public void setLength(int ticks){
		this.ticks = ticks;
	}
	public int getLength(){
		return ticks;
	}
	public Player getSeller(){
		return seller;
	}
	public ItemStack getItemStack(){
		return iStack;
	}
	public double getPrice(){
		return price;
	}
	public int getAmount(){
		return amount;
	}
	/**
	 * Sets the latest bidder for this auction.
	 * @param bidder The player who bidded
	 * @param price The price they're paying
	 */
	public void bid(Player bidder, double price){
		this.bidder = bidder;
		this.price = price;
	}
	public Player getBidder(){
		return this.bidder;
	}
	
	/**
	 * Starts this auction off. Announces all required info.
	 */
	public void start(){
		final int updateFrequency = 300;
		
		Runnable r = new Runnable(){
			@Override
			public void run() {
				ticks = Math.max(ticks - updateFrequency, 0);
				
				if(ticks > 0){
					Bukkit.broadcastMessage(ChatColor.RED + "[Auction] " + (ticks/20) + " seconds remain!");
				}
				
				if(ticks == 0){
					end();
				}
				
				if(ticks < updateFrequency){
					timer.cancel(); //Throw out the old task
					timer = Bukkit.getScheduler().runTaskLater(QuickShop.instance, this, ticks); //In with the new
				}
			}
		};
		
		timer = Bukkit.getScheduler().runTaskTimer(QuickShop.instance, r, updateFrequency, updateFrequency);
	}
	
	/**
	 * Cancels this auction in an emergency.
	 */
	public void cancel(){
		if(timer != null){
			timer.cancel();
			timer = null;
		}
		Bukkit.broadcastMessage(ChatColor.RED + "[Auction] The auction has been cancelled.");
	}
	
	public boolean isRunning(){
		return timer != null;
	}
	
	/**
	 * Ends the auction, and awards the highest bidder their items.
	 */
	public void end(){
		if(timer != null){
			timer.cancel();
			timer = null;
		}
		
		if(!bidder.isOnline()){
			bidder = Bukkit.getPlayerExact(bidder.getName());
			
			if(bidder == null){
				seller.sendMessage(ChatColor.RED + "Last bidder is no longer online. Auction failed.");
				return;
			}
			//Else, they just relogged.
		}
		
		if(QuickShop.instance.getEcon().transfer(bidder.getName(), seller.getName(), price) == false){
			//You don't have the cash to buy that.
			bidder.sendMessage(ChatColor.RED + "You don't have enough cash!");
			seller.sendMessage(ChatColor.RED + "The bidder doesn't have enough cash!");
			return;
		}
		
		LinkedList<ItemStack> drops = new LinkedList<ItemStack>();
		
		int amount = this.amount;
		while(amount > 0){
			int stackSize = Math.min(amount, this.iStack.getMaxStackSize());
			this.iStack.setAmount(stackSize);
			
			drops.addAll(bidder.getInventory().addItem(iStack.clone()).values());
			amount -= stackSize;
		}
		
		for(ItemStack drop : drops){
			bidder.getWorld().dropItem(bidder.getLocation(), drop);
		}
		
		bidder.sendMessage(ChatColor.GREEN + "Auction won: " + QuickShop.instance.getEcon().format(price) + " for " + amount + " " + Util.getName(iStack));
		Bukkit.broadcastMessage(ChatColor.GREEN + "The auction has been won by " + bidder.getName());
	}
}