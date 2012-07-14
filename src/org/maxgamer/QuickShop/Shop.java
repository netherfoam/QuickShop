package org.maxgamer.QuickShop;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.util.Vector;

public class Shop{
	private Location loc;
	private double price;
	private String owner;
	private ItemStack item;
	private Item displayItem;
	
	private QuickShop plugin;
	
	/**
	 * Adds a new shop.
	 * @param loc The location of the chest block
	 * @param price The cost per item
	 * @param item The itemstack with the properties we want. This is .cloned, no need to worry about references
	 * @param owner The player who owns this shop.
	 */
	public Shop(Location loc, double price, ItemStack item, String owner){
		this.loc = loc;
		this.price = price;
		this.owner = owner;
		this.item = item.clone();
		this.plugin = (QuickShop) Bukkit.getPluginManager().getPlugin("QuickShop");
		this.item.setAmount(1);
		spawnDisplayItem();
	}
	/**
	 * Returns the number of items this shop has in stock.
	 * @return The number of items available for purchase.
	 */
	public int getRemainingStock(){
		Chest chest = (Chest) loc.getBlock().getState();
		int stock = 0;
		
		ItemStack[] in = chest.getInventory().getContents();
		for(ItemStack item : in){
			if(item != null && item.getType() == getMaterial() && item.getData().equals(getData()) && item.getEnchantments().equals(getEnchants())){
				stock = stock + item.getAmount();
			}
		}
		
		return stock;
	}
	
	public Location getLocation(){
		return this.loc;
	}
	public Location getDisplayLocation(){
		Location dispLoc = this.loc.clone();
		dispLoc.add(0.5, 1, 0.5);
		return dispLoc;
	}
	
	public double getPrice(){
		return this.price;
	}
	public Material getMaterial(){
		return this.item.getType();
	}
	
	public MaterialData getData(){
		return this.item.getData();
	}
	public Chest getChest(){
		return (Chest) this.loc.getBlock().getState();
	}
	public String getOwner(){
		return this.owner;
	}
	public Map<Enchantment, Integer> getEnchants(){
		return this.item.getEnchantments();
	}
	public ItemStack getItem(){
		return item;
	}
	/**
	 * Removes an item from the shop.
	 * @param item The itemstack.  The amount does not matter, just everything else
	 * @param amount The amount to remove from the shop.
	 */
	public void remove(ItemStack item, int amount){
		Inventory inv = this.getChest().getInventory();
		
		int remains = amount;
		
		while(remains > 0){
			int stackSize = Math.min(remains, item.getMaxStackSize());
			item.setAmount(stackSize);
			inv.removeItem(item);
			remains = remains - stackSize;
		}
	}
	
	/**
	 * Spawns the dummy item on top of the shop.
	 */
	public void spawnDisplayItem(){
		Location sLoc = this.getDisplayLocation();
		
		Item item = this.loc.getWorld().dropItem(sLoc, this.item.clone());
		item.setVelocity(new Vector(0, 0.1, 0));
		//Actually not possible.
		item.setPickupDelay(6000);  
		//Protects the item from decay.
		plugin.getProtectedItems().put(this, item);
		this.displayItem = item;
	}
	public void removeDupeItem(Block b){
		Chunk c = b.getChunk();
		for (Entity e : c.getEntities()) {
			if (e.getLocation().getBlock().equals(b) && e instanceof Item && !e.equals(item)) {
				e.remove();
			}
		}
	}
	
	
	/**
	 * Removes the display item entirely.
	 */
	public void deleteDisplayItem(){
		plugin.getProtectedItems().remove(item);
		this.displayItem.remove();
	}
}