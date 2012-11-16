package org.maxgamer.QuickShop.Shop;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ShopCreateEvent extends Event implements Cancellable{
	private static final HandlerList handlers = new HandlerList();
	private Shop shop;
	private boolean cancelled;
	
	public ShopCreateEvent(Shop shop){
		this.shop = shop;
	}
	
	/**
	 * The shop to be created
	 * @return The shop to be created
	 */
	public Shop getShop(){
		return this.shop;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public boolean isCancelled() {
		return this.cancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.setCancelled(cancel);
	}
}