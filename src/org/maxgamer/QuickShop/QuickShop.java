package org.maxgamer.QuickShop;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.maxgamer.QuickShop.Database.Database;
import org.maxgamer.QuickShop.Listeners.BlockListener;
import org.maxgamer.QuickShop.Listeners.ChatListener;
import org.maxgamer.QuickShop.Listeners.ChunkListener;
import org.maxgamer.QuickShop.Listeners.ClickListener;
import org.maxgamer.QuickShop.Listeners.MoveListener;
//import org.maxgamer.QuickShop.Listeners.PickupListener;

public class QuickShop extends JavaPlugin{
	private Economy economy;
	private HashMap<Location, Shop> shops = new HashMap<Location, Shop>(30);
	private HashMap<String, Info> actions = new HashMap<String, Info>(30);
	private HashSet<Material> tools = new HashSet<Material>(50);
	
	private HashMap<Shop, Item> spawnedItems = new HashMap<Shop, Item>(30);
	
	private Database database;
	public HashSet<String> queries = new HashSet<String>(5);
	public boolean queriesInUse = false;
	
	private ChatListener chatListener = new ChatListener(this);
	private ClickListener clickListener = new ClickListener(this);
	private BlockListener blockListener = new BlockListener(this);
	private MoveListener moveListener = new MoveListener(this);
	private ChunkListener chunkListener = new ChunkListener(this);
	//private PickupListener pickupListener = new PickupListener(this);
	
	public void onEnable(){
		getLogger().info("Hooking Vault");
		setupEconomy();
		getLogger().info("Registering Listeners");
		Bukkit.getServer().getPluginManager().registerEvents(chatListener, this);
		Bukkit.getServer().getPluginManager().registerEvents(clickListener, this);
		Bukkit.getServer().getPluginManager().registerEvents(blockListener, this);
		Bukkit.getServer().getPluginManager().registerEvents(moveListener, this);
		Bukkit.getServer().getPluginManager().registerEvents(chunkListener, this);
		//Bukkit.getServer().getPluginManager().registerEvents(pickupListener, this);
		
		if(!this.getDataFolder().exists()){
			this.getDataFolder().mkdir();
		}
		File configFile = new File(this.getDataFolder(), "config.yml");
		if(!configFile.exists()){
			this.saveConfig();
		}
		this.getConfig().options().copyDefaults(true);
		
		this.database = new Database(this, this.getDataFolder() + File.separator + "shops.db");
		
		getLogger().info("Loading tools");
		loadTools();
		
		getLogger().info("Starting item scheduler");
		
		getDB().getConnection();
		
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){
			@Override
			public void run() {
				for(Entry<Shop, Item> entry : spawnedItems.entrySet()){
					entry.getValue().setTicksLived(1);
				}
			}
			
		}, 0, 20);
		
		if(!getDB().hasTable()){
			try {
				getDB().createTable();
			} catch (SQLException e) {
				e.printStackTrace();
				getLogger().severe("Could not create database table");
			}
		}
		
		Bukkit.getScheduler().scheduleAsyncRepeatingTask(this, new Runnable(){

			@Override
			public void run() {
				QuickShop plugin = (QuickShop) Bukkit.getPluginManager().getPlugin("QuickShop");
				
				Database db = plugin.getDB();
				
				Connection con = db.getConnection();
				plugin.getLogger().info("Updating DB");
				try {
					Statement st = con.createStatement();
					while(plugin.queriesInUse){
						//Nothing
					}
					//plugin.getLogger().info("Locking queries buffer");
					//long t1 = System.nanoTime();
					plugin.queriesInUse = true;
					for(String q : plugin.queries){
						st.addBatch(q);
					}
					plugin.queries.clear();
					plugin.queriesInUse = false;
					//long t2 = System.nanoTime();
					//plugin.getLogger().info("Unlocked queries buffer");
					//plugin.getLogger().info(t2 - t1 + " nanoseconds locked.");
					st.executeBatch();
					st.close();
					
				} catch (SQLException e) {
					e.printStackTrace();
					plugin.getLogger().severe("Could not execute query");
				}
				
			}
			
		}, 300, 300);
		
		
		Connection con = database.getConnection();
		try {
			PreparedStatement ps = con.prepareStatement("SELECT * FROM shops");
			ResultSet rs = ps.executeQuery();
			while(rs.next()){
				int x = rs.getInt("x");
				int y = rs.getInt("y");
				int z = rs.getInt("z");
				World world = Bukkit.getWorld(rs.getString("world"));
				
				ItemStack item = this.makeItem(rs.getString("itemString"));				
				
				String owner = rs.getString("owner");
				double price = rs.getDouble("price");
				Location loc = new Location(world, x, y, z);
				
				if(loc.getBlock().getType() != Material.CHEST){
					getLogger().info("Shop is not a chest at: " + x + ", " + y + ", " + z);
					//ToDo:  Delete the entry from the database.
					continue;
				}
				
				Shop shop = new Shop(loc, price, item, owner);
				
				this.getShops().put(loc, shop);
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
			getLogger().severe("Could not load shops.");
		}
		
	}
	public void onDisable(){
		for(Shop shop : shops.values()){
			shop.deleteDisplayItem();
		}
	}
	public Economy getEcon(){
		return economy;
	}
	public HashMap<Location, Shop> getShops(){
		return this.shops;
	}
	/**
	 * @return Returns the HashMap<Player name, shopInfo>. Info contains what their last question etc was.
	 */
	public HashMap<String, Info> getActions(){
		return this.actions;
	}
	
	/**
	 * Sets up the vault economy for hooking into & purchases.
	 * @return True is success
	 */
	private boolean setupEconomy(){
	        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
	        if (economyProvider != null) {
	            economy = economyProvider.getProvider();
	        }

	        return (economy != null);
    }
	
	 /**
	  * Fetches a shop in a particular location, or null.
	  * @param loc The location to check.
	  * @return The shop at the location.
	  */
	public Shop getShop(Location loc){
		return this.shops.get(loc);
	}
	/**
	 * @param mat The material to check
	 * @return Returns true if the item is a tool (Has durability) or false if it doesn't.
	 */
	public boolean isTool(Material mat){
		return this.tools.contains(mat);
	}
	/**
	 * @return Returns the database handler for queries etc.
	 */
	public Database getDB(){
		return this.database;
	}
	 
	 /**
	  * Gets the percentage (Without trailing %) damage on a tool.
	  * @param item The ItemStack of tools to check
	  * @return The percentage 'health' the tool has. (Opposite of total damage)
	  */
	public String getToolPercentage(ItemStack item){
		double dura = item.getDurability();
		double max = item.getType().getMaxDurability();
		
		DecimalFormat formatter = new DecimalFormat("0");
		return formatter.format((1 - dura/max)* 100.0);
	}
	
	public boolean isProtectedItem(Item item){
		return this.spawnedItems.containsValue(item);
	}
	
	/**
	 * @return Returns a hashmap<shop, item> of the protected items.
	 */
	public HashMap<Shop, Item> getProtectedItems(){
		return this.spawnedItems;
	}
	/**
	 * Converts a string into an item from the database.
	 * @param itemString The database string.  Is the result of makeString(ItemStack item).
	 * @return A new itemstack, with the properties given in the string
	 */
	public ItemStack makeItem(String itemString){
		String[] itemInfo = itemString.split(":");
		
		ItemStack item = new ItemStack(Material.getMaterial(itemInfo[0]));
		MaterialData data = new MaterialData(Integer.parseInt(itemInfo[1]));
		item.setData(data);
		item.setDurability( Short.parseShort(itemInfo[2]));
		item.setAmount(Integer.parseInt(itemInfo[3]));
		
		for(int i = 4; i < itemInfo.length; i = i + 2){
			item.addEnchantment(Enchantment.getByName(itemInfo[i]), Integer.parseInt(itemInfo[i+1]));
		}
		return item;
	}
	
	/**
	 * Converts an itemstack into a string for database storage.  See makeItem(String itemString) for 
	 * reversing this.
	 * @param item The item to model it off of.
	 * @return A new string with the properties of the item.
	 */
	public String makeString(ItemStack item){
		String itemString = item.getType().toString() + ":" + item.getData().getData() + ":" + item.getDurability() + ":" + item.getAmount() + ":";
		
		for(Entry<Enchantment, Integer> ench : item.getEnchantments().entrySet()){
			itemString += ench.getKey().getName() + ":" + ench.getValue() + ":";
		}
		return itemString;
	}
	
	public void loadTools(){
		tools.add(Material.BOW);
		tools.add(Material.SHEARS);
		tools.add(Material.FISHING_ROD);
		tools.add(Material.FLINT_AND_STEEL);

		tools.add(Material.CHAINMAIL_BOOTS);
		tools.add(Material.CHAINMAIL_CHESTPLATE);
		tools.add(Material.CHAINMAIL_HELMET);
		tools.add(Material.CHAINMAIL_LEGGINGS);
		
		tools.add(Material.WOOD_AXE);
		tools.add(Material.WOOD_HOE);
		tools.add(Material.WOOD_PICKAXE);
		tools.add(Material.WOOD_SPADE);
		tools.add(Material.WOOD_SWORD);
		
		tools.add(Material.LEATHER_BOOTS);
		tools.add(Material.LEATHER_CHESTPLATE);
		tools.add(Material.LEATHER_HELMET);
		tools.add(Material.LEATHER_LEGGINGS);
		
		tools.add(Material.DIAMOND_AXE); 
		tools.add(Material.DIAMOND_HOE);
		tools.add(Material.DIAMOND_PICKAXE);
		tools.add(Material.DIAMOND_SPADE);
		tools.add(Material.DIAMOND_SWORD);

		tools.add(Material.DIAMOND_BOOTS);
		tools.add(Material.DIAMOND_CHESTPLATE);
		tools.add(Material.DIAMOND_HELMET);
		tools.add(Material.DIAMOND_LEGGINGS);
		tools.add(Material.STONE_AXE); 
		tools.add(Material.STONE_HOE);
		tools.add(Material.STONE_PICKAXE);
		tools.add(Material.STONE_SPADE);
		tools.add(Material.STONE_SWORD);

		tools.add(Material.GOLD_AXE); 
		tools.add(Material.GOLD_HOE);
		tools.add(Material.GOLD_PICKAXE);
		tools.add(Material.GOLD_SPADE);
		tools.add(Material.GOLD_SWORD);

		tools.add(Material.GOLD_BOOTS);
		tools.add(Material.GOLD_CHESTPLATE);
		tools.add(Material.GOLD_HELMET);
		tools.add(Material.GOLD_LEGGINGS);
		tools.add(Material.IRON_AXE); 
		tools.add(Material.IRON_HOE);
		tools.add(Material.IRON_PICKAXE);
		tools.add(Material.IRON_SPADE);
		tools.add(Material.IRON_SWORD);

		tools.add(Material.IRON_BOOTS);
		tools.add(Material.IRON_CHESTPLATE);
		tools.add(Material.IRON_HELMET);
		tools.add(Material.IRON_LEGGINGS);
	}
}