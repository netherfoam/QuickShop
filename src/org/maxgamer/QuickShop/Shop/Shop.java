package org.maxgamer.QuickShop.Shop;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.maxgamer.QuickShop.QuickShop;

public class Shop{
	private Location loc;
	private double price;
	private String owner;
	private ItemStack item;
	private DisplayItem displayItem;
	private boolean unlimited;
	private ShopType shopType;
	
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
		this.displayItem = new DisplayItem(plugin, this, this.item);
		
		this.shopType = ShopType.SELLING;
	}
	/**
	 * Returns the number of items this shop has in stock.
	 * @return The number of items available for purchase.
	 */
	public int getRemainingStock(){
		if(this.unlimited) return 9999;
		
		Chest chest = (Chest) loc.getBlock().getState();
		int stock = 0;
		
		ItemStack[] in = chest.getInventory().getContents();
		for(ItemStack item : in){
			if(this.matches(item)){
				stock = stock + item.getAmount();
			}
		}
		
		return stock;
	}
	
	/**
	 * Returns the number of free spots in the chest for the particular item.
	 * @param stackSize
	 * @return
	 */
	public int getRemainingSpace(int stackSize){
		if(this.unlimited) return 9999;
		
		Chest chest = (Chest) loc.getBlock().getState();
		int space = 0;
		
		ItemStack[] inv = chest.getInventory().getContents();
		for(ItemStack item : inv){
			if(item == null || item.getType() == Material.AIR || item.getAmount() == 0){
				space += stackSize;
			}
			else if(this.matches(item)){
				space += stackSize - item.getAmount();
			}
		}
		
		return space;
	}
	/**
	 * Returns true if the ItemStack matches what this shop is selling/buying
	 * @param item The ItemStack
	 * @return True if the ItemStack is the same (Excludes amounts)
	 */
	public boolean matches(ItemStack item){
		return (item != null && item.getType() == getMaterial() && item.getDurability() == getDurability() && item.getEnchantments().equals(getEnchants()));
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
	 * @return The ItemStack type of this shop
	 */
	public Material getMaterial(){
		return this.item.getType();
	}
	
	/**
	 * Updates the shop in the database.
	 * @param isNew True if this shop is not in the database yet
	 */
	public void update(boolean isNew){
		int x = this.getLocation().getBlockX();
		int y = this.getLocation().getBlockY();
		int z = this.getLocation().getBlockZ();
		
		String world = this.getLocation().getWorld().getName();
		
		int unlimited = this.isUnlimited() ? 1 : 0;

		String q = "";
		if(isNew){
			q = "INSERT INTO shops VALUES ('"+this.getOwner()+"', '"+this.getPrice()+"', '"+plugin.makeString(this.item)+"', '"+x+"', '"+y+"', '"+z+"', '"+world+"', '"+unlimited+"', '"+ShopType.toID(this.shopType)+"')";
		}
		else{
			q = "UPDATE shops SET owner = '"+this.getOwner()+"', itemString = '"+plugin.makeString(this.item)+"', unlimited = '"+unlimited+"', type = '"+ShopType.toID(this.shopType)+"'  WHERE x = '"+x+"' AND y = '"+y+"' AND z = '"+z+"' AND world = '"+world+"'";  
		}
		
		plugin.getDB().writeToBuffer(q);
	}
	
	/**
	 * Upates the shop into the database.
	 */
	public void update(){
		this.update(false);
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
		if(this.unlimited) return;
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
	 * Add an item from to shop.
	 * @param item The itemstack.  The amount does not matter, just everything else
	 * @param amount The amount to add to the shop.
	 */
	public void add(ItemStack item, int amount){
		if(this.unlimited) return;
		
		Inventory inv = this.getChest().getInventory();
		
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
	 * @param item The itemStack to sell
	 * @param amount The amount to sell
	 */
	public void sell(Player p, ItemStack item, int amount){
		if(amount < 0) this.buy(p, item, -amount);
		//Items to drop on floor
		HashMap<Integer, ItemStack> floor = new HashMap<Integer, ItemStack>(30);
		//We do NOT want to modify this
		ItemStack transfer = item.clone();
		
		if(!this.isUnlimited()){
			this.remove(transfer, amount);
		}
		
		while(amount > 0){
			int stackSize = Math.min(amount, transfer.getMaxStackSize());
			if(stackSize == -1){
				stackSize = amount;
			}
			
			transfer.setAmount(stackSize);
			
			//Give the player the items.
			//Store the leftover items they didn't have room for
			floor.putAll(p.getInventory().addItem(transfer));
			amount -= stackSize;
		}
		
		//Drop the remainder on the floor.
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
	public void buy(Player p, ItemStack item, int amount){
		if(amount < 0) this.sell(p, item, -amount);
		//We do NOT want to modify this
		ItemStack transfer = item.clone();
		
		transfer.setAmount(amount);
		p.getInventory().removeItem(transfer);
		
		while(amount > 0 && !this.isUnlimited()){
			int stackSize = Math.min(amount, transfer.getMaxStackSize());
			if(stackSize == -1){
				stackSize = amount;
			}
			
			transfer.setAmount(stackSize);
			
			this.add(transfer, amount);
			
			amount -= stackSize;
		}
	}
	
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
	
	public enum ShopType{
		SELLING(),
		BUYING();
		public static ShopType fromID(int id){
			if(id == 0){
				return ShopType.SELLING;
			}
			if(id == 1){
				return ShopType.BUYING;
			}
			return null;
		}
		public static int toID(ShopType shopType){
			if(shopType == ShopType.SELLING){
				return 0;
			}
			if(shopType == ShopType.BUYING){
				return 1;
			}
			else{
				return -1;
			}
		}
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
	 * Changes a shop type to Buying or Selling. Also updates the signs nearby
	 * @param shopType The new type (ShopType.BUYING or ShopType.SELLING)
	 */
	public void setShopType(ShopType shopType){
		this.shopType = shopType;
	}
	
	/**
	 * Updates signs
	 */
	public void setSignText(){
		String[] lines = new String[3];
		lines[0] = ChatColor.RED + "[QuickShop]";
		if(this.isBuying()){
			lines[1] = "Buying:";
		}
		if(this.isSelling()){
			lines[1] = "Selling:";
		}
		lines[2] = plugin.getDataName(this.getMaterial(), this.getDurability());
		lines[3] = "For " + this.price + " each";
		this.setSignText(lines);
	}
	
	/**
	 * Changes all lines of text on a sign near the shop
	 * @param lines The array of lines to change. Index is line number.
	 */
	public void setSignText(String[] lines){
		Block[] blocks = new Block[4];
		blocks[0] = loc.getBlock().getRelative(1, 0, 0);
		blocks[1] = loc.getBlock().getRelative(-1, 0, 0);
		blocks[2] = loc.getBlock().getRelative(0, 0, 1);
		blocks[3] = loc.getBlock().getRelative(0, 0, -1);
		
		for(Block b : blocks){
			if(b.getType() != Material.WALL_SIGN) continue;
			Sign sign = (Sign) b.getState();
			
			for(int i = 0; i < lines.length; i++){
				sign.setLine(i, lines[i]);
			}
			
			sign.update(true);
		}
	}
	
	public String getDataName(){
		return plugin.getDataName(this.getMaterial(), this.getDurability());
	}
	
	/**
	 * Deletes the shop from the list of shops
	 * and queues it for database deletion
	 */
	public void delete(){
		//Delete the display item
		this.getDisplayItem().remove();
		
		//Delete the signs around it
		Block[] blocks = new Block[4];
		blocks[0] = loc.getBlock().getRelative(1, 0, 0);
		blocks[1] = loc.getBlock().getRelative(-1, 0, 0);
		blocks[2] = loc.getBlock().getRelative(0, 0, 1);
		blocks[3] = loc.getBlock().getRelative(0, 0, -1);
		
		for(Block b : blocks){
			if(b.getType() == Material.WALL_SIGN){
				b.setType(Material.AIR);
			}
		}
		
		//Delete it from the database
		int x = this.getLocation().getBlockX();
		int y = this.getLocation().getBlockY();
		int z = this.getLocation().getBlockZ();
		String world = this.getLocation().getWorld().getName();
		plugin.getDB().writeToBuffer("DELETE FROM shops WHERE x = '"+x+"' AND y = '"+y+"' AND z = '"+z+"' AND world = '"+world+"'");
		
		//Refund if necessary
		if(plugin.getConfig().getBoolean("shop.refund")){
			plugin.getEcon().depositPlayer(this.getOwner(), plugin.getConfig().getDouble("shop.cost"));
		}
		
		//Delete it from memory
		plugin.getShops().remove(this.getLocation());
	}
}