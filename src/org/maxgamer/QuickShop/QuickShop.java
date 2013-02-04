package org.maxgamer.QuickShop;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
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
import org.maxgamer.QuickShop.Metrics.Metrics;
import org.maxgamer.QuickShop.Metrics.ShopListener;
import org.maxgamer.QuickShop.Shop.*;
import org.maxgamer.QuickShop.Util.Converter;
import org.maxgamer.QuickShop.Util.Util;
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
	/** Whether we players are charged a fee to change the price on their shop (To help deter endless undercutting */
	public boolean priceChangeRequiresFee = false;
	
	
	/** Use SpoutPlugin to get item / block names */
	public boolean useSpout = false;
	
	private Metrics metrics;
	
	/** Whether debug info should be shown in the console */
	public boolean debug = false;
	
	/** The plugin metrics from Hidendra */
	public Metrics getMetrics(){ return metrics; }
	
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
			int res = Converter.convert();
			
			if(res < 0){
				System.out.println("Could not convert shops. Exitting.");
				return;
			}
			if(res > 0){
				System.out.println("Conversion success. Continuing...");
			}
			
			con = database.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM shops");
			ResultSet rs = ps.executeQuery();
			
			while(rs.next()){
				int x = 0;
				int y = 0;
				int z = 0;
				String worldName = null;
				try{
					x = rs.getInt("x");
					y = rs.getInt("y");
					z = rs.getInt("z");
					worldName = rs.getString("world");
					World world = Bukkit.getWorld(worldName);
					
					ItemStack item = Util.getItemStack(rs.getBytes("item"));
					
					String owner = rs.getString("owner");
					double price = rs.getDouble("price");
					Location loc = new Location(world, x, y, z);
					/* Skip invalid shops, if we know of any */
					if(world != null && loc.getBlock().getType() != Material.CHEST){
						getLogger().info("Shop is not a chest in " +rs.getString("world") + " at: " + x + ", " + y + ", " + z + ".  Deleting.");
						PreparedStatement delps = getDB().getConnection().prepareStatement("DELETE FROM shops WHERE x = ? AND y = ? and z = ? and world = ?");
						delps.setInt(1, x);
						delps.setInt(2, y);
						delps.setInt(3, z);
						delps.setString(4, worldName);
						
						delps.execute();
						continue;
					}
					
					int type = rs.getInt("type");
					
					Shop shop = new ChestShop(loc, price, item, owner);
					shop.setUnlimited(rs.getBoolean("unlimited"));
					
					shop.setShopType(ShopType.fromID(type));
					
					shopManager.loadShop(rs.getString("world"), shop);
					
					if(loc.getWorld() != null && loc.getChunk().isLoaded()){
						shop.onLoad();
					}
					
					count++;
				}
				catch(ClassNotFoundException e){
					e.printStackTrace();
					getLogger().severe("This version of QuickShop is incompatible with this version of bukkit!");
				}
				catch(Exception e){
					e.printStackTrace();
					getLogger().severe("Error loading a shop! Coords: "+worldName+" (" + x + ", " + y + ", " + z + ") - Skipping it...");
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
			getLogger().severe("Shop.find-distance is too high! Pick a number under 100!");
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
			
			if(metrics.isOptOut() == false){
				getServer().getPluginManager().registerEvents(new ShopListener(), this);
				if(this.metrics.start()){
					getLogger().info("Metrics started.");
				}
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
		this.priceChangeRequiresFee = this.getConfig().getBoolean("shop.price-change-requires-fee");
		
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
}