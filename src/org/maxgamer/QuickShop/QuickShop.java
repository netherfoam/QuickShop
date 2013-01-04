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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.maxgamer.QuickShop.Command.QS;
import org.maxgamer.QuickShop.Database.Database;
import org.maxgamer.QuickShop.Economy.*;
import org.maxgamer.QuickShop.Listeners.*;
import org.maxgamer.QuickShop.Shop.*;
import org.maxgamer.QuickShop.Shop.Shop.ShopType;
import org.maxgamer.QuickShop.Watcher.*;

public class QuickShop extends JavaPlugin{
	/** The active instance of QuickShop */
	public static QuickShop instance;
	
	/** The economy we hook into for transactions */
	private Economy economy;
	
	/** The Shop Manager used to store shops */
	private ShopManager shopManager;
	
	/** A set of players who have been warned ("Your shop isn't automatically locked") */
	public HashSet<String> warnings = new HashSet<String>(10);
	
	/** The database for storing all our data for persistence */
	private Database database;
	
	//Listeners - We decide which one to use at runtime
	private ChatListener chatListener;
	private HeroChatListener heroChatListener;
	
	//Listeners (These don't)
	private BlockListener blockListener = new BlockListener(this);
	private PlayerListener playerListener = new PlayerListener(this);
	private ChunkListener chunkListener = new ChunkListener(this);
	private WorldListener worldListener = new WorldListener(this);
	
	private BukkitTask itemWatcherTask;
	private LogWatcher logWatcher;
	
	/** Whether shops should be locked from other players opening them */
	public boolean lock;
	/** Whether players are required to sneak to create a shop */
	public boolean sneak;
	/** Whether we should use display items or not */
	public boolean display = true;
	
	/** Use SpoutPlugin to get item / block names */
	public boolean useSpout = false;
	
	private Metrics metrics;
	
	/** Whether debug info should be shown in the console */
	public boolean debug = false;
	
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
		
		
		ConfigurationSection dbCfg = getConfig().getConfigurationSection("database");
		if(dbCfg.getBoolean("mysql")){
			//MySQL database - Required database be created first.
			String user = dbCfg.getString("user");
			String pass = dbCfg.getString("password");
			String host = dbCfg.getString("host");
			String port = dbCfg.getString("port");
			String database = dbCfg.getString("database");
			
			this.database = new Database(host, port, database, user, pass);
		}
		else{
			//SQLite database - Doing this handles file creation
			this.database = new Database(new File(this.getDataFolder(), "shops.db"));
		}
		
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
					System.out.println("Could not convert shops to 2.9. Exitting");
					return;
				}
			}
			
			con = database.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM shops");
			ResultSet rs = ps.executeQuery();
			
			String colType = rs.getMetaData().getColumnTypeName(3);
			if(!colType.equalsIgnoreCase("BLOB")){
				System.out.println("Item column type: " + colType + ", converting to BLOB.");
				
				//We're using the old format
				try{
					this.convertDatabase_3_4();
				}
				catch(Exception e){
					e.printStackTrace();
					System.out.println("Could not convert shops to 3.4, exitting!");
					return;
				}
				
				ps.close();
				rs.close();
				
				//Try again.
				con = database.getConnection();
				ps = con.prepareStatement("SELECT * FROM shops");
				rs = ps.executeQuery();
			}
			
			while(rs.next()){
				try{
					int x = rs.getInt("x");
					int y = rs.getInt("y");
					int z = rs.getInt("z");
					World world = Bukkit.getWorld(rs.getString("world"));
	
					ItemStack item = Util.getItemStack(rs.getBytes("item"));
					
					String owner = rs.getString("owner");
					double price = rs.getDouble("price");
					Location loc = new Location(world, x, y, z);
					/* Delete invalid shops, if we know of any */
					if(world != null && loc.getBlock().getType() != Material.CHEST){
						getLogger().info("Shop is not a chest in " +rs.getString("world") + " at: " + x + ", " + y + ", " + z + ".  Removing from DB.");
						//getDB().execute("DELETE FROM shops WHERE x = "+x+" AND y = "+y+" AND z = "+z+" AND world = '"+rs.getString("world")+"'");
						PreparedStatement delps = getDB().getConnection().prepareStatement("DELETE FROM shops WHERE x = ? AND y = ? and z = ? and world = ?");
						delps.setInt(1, x);
						delps.setInt(2, y);
						delps.setInt(3, z);
						delps.setString(4, rs.getString("world"));
						
						delps.execute();
						continue;
					}
					
					int type = rs.getInt("type");
					
					Shop shop = new Shop(loc, price, item, owner);
					shop.setUnlimited(rs.getBoolean("unlimited"));
					
					shop.setShopType(ShopType.fromID(type));
					
					shopManager.loadShop(rs.getString("world"), shop);
					count++;
				}
				catch(ClassNotFoundException e){
					e.printStackTrace();
					getLogger().severe("This version of QuickShop is incompatible with this version of bukkit!");
				}
				catch(Exception e){
					e.printStackTrace();
					getLogger().severe("Error loading a shop! Skipping it...");
				}
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
			getLogger().severe("Could not load shops.");
		}
		getLogger().info("Loaded "+count+" shops.");
		
		MsgUtil.loadTransactionMessages();
		MsgUtil.clean();
		
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
		
		/**
		 * If the server has Spout we can get the names of custom items.
		 * Latest SpoutPlugin http://get.spout.org/1412/SpoutPlugin.jar
		 * http://build.spout.org/view/Legacy/job/SpoutPlugin/1412/ 
		 */
		if(Bukkit.getPluginManager().getPlugin("Spout") != null){
			this.getLogger().info("Found Spout...");
			this.useSpout = true;
		}
		else{
			this.useSpout = false;
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
		
		getLogger().info("QuickShop loaded!");
	}
	/** Reloads QuickShops config */
	@Override
	public void reloadConfig(){
		super.reloadConfig();
		
		//Load quick variables
		this.display = this.getConfig().getBoolean("shop.display-items"); 
		this.sneak = this.getConfig().getBoolean("shop.sneak-only");
		this.lock = this.getConfig().getBoolean("shop.lock");
		
		MsgUtil.loadCfgMessages();
	}
	
	/**
	 * Tries to load the economy and its core.  If this fails, it will try to use vault. If that fails, it will return false.
	 * @return true if successful, false if the core is invalid or is not found, and vault cannot be used.
	 */
	public boolean loadEcon(){
		String econ = getConfig().getString("economy");
		//Fall back to vault if none specified
		if(econ == null || econ.isEmpty()) econ = "Vault";
		//Capitalize the first letter, lowercase the rest
		econ = econ.substring(0, 1).toUpperCase() + econ.substring(1).toLowerCase();
		
		//The core to use
		EconomyCore core = null;
		try{
			getLogger().info("Hooking " + econ);
			//Throws ClassNotFoundException if they gave us the wrong economy
			Class<? extends EconomyCore> ecoClass = Class.forName("org.maxgamer.QuickShop.Economy.Economy_"+econ).asSubclass(EconomyCore.class);
			//Throws NoClassDefFoundError if the economy is not installed
			core = ecoClass.newInstance();
		}
		catch(NoClassDefFoundError e){
			//Thrown because the plugin backend is not installed
			e.printStackTrace();
			System.out.println("Could not find economy called " + econ + "... Is it installed? Using Vault instead!");
			core = new Economy_Vault();
		}
		catch(ClassNotFoundException e){
			//Thrown because we don't have a bridge for that plugin
			e.printStackTrace();
			System.out.println("QuickShop does not know how to hook into " + econ + "! Using Vault instead!");
			core = new Economy_Vault();
		} catch (InstantiationException e) {
			//Should not be thrown
			e.printStackTrace();
			System.out.println("Invalid Economy Core! " + econ);
			return false;
		} catch (IllegalAccessException e) {
			//Should not be thrown
			e.printStackTrace();
			System.out.println("Invalid Economy Core! " + econ);
			return false;
		}
		
		if(core == null || !core.isValid()){
			getLogger().severe("Economy is not valid!");
			getLogger().severe("QuickShop could not hook an economy!");
			getLogger().severe("QuickShop CANNOT start!");
			if(econ.equals("Vault")) getLogger().severe("(Does Vault have an Economy to hook into?!)");
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
		if(database.getTask() != null){
			this.database.getTask().cancel();
			this.database.setTask(null);
			this.database.getDatabaseWatcher().run();
		}
		
		try {
			this.database.getConnection().close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		this.warnings.clear();
		
		this.reloadConfig();
	}
	/**
	 * Returns the economy for moving currency around
	 * @return The economy for moving currency around
	 */
	public EconomyCore getEcon(){
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
		System.out.println("Converting shops to 2.9 format...");
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
			
			int type = rs.getInt("type");
			Shop shop = new Shop(loc, price, item, owner);
			shop.setUnlimited(rs.getBoolean("unlimited"));
			shop.setShopType(ShopType.fromID(type));
			
			shopManager.loadShop(rs.getString("world"), shop);
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
					
					//Use the old setString, because it is still below v3.4
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
	
	/** Converts the database to v 3.4 format. 
	 * @throws SQLException */
	public void convertDatabase_3_4() throws Exception{
		Connection con = database.getConnection();
		System.out.println("Converting shops to 3.4 format...");
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

			//ItemStack item = Util.makeItem(rs.getString("item"));
			ItemStack item = Util.getItemStack(rs.getString("item"));
			
			String owner = rs.getString("owner");
			double price = rs.getDouble("price");
			Location loc = new Location(world, x, y, z);
			
			int type = rs.getInt("type");
			Shop shop = new Shop(loc, price, item, owner);
			shop.setUnlimited(rs.getBoolean("unlimited"));
			shop.setShopType(ShopType.fromID(type));
			
			shopManager.loadShop(rs.getString("world"), shop);
			shops++;
		}
		ps.close();
		rs.close();
		
		System.out.println("Loading complete. Backing up and deleting shops table...");
		//Step 2: Delete shops table.
		File existing = new File(this.getDataFolder(), "shops.db");
		File backup = new File(existing.getAbsolutePath() + ".bak2");
		
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
					
					ps.setBytes(3, Util.getNBTBytes(shop.getItem()));
					
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