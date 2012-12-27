package org.maxgamer.QuickShop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.maxgamer.QuickShop.Command.QS;
import org.maxgamer.QuickShop.Database.Database;
import org.maxgamer.QuickShop.Economy.Economy;
import org.maxgamer.QuickShop.Economy.Economy_Core;
import org.maxgamer.QuickShop.Economy.Economy_Vault;
import org.maxgamer.QuickShop.Listeners.*;
import org.maxgamer.QuickShop.Shop.Shop;
import org.maxgamer.QuickShop.Shop.Shop.ShopType;
import org.maxgamer.QuickShop.Shop.ShopChunk;
import org.maxgamer.QuickShop.Watcher.*;

public class QuickShop extends JavaPlugin{
	public static QuickShop instance;
	private Economy economy;
	private ShopManager shopManager;
	
	public boolean debug = false;
	public HashSet<String> warnings = new HashSet<String>(10);
	
	public boolean display = true;
	
	private Database database;
	
	//Listeners - We decide which one to use at runtime
	private ChatListener chatListener;
	private HeroChatListener heroChatListener;
	
	//Listeners (These don't)
	private BlockListener blockListener = new BlockListener(this);
	private PlayerListener playerListener = new PlayerListener(this);
	private ChunkListener chunkListener = new ChunkListener(this);
	private WorldListener worldListener = new WorldListener(this);
	
	//private int itemWatcherID;
	private BukkitTask itemWatcherTask;
	public boolean lock;
	public boolean sneak;
	
	private Metrics metrics;
	
	private LogWatcher logWatcher;
	
	public void onEnable(){
		instance = this;
		
		saveDefaultConfig(); //Creates the config folder and copies config.yml (If one doesn't exist) as required.
		reloadConfig(); //Reloads messages.yml too, aswell as config.yml and others.
		getConfig().options().copyDefaults(true); //Load defaults.
		

		if(loadEcon() == false) return;
		
		//Create the shop manager.
		this.shopManager = new ShopManager(this);
		
		if(this.display){
			// Display item handler thread
			getLogger().info("Starting item scheduler");
			ItemWatcher itemWatcher = new ItemWatcher(this);
			itemWatcherTask = Bukkit.getScheduler().runTaskTimer(this, itemWatcher, 600, 600);
		}
		
		if(this.getConfig().getBoolean("log-actions")){
			//Logger Handler
			this.logWatcher = new LogWatcher(this, new File(this.getDataFolder(), "qs.log"));
			logWatcher.task = Bukkit.getScheduler().runTaskTimerAsynchronously(this, this.logWatcher, 150, 150);
		}
		
		/* Start database - Also creates DB file. */
		this.database = new Database(new File(this.getDataFolder(), "shops.db"));
		
		/* Creates DB table 'shops' */
		if(!getDB().hasTable("shops")){
			try {
				shopManager.createShopsTable();
			} catch (SQLException e) {
				e.printStackTrace();
				getLogger().severe("Could not create shops table");
			}
		}
		if(!getDB().hasTable("messages")){
			try{
				shopManager.createMessagesTable();
			}
			catch (SQLException e) {
				e.printStackTrace();
				getLogger().severe("Could not create messages table");
			}
		}
		
		//Make the database up to date
		shopManager.checkColumns();
		
		/* Load shops from database to memory */
		int count = 0; //Shops count
		Connection con;
		try {
			getLogger().info("Loading shops from database...");
			
			if(database.hasColumn("shops", "itemString")){
				//Convert.
				try{
					this.convertDatabase_2_9();
				}
				catch(Exception e){
					e.printStackTrace();
					System.out.println("Could not convert shops. Exitting");
					return;
				}
			}
			
			con = database.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM shops");
			ResultSet rs = ps.executeQuery();
			while(rs.next()){
				int x = rs.getInt("x");
				int y = rs.getInt("y");
				int z = rs.getInt("z");
				World world = Bukkit.getWorld(rs.getString("world"));

				ItemStack item = Util.getItemStack(rs.getString("item"));
				
				String owner = rs.getString("owner");
				double price = rs.getDouble("price");
				Location loc = new Location(world, x, y, z);
				/* Delete invalid shops, if we know of any */
				if(world != null && loc.getBlock().getType() != Material.CHEST){
					getLogger().info("Shop is not a chest in " +rs.getString("world") + " at: " + x + ", " + y + ", " + z + ".  Removing from DB.");
					getDB().execute("DELETE FROM shops WHERE x = "+x+" AND y = "+y+" AND z = "+z+" AND world = '"+rs.getString("world")+"'");
				}
				
				int type = rs.getInt("type");
				
				Shop shop = new Shop(loc, price, item, owner);
				shop.setUnlimited(rs.getBoolean("unlimited"));
				
				shop.setShopType(ShopType.fromID(type));
				
				shopManager.addShop(rs.getString("world"), shop);
				count++;
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
			getLogger().severe("Could not load shops.");
		}
		getLogger().info("Loaded "+count+" shops.");
		
		//Register events
		getLogger().info("Registering Listeners");
		Bukkit.getServer().getPluginManager().registerEvents(blockListener, this);
		Bukkit.getServer().getPluginManager().registerEvents(playerListener, this);
		
		if(this.display){
			Bukkit.getServer().getPluginManager().registerEvents(chunkListener, this);
		}
		
		Bukkit.getServer().getPluginManager().registerEvents(worldListener, this);
		
		if(Bukkit.getPluginManager().getPlugin("Herochat") != null){
			this.getLogger().info("Found Herochat... Hooking!");
			this.heroChatListener = new HeroChatListener(this);
			Bukkit.getServer().getPluginManager().registerEvents(heroChatListener, this);
		}
		else{
			this.chatListener = new ChatListener(this);
			Bukkit.getServer().getPluginManager().registerEvents(chatListener, this);
		}
		
		//Command handlers
		QS commandExecutor = new QS(this);
		getCommand("qs").setExecutor(commandExecutor);
		
		if(getConfig().getInt("shop.find-distance") > 100){
			getLogger().severe("Shop.find-distance is TOO HIGH! This will cause you LAG! Pick a number under 100 or don't whinge.");
		}
		
		try{
			this.metrics = new Metrics(this);
			if(this.metrics.start()){
				getLogger().info("Metrics started.");
			}
		}
		catch(IOException e){
			getLogger().info("Could not start metrics.");
		}
	}
	/** Reloads QuickShops config */
	@Override
	public void reloadConfig(){
		super.reloadConfig();
		
		//Load quick variables
		this.display = this.getConfig().getBoolean("shop.display-items"); 
		this.sneak = this.getConfig().getBoolean("shop.sneak-only");
		this.lock = this.getConfig().getBoolean("shop.lock");
		
		MsgUtil.loadMessages();
	}
	
	public boolean loadEcon(){
		String econ = getConfig().getString("economy");
		if(econ == null || econ.isEmpty()) econ = "Vault";
		econ = econ.substring(0, 1).toUpperCase() + econ.substring(1).toLowerCase();
		Economy_Core core = null;
		try{
			getLogger().info("Hooking " + econ);
			Class<? extends Economy_Core> ecoClass = Class.forName("org.maxgamer.QuickShop.Economy.Economy_"+econ).asSubclass(Economy_Core.class);
			core = ecoClass.newInstance();
		}
		catch(NoClassDefFoundError e){
			e.printStackTrace();
			System.out.println("No such economy hook found: " + econ + ", using vault!");
			core = new Economy_Vault();
		}
		catch(ClassNotFoundException e){
			e.printStackTrace();
			System.out.println("No such economy hook found: " + econ + ", using vault!");
			core = new Economy_Vault();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		
		if(core == null || !core.isValid()){
			getLogger().severe("Economy is not valid!");
			getLogger().severe("QuickShop could not hook an economy!");
			getLogger().severe("QuickShop CANNOT start!");
			if(econ.equals("Vault")) getLogger().severe("(Does Vault have an Economy to hook into?!");
			return false;
		}
		else{
			this.economy = new Economy(core);
			return true;
		}
	}
	
	public void onDisable(){
		if(itemWatcherTask != null){
			itemWatcherTask.cancel();
		}
		if(logWatcher != null){
			//Bukkit.getScheduler().cancelTask(logWatcher.taskId);
			logWatcher.task.cancel();
			logWatcher.close(); //Closes the file
		}
		
		/* Remove all display items, and any dupes we can find */
		shopManager.clear();
		
		/* Empty the buffer */
		this.database.getDatabaseWatcher().run();
		this.warnings.clear();
		
		this.reloadConfig();
	}
	/**
	 * Returns the economy for moving currency around
	 * @return The economy for moving currency around
	 */
	public Economy_Core getEcon(){
		return economy;
	}
	
	/**
	 * Logs the given string to qs.log, if QuickShop is configured to do so.
	 * @param s The string to log. It will be prefixed with the date and time.
	 */
	public void log(String s){
		if(this.logWatcher == null) return;
		Date date = Calendar.getInstance().getTime();
		Timestamp time = new Timestamp(date.getTime());
		this.logWatcher.add("["+time.toString()+"] "+ s);
	}

	/**
	 * @return Returns the database handler for queries etc.
	 */
	public Database getDB(){
		return this.database;
	}
	
	/**
	 * Prints debug information if QuickShop is configured to do so.
	 * @param s The string to print.
	 */
	public void debug(String s){
		if(!debug) return;
		this.getLogger().info(ChatColor.YELLOW + "[Debug] " + s);
	}
	
	/**
	 * Returns the ShopManager.  This is used for fetching, adding and removing shops.
	 * @return The ShopManager.
	 */
	public ShopManager getShopManager(){
		return this.shopManager;
	}
	/** Converts the database to v 2.9 format. 
	 * @throws SQLException */
	public void convertDatabase_2_9() throws Exception{
		Connection con = database.getConnection();
		System.out.println("Converting shops...");
		//Step 1: Load existing shops.
		PreparedStatement ps = con.prepareStatement("SELECT * FROM shops");
		ResultSet rs = ps.executeQuery();
		int shops = 0;
		System.out.println("Loading shops...");
		while(rs.next()){
			int x = rs.getInt("x");
			int y = rs.getInt("y");
			int z = rs.getInt("z");
			World world = Bukkit.getWorld(rs.getString("world"));

			ItemStack item = Util.makeItem(rs.getString("itemString"));				
			
			String owner = rs.getString("owner");
			double price = rs.getDouble("price");
			Location loc = new Location(world, x, y, z);
			/* Delete invalid shops, if we know of any */
			if(world != null && loc.getBlock().getType() != Material.CHEST){
				getLogger().info("Shop is not a chest in " +rs.getString("world") + " at: " + x + ", " + y + ", " + z + ".  Removing from DB.");
				getDB().execute("DELETE FROM shops WHERE x = "+x+" AND y = "+y+" AND z = "+z+" AND world = '"+rs.getString("world")+"'");
			}
			
			int type = rs.getInt("type");
			Shop shop = new Shop(loc, price, item, owner);
			shop.setUnlimited(rs.getBoolean("unlimited"));
			shop.setShopType(ShopType.fromID(type));
			
			shopManager.addShop(rs.getString("world"), shop);
			shops++;
		}
		ps.close();
		rs.close();
		
		System.out.println("Loading complete. Backing up and deleting shops table...");
		//Step 2: Delete shops table.
		File existing = new File(this.getDataFolder(), "shops.db");
		File backup = new File(existing.getAbsolutePath() + ".bak");
		
		InputStream in = new FileInputStream(existing);
		OutputStream out = new FileOutputStream(backup);
		
		byte[] buf = new byte[1024];
		int len;
		while((len = in.read(buf)) > 0){
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
		
		ps = con.prepareStatement("DELETE FROM shops");
		ps.execute();
		ps.close();
		con.close();
		
		con = database.getConnection();
		ps = con.prepareStatement("DROP TABLE shops");
		ps.execute();
		ps.close();
		
		//Step 3: Create shops table.
		shopManager.createShopsTable();
		
		//Step 4: Export the new data into the table
		for(Entry<String, HashMap<ShopChunk, HashMap<Location, Shop>>> worlds : shopManager.getShops().entrySet()){
			String world = worlds.getKey();
			for(Entry<ShopChunk, HashMap<Location, Shop>> chunks : worlds.getValue().entrySet()){
				for(Shop shop : chunks.getValue().values()){
					ps = con.prepareStatement("INSERT INTO shops (owner, price, item, x, y, z, world, unlimited, type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
					ps.setString(1, shop.getOwner());
					ps.setDouble(2, shop.getPrice());
					
					ps.setString(3, Util.getNBTString(shop.getItem()));
					
					ps.setInt(4, shop.getLocation().getBlockX());
					ps.setInt(5, shop.getLocation().getBlockY());
					ps.setInt(6, shop.getLocation().getBlockZ());
					ps.setString(7, world); 
					ps.setInt(8, (shop.isUnlimited() ? 1 : 0));
					ps.setInt(9, ShopType.toID(shop.getShopType()));
					
					ps.execute();
					ps.close();
					
					shops--;
					if(shops % 10 == 0){
						System.out.println("Remaining: " + shops + " shops.");
					}
				}
			}
		}
		
		System.out.println("Conversion complete.");
	}
}