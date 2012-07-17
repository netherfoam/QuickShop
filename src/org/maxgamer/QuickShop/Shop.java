package org.maxgamer.QuickShop;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
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
			if(item != null && item.getType() == getMaterial() && item.getDurability() == getDurability() && item.getEnchantments().equals(getEnchants())){
				stock = stock + item.getAmount();
			}
		}
		
		return stock;
	}
	/**
	 * @return The location of the shops chest
	 */
	public Location getLocation(){
		return this.loc;
	}
	/**
	 * @return The display item location.  Not block location.
	 */
	public Location getDisplayLocation(){
		Location dispLoc = this.loc.clone();
		dispLoc.add(0.5, 1, 0.5);
		return dispLoc;
	}
	
	/**
	 * @return The price per item this shop is selling
	 */
	public double getPrice(){
		return this.price;
	}
	/**
	 * @return The ItemStack type of this shop
	 */
	public Material getMaterial(){
		return this.item.getType();
	}
	
	/**
	 * @return The durability of the item
	 */
	public short getDurability(){
		return this.item.getDurability();
	}
	/**
	 * @return The chest this shop is based on.
	 */
	public Chest getChest(){
		return (Chest) this.loc.getBlock().getState();
	}
	/**
	 * @return The name of the player who owns the shop.
	 */
	public String getOwner(){
		return this.owner;
	}
	/**
	 * @return The enchantments the shop has on its items.
	 */
	public Map<Enchantment, Integer> getEnchants(){
		return this.item.getEnchantments();
	}
	/**
	 * @return Returns a dummy itemstack of the item this shop is selling.
	 */
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
		item.setPickupDelay(Integer.MAX_VALUE);  
		//Protects the item from decay.
		plugin.getProtectedItems().put(this, item);
		this.displayItem = item;
	}
	
	/**
	 * Spawns the new display item and removes duplicate items.
	 */
	public void respawnDisplayItem(){
		spawnDisplayItem();
		removeDupeItem();
	}
	
	/**
	 * Removes all items floating ontop of the chest
	 * that aren't the display item.
	 */
	public void removeDupeItem(){
		Location displayLoc = this.getLocation().getBlock().getRelative(0, 1, 0).getLocation();
		
		Chunk c = displayLoc.getChunk();
		for (Entity e : c.getEntities()) {
			if(e.getEntityId() != this.displayItem.getEntityId() && (e.getLocation().getBlock().getLocation().equals(displayLoc) || e.getLocation().getBlock().getLocation().equals(this.loc)) && e instanceof Item) {
				ItemStack itm = ((Item) e).getItemStack();
				if(itm.getType() == item.getType() && itm.getAmount() == 1 && itm.getDurability() == item.getDurability()){
					e.remove();
				}
				
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