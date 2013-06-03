package org.maxgamer.QuickShop.Shop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.maxgamer.QuickShop.QuickShop;
import org.maxgamer.QuickShop.Util.MsgUtil;
import org.maxgamer.QuickShop.Util.Util;

public class ContainerShop implements Shop{
	private Location loc;
	private double price;
	private String owner;
	private ItemStack item;
	private DisplayItem displayItem;
	private boolean unlimited;
	private ShopType shopType;
	
	private QuickShop plugin;
	
	/**
	 * Returns a clone of this shop.
	 * References to the same display item,
	 * itemstack, location and owner as
	 * this shop does. Do not modify them or
	 * you will modify this shop.
	 * 
	 * **NOT A DEEP CLONE**
	 */
	public ContainerShop clone(){
		return new ContainerShop(this);
	}
	
	private ContainerShop(ContainerShop s){
		this.displayItem = s.displayItem;
		this.shopType = s.shopType;
		this.item = s.item;
		this.loc = s.loc;
		this.plugin = s.plugin;
		this.unlimited = s.unlimited;
		this.owner = s.owner;
		this.price = s.price;
	}
	
	/**
	 * Adds a new shop.
	 * @param loc The location of the chest block
	 * @param price The cost per item
	 * @param item The itemstack with the properties we want. This is .cloned, no need to worry about references
	 * @param owner The player who owns this shop.
	 */
	public ContainerShop(Location loc, double price, ItemStack item, String owner){
		this.loc = loc;
		this.price = price;
		this.owner = owner;
		this.item = item.clone();
		this.plugin = (QuickShop) Bukkit.getPluginManager().getPlugin("QuickShop");
		this.item.setAmount(1);
		
		if(plugin.display){
			this.displayItem = new DisplayItem(this, this.item);
		}
		
		this.shopType = ShopType.SELLING;
	}
	/**
	 * Returns the number of items this shop has in stock.
	 * @return The number of items available for purchase.
	 */
	public int getRemainingStock(){
		if(this.unlimited) return 10000;
		return Util.countItems(this.getInventory(), this.getItem());
	}
	
	/**
	 * Returns the number of free spots in the chest for the particular item.
	 * @param stackSize
	 * @return
	 */
	public int getRemainingSpace(){
		if(this.unlimited) return 10000;
		return Util.countSpace(this.getInventory(), item);
	}
	/**
	 * Returns true if the ItemStack matches what this shop is selling/buying
	 * @param item The ItemStack
	 * @return True if the ItemStack is the same (Excludes amounts)
	 */
	public boolean matches(ItemStack item){
		return Util.matches(this.item, item);
	}
	
	/**
	 * Returns the shop that shares it's inventory with this one.
	 * @return the shop that shares it's inventory with this one.
	 * Will return null if this shop is not attached to another.
	 */
	public ContainerShop getAttachedShop(){
		Block c = Util.getSecondHalf(this.getLocation().getBlock());
		if(c == null) return null;
		Shop shop = plugin.getShopManager().getShop(c.getLocation());
		return shop == null ? null : (ContainerShop) shop;
	}
	
	/**
	 * Returns true if this shop is a double chest, and the other half is selling/buying the same as this is buying/selling.
	 * @return true if this shop is a double chest, and the other half is selling/buying the same as this is buying/selling.
	 */
	public boolean isDoubleShop(){
		ContainerShop nextTo = this.getAttachedShop();
		if(nextTo == null){
			return false;
		}
		
		if(nextTo.matches(this.getItem())){
			//They're both trading the same item
			if(this.getShopType() == nextTo.getShopType()){
				//They're both buying or both selling => Not a double shop, just two shops.
				return false;
			}
			else{
				//One is buying, one is selling.
				return true;
			}
		}
		else{
			return false;
		}
	}
	
	/**
	 * @return The location of the shops chest
	 */
	public Location getLocation(){
		return this.loc;
	}
	
	/**
	 * @return The price per item this shop is selling
	 */
	public double getPrice(){
		return this.price;
	}
	
	/**
	 * Sets the price of the shop. Does not update it in the database. Use shop.update() for that.
	 * @param price The new price of the shop.
	 */
	public void setPrice(double price){
		this.price = price;
	}
	/**
	 * @return The ItemStack type of this shop
	 */
	public Material getMaterial(){
		return this.item.getType();
	}
	
	/**
	 * Upates the shop into the database. 
	 */
	public void update(){
		int x = this.getLocation().getBlockX();
		int y = this.getLocation().getBlockY();
		int z = this.getLocation().getBlockZ();
		
		String world = this.getLocation().getWorld().getName();		
		int unlimited = this.isUnlimited() ? 1 : 0;

		String q = "UPDATE shops SET owner = ?, itemConfig = ?, unlimited = ?, type = ?, price = ? WHERE x = ? AND y = ? and z = ? and world = ?";
		try{
			plugin.getDB().execute(q, this.getOwner(), Util.serialize(this.getItem()), unlimited, shopType.toID(), this.getPrice(), x, y, z, world);
		}
		catch(Exception e){
			e.printStackTrace();
			System.out.println("Could not update shop in database! Changes will revert after a reboot!");
		}
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

	public Inventory getInventory(){
		InventoryHolder container = (InventoryHolder) this.loc.getBlock().getState();
		return container.getInventory();
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
		return this.item.getItemMeta().getEnchants();
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
		if(this.unlimited) return;
		Inventory inv = this.getInventory();
		
		int remains = amount;
		
		while(remains > 0){
			int stackSize = Math.min(remains, item.getMaxStackSize());
			item.setAmount(stackSize);
			inv.removeItem(item);
			remains = remains - stackSize;
		}
	}
	
	/**
	 * Add an item to shops chest.
	 * @param item The itemstack.  The amount does not matter, just everything else
	 * @param amount The amount to add to the shop.
	 */
	public void add(ItemStack item, int amount){
		if(this.unlimited) return;
		
		Inventory inv = this.getInventory();
		
		int remains = amount;
		
		while(remains > 0){
			int stackSize = Math.min(remains, item.getMaxStackSize());
			item.setAmount(stackSize);
			inv.addItem(item);
			remains = remains - stackSize;
		}
	}
	
	/**
	 * Sells amount of item to Player p.  Does NOT check our inventory, or balances
	 * @param p The player to sell to
	 * @param amount The amount to sell
	 */
	public void sell(Player p, int amount){
		if(amount < 0) this.buy(p, -amount);
		//Items to drop on floor
		ArrayList<ItemStack> floor = new ArrayList<ItemStack>(5);
		Inventory pInv = p.getInventory();
		if(this.isUnlimited()){
			ItemStack item = this.item.clone();
			
			while(amount > 0){
				int stackSize = Math.min(amount, this.item.getMaxStackSize());
				item.setAmount(stackSize);
				pInv.addItem(item);
				
				amount -= stackSize;
			}
		}
		else{
			ItemStack[] chestContents = this.getInventory().getContents();
			for(int i = 0; amount > 0 && i < chestContents.length; i++){
				//Can't clone it here, it could be null
				ItemStack item = chestContents[i];
				
				if(item != null && this.matches(item)){
					//Copy it, we don't want to interfere
					item = item.clone();
					
					//Amount = total, item.getAmount() = how many items in the stack
					int stackSize = Math.min(amount, item.getAmount());
					
					//If Amount is item.getAmount(), then this sets the amount to 0
					//Else it sets it to the remainder
					chestContents[i].setAmount(chestContents[i].getAmount() - stackSize);
					
					//We can modify this, it is a copy.
					item.setAmount(stackSize);
					
					//Add the items to the players inventory
					floor.addAll(pInv.addItem(item).values());
					
					amount -= stackSize;
				}
			}
			
			//We now have to update the chests inventory manually.
			this.getInventory().setContents(chestContents);
		}
		
		for(int i = 0; i < floor.size(); i++){
			p.getWorld().dropItem(p.getLocation(), floor.get(i));								
		}
	}
	
	/**
	 * Buys amount of item from Player p.  Does NOT check our inventory, or balances
	 * @param p The player to buy from
	 * @param item The itemStack to buy
	 * @param amount The amount to buy
	 */
	public void buy(Player p, int amount){
		if(amount < 0) this.sell(p, -amount);
		
		if(this.isUnlimited()){
			ItemStack[] contents = p.getInventory().getContents();
			
			for(int i = 0; amount > 0 && i < contents.length; i++){
				ItemStack stack = contents[i];
				if(stack == null) continue; //No item
				if(matches(stack)){
					int stackSize = Math.min(amount, stack.getAmount());
					
					stack.setAmount(stack.getAmount() - stackSize);
					amount -= stackSize;
				}
			}
			//Send the players new inventory to them
			p.getInventory().setContents(contents);
			
			//This should not happen.
			if(amount > 0){
				plugin.getLogger().log(Level.WARNING, "Could not take all items from a players inventory on purchase! " + p.getName() + ", missing: " + amount + ", item: " + this.getDataName() + "!");
			}
		}
		else{
			ItemStack[] playerContents = p.getInventory().getContents();
			Inventory chestInv = this.getInventory();
			
			for(int i = 0; amount > 0 && i < playerContents.length; i++){
				ItemStack item = playerContents[i];
				
				if(item != null && this.matches(item)){
					//Copy it, we don't want to interfere
					item = item.clone();
					//Amount = total, item.getAmount() = how many items in the stack
					int stackSize = Math.min(amount, item.getAmount());
					
					//If Amount is item.getAmount(), then this sets the amount to 0
					//Else it sets it to the remainder
					playerContents[i].setAmount(playerContents[i].getAmount() - stackSize);
					
					//We can modify this, it is a copy.
					item.setAmount(stackSize);
					
					//Add the items to the players inventory
					chestInv.addItem(item);
					
					amount -= stackSize;
				}
			}
			
			//Now update the players inventory.
			p.getInventory().setContents(playerContents);
		}
	}
	
	/**
	 * Changes the owner of this shop to the given player.
	 * @param owner The name of the owner.
	 * You must do shop.update() after to save it after a reboot.
	 */
	public void setOwner(String owner){
		this.owner = owner;
	}
	
	/**
	 * Returns the display item associated with this shop.
	 * @return The display item associated with this shop.
	 */
	public DisplayItem getDisplayItem(){
		return this.displayItem;
	}
	
	public void setUnlimited(boolean unlimited){
		this.unlimited = unlimited;
	}
	public boolean isUnlimited(){
		return this.unlimited;
	}
	
	public ShopType getShopType(){
		return this.shopType;
	}
	
	public boolean isBuying(){
		return this.shopType == ShopType.BUYING;
	}
	
	public boolean isSelling(){
		return this.shopType == ShopType.SELLING;
	}
	
	/**
	 * Changes a shop type to Buying or Selling. Also updates the signs nearby.
	 * @param shopType The new type (ShopType.BUYING or ShopType.SELLING)
	 */
	public void setShopType(ShopType shopType){
		this.shopType = shopType;
		this.setSignText();
	}
	
	/**
	 * Updates signs attached to the shop
	 */
	public void setSignText(){
		if(Util.isLoaded(this.getLocation()) == false) return;
		
		String[] lines = new String[4];
		lines[0] = ChatColor.RED + "[QuickShop]";
		if(this.isBuying()){
			lines[1] = MsgUtil.getMessage("signs.buying", ""+this.getRemainingSpace());
		}
		if(this.isSelling()){
			lines[1] = MsgUtil.getMessage("signs.selling", ""+this.getRemainingStock());
		}
		lines[2] = Util.getName(this.item);
		lines[3] = MsgUtil.getMessage("signs.price", ""+this.getPrice());
		this.setSignText(lines);
	}
	
	/**
	 * Changes all lines of text on a sign near the shop
	 * @param lines The array of lines to change. Index is line number.
	 */
	public void setSignText(String[] lines){
		if(Util.isLoaded(this.getLocation()) == false) return;
		
		for(Sign sign : this.getSigns()){
			for(int i = 0; i < lines.length; i++){
				sign.setLine(i,  lines[i]);
			}
			
			sign.update();
		}
	}
	
	/**
	 * Returns a list of signs that are attached to this shop (QuickShop and blank signs only)
	 * @return a list of signs that are attached to this shop (QuickShop and blank signs only)
	 */
	public List<Sign> getSigns(){
		ArrayList<Sign> signs = new ArrayList<Sign>(1);
		
		if(this.getLocation().getWorld() == null) return signs;
		
		Block[] blocks = new Block[4];
		blocks[0] = loc.getBlock().getRelative(1, 0, 0);
		blocks[1] = loc.getBlock().getRelative(-1, 0, 0);
		blocks[2] = loc.getBlock().getRelative(0, 0, 1);
		blocks[3] = loc.getBlock().getRelative(0, 0, -1);
		
		for(Block b : blocks){
			if(b.getType() != Material.WALL_SIGN) continue;
			if(!isAttached(b)) continue;
			Sign sign = (Sign) b.getState();
			
			if(sign.getLine(0).contains("[QuickShop")){
				signs.add(sign);
			}
			else{
				boolean text = false;
				for(String s : sign.getLines()){
					if(!s.isEmpty()){
						text = true;
						break;
					}
				}
				
				if(!text){
					signs.add(sign);
				}
			}
		}
		return signs;
	}
	
	public boolean isAttached(Block b){
		if(b.getType() != Material.WALL_SIGN) new IllegalArgumentException(b + " Is not a sign!").printStackTrace();
		return this.getLocation().getBlock().equals(Util.getAttached(b));
	}
	
	/**
	 * Convenience method. Equivilant to org.maxgamer.QuickShop.Util.getName(shop.getItem()).
	 * @return The name of this shops item
	 */
	public String getDataName(){
		return Util.getName(this.getItem());
	}
	
	/**
	 * Deletes the shop from the list of shops
	 * and queues it for database deletion
	 * *DOES* delete it from memory
	 */
	public void delete(){
		delete(true);
	}
	
	/**
	 * Deletes the shop from the list of shops
	 * and queues it for database deletion
	 * @param fromMemory True if you are *NOT* iterating over this currently, *false if you are iterating*
	 */
	public void delete(boolean fromMemory){
		//Delete the display item
		
		if(this.getDisplayItem() != null){
			this.getDisplayItem().remove();
		}
		
		//Delete the signs around it
		for(Sign s : this.getSigns()){
			s.getBlock().setType(Material.AIR);
		}
		
		//Delete it from the database
		int x = this.getLocation().getBlockX();
		int y = this.getLocation().getBlockY();
		int z = this.getLocation().getBlockZ();
		String world = this.getLocation().getWorld().getName();
		plugin.getDB().execute("DELETE FROM shops WHERE x = '"+x+"' AND y = '"+y+"' AND z = '"+z+"' AND world = '"+world+"'");
		
		//Refund if necessary
		if(plugin.getConfig().getBoolean("shop.refund")){
			plugin.getEcon().deposit(this.getOwner(), plugin.getConfig().getDouble("shop.cost"));
		}
		
		if(fromMemory){
			//Delete it from memory
			plugin.getShopManager().removeShop(this);
		}
	}
	
	public boolean isValid(){
		checkDisplay();
		
		return Util.canBeShop(this.getLocation().getBlock());
	}
	
	private void checkDisplay(){
		if(plugin.display == false) return;
		if(getLocation().getWorld() == null) return; //not loaded
		
		boolean trans = Util.isTransparent(getLocation().clone().add(0.5, 1.2, 0.5).getBlock().getType());
		
		if(trans && this.getDisplayItem() == null){
			this.displayItem = new DisplayItem(this, this.getItem());
			this.getDisplayItem().spawn();
		}
		
		if(this.getDisplayItem() != null){
			if(!trans){ //We have a display item in a block... delete it
				this.getDisplayItem().remove();
				this.displayItem = null;
				return;
			}
		
			DisplayItem disItem = this.getDisplayItem();
			Location dispLoc = disItem.getDisplayLocation();
			
			if(dispLoc.getBlock() != null && dispLoc.getBlock().getType() == Material.WATER){ //Flowing water.  Stationery water does not move items.
				disItem.remove();
				return;
			}
			if(disItem.getItem() == null){
				disItem.removeDupe();
				disItem.spawn();
				return;
			}
			
			Item item = disItem.getItem();
			
			if(item.getTicksLived() > 5000 || !item.isValid() || item.isDead()){
				disItem.respawn();
				disItem.removeDupe();
			}
			else if(item.getLocation().distanceSquared(dispLoc) > 1){
				item.teleport(dispLoc, TeleportCause.PLUGIN);
			}
		}
	}
	
	public void onUnload(){
		if(this.getDisplayItem() != null){
			this.getDisplayItem().remove();
			this.displayItem = null;
		}
	}
	public void onLoad(){
		checkDisplay();
	}
	public void onClick(){
		this.setSignText();
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder("Shop " + (loc.getWorld() == null ? "unloaded world" : loc.getWorld().getName()) + "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")");
		sb.append(" Owner: " + getOwner());
		if(isUnlimited()) sb.append(" Unlimited: true");
		sb.append(" Price: " + getPrice());
		sb.append( "Item: " + getItem().toString());
		
		return sb.toString();
	}
}