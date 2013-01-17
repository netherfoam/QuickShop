package org.maxgamer.QuickShop.Shop;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface Shop{
	/**
	 * Returns a clone of this shop.
	 * References to the same display item,
	 * itemstack, location and owner as
	 * this shop does. Do not modify them or
	 * you will modify this shop.
	 * 
	 * **NOT A DEEP CLONE**
	 */
	public Shop clone();
	/**
	 * Returns the number of items this shop has in stock.
	 * @return The number of items available for purchase.
	 */
	public int getRemainingStock();
	
	/**
	 * Returns the number of free spots in the chest for the particular item.
	 * @param stackSize
	 * @return
	 */
	public int getRemainingSpace();
	/**
	 * Returns true if the ItemStack matches what this shop is selling/buying
	 * @param item The ItemStack
	 * @return True if the ItemStack is the same (Excludes amounts)
	 */
	public boolean matches(ItemStack item);
	
	/**
	 * @return The location of the shops chest
	 */
	public Location getLocation();
	
	/**
	 * @return The price per item this shop is selling
	 */
	public double getPrice();
	
	/**
	 * Sets the price of the shop. Does not update it in the database. Use shop.update() for that.
	 * @param price The new price of the shop.
	 */
	public void setPrice(double price);
	
	/**
	 * Upates the shop into the database. 
	 */
	public void update();
	
	/**
	 * @return The durability of the item
	 */
	public short getDurability();
	
	/**
	 * @return The name of the player who owns the shop.
	 */
	public String getOwner();
	/**
	 * @return Returns a dummy itemstack of the item this shop is selling.
	 */
	public ItemStack getItem();
	/**
	 * Removes an item from the shop.
	 * @param item The itemstack.  The amount does not matter, just everything else
	 * @param amount The amount to remove from the shop.
	 */
	public void remove(ItemStack item, int amount);
	
	/**
	 * Add an item to shops chest.
	 * @param item The itemstack.  The amount does not matter, just everything else
	 * @param amount The amount to add to the shop.
	 */
	public void add(ItemStack item, int amount);
	
	/**
	 * Sells amount of item to Player p.  Does NOT check our inventory, or balances
	 * @param p The player to sell to
	 * @param amount The amount to sell
	 */
	public void sell(Player p, int amount);
	
	/**
	 * Buys amount of item from Player p.  Does NOT check our inventory, or balances
	 * @param p The player to buy from
	 * @param item The itemStack to buy
	 * @param amount The amount to buy
	 */
	public void buy(Player p, int amount);
	
	/**
	 * Changes the owner of this shop to the given player.
	 * @param owner The name of the owner.
	 * You must do shop.update() after to save it after a reboot.
	 */
	public void setOwner(String owner);
	
	public void setUnlimited(boolean unlimited);
	public boolean isUnlimited();
	
	public ShopType getShopType();
	
	public boolean isBuying();
	
	public boolean isSelling();
	
	/**
	 * Changes a shop type to Buying or Selling. Also updates the signs nearby.
	 * @param shopType The new type (ShopType.BUYING or ShopType.SELLING)
	 */
	public void setShopType(ShopType shopType);
	
	/**
	 * Updates signs attached to the shop
	 */
	public void setSignText();
	
	/**
	 * Changes all lines of text on a sign near the shop
	 * @param lines The array of lines to change. Index is line number.
	 */
	public void setSignText(String[] lines);
	
	/**
	 * Returns a list of signs that are attached to this shop (QuickShop and blank signs only)
	 * @return a list of signs that are attached to this shop (QuickShop and blank signs only)
	 */
	public List<Sign> getSigns();
	
	public boolean isAttached(Block b);
	
	/**
	 * Convenience method. Equivilant to org.maxgamer.QuickShop.Util.getName(shop.getItem()).
	 * @return The name of this shops item
	 */
	public String getDataName();
	
	/**
	 * Deletes the shop from the list of shops
	 * and queues it for database deletion
	 * *DOES* delete it from memory
	 */
	public void delete();
	
	/**
	 * Deletes the shop from the list of shops
	 * and queues it for database deletion
	 * @param fromMemory True if you are *NOT* iterating over this currently, *false if you are iterating*
	 */
	public void delete(boolean fromMemory);
	
	/**
	 * Should return true if this shop is valid.
	 * Should return false if it is not - Such as, a ChestShop should be situated on a chest.
	 * 
	 * This method is called periodically.  Here, you should check:
	 * <br/> * The block this is on has not changed (E.g. WorldEdit does not throw block events)
	 * <br/> * The display item (if any) is still valid etc
	 * <br/> * And anything else that has to be brute force checked periodically.
	 * <br/>
	 * <br/> You can safely assume that this shop's world is loaded during this method.
	 * @return
	 */
	public boolean isValid();
	/**
	 * This method is called whenever the shop should be unloaded.
	 * E.g. for chest shops, they should clean up their own display items.
	 * This method is called when the chunk the shop is stored in is unloaded.
	 * 
	 * This should not remove the shop from memory (That is done by the caller, if at all).
	 */
	public void onUnload();
	/**
	 * This method is called whenever the shop is loaded.
	 * Such as when it is first created, or when the chunk
	 * it is in is loaded from disk.
	 */
	public void onLoad();
}