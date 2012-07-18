package org.maxgamer.QuickShop.Shop;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Info{
	Location loc;
	ShopAction action;
	ItemStack item;
	
	/**
	 * Stores info for the players last shop interact.
	 * @param loc The location they clicked (Block.getLocation())
	 * @param action The action (ShopAction.*)
	 * @param material The material they were holding
	 * @param data The data value of the material
	 */
	public Info(Location loc, ShopAction action, ItemStack item){
		this.loc = loc;
		this.action = action;
		if(item != null) this.item = item.clone();
	}
	public ShopAction getAction(){
		return this.action;
	}
	public Location getLocation(){
		return this.loc;
	}
	public Material getMaterial(){
		return this.item.getType();
	}
	public byte getData(){
		return this.getData();
	}
	public ItemStack getItem(){
		return this.item;
	}
	public void setAction(ShopAction action){
		this.action = action;
	}
}