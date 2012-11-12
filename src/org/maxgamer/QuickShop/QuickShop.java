package org.maxgamer.QuickShop;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.maxgamer.QuickShop.Command.QS;
import org.maxgamer.QuickShop.Database.Database;
import org.maxgamer.QuickShop.Listeners.BlockListener;
import org.maxgamer.QuickShop.Listeners.ChatListener;
import org.maxgamer.QuickShop.Listeners.ChunkListener;
import org.maxgamer.QuickShop.Listeners.ClickListener;
import org.maxgamer.QuickShop.Listeners.HeroChatListener;
import org.maxgamer.QuickShop.Listeners.JoinListener;
import org.maxgamer.QuickShop.Listeners.MoveListener;
import org.maxgamer.QuickShop.Listeners.QuitListener;
import org.maxgamer.QuickShop.Listeners.WorldListener;
import org.maxgamer.QuickShop.Shop.Info;
import org.maxgamer.QuickShop.Shop.Shop;
import org.maxgamer.QuickShop.Shop.Shop.ShopType;
import org.maxgamer.QuickShop.Watcher.BufferWatcher;
import org.maxgamer.QuickShop.Watcher.ItemWatcher;
import org.maxgamer.QuickShop.Watcher.LogWatcher;

import net.milkbowl.vault.economy.Economy;

public class QuickShop extends JavaPlugin{
	private Economy economy;
	private ShopManager shopManager;
	
	private HashMap<String, Info> actions = new HashMap<String, Info>(30);
	private HashSet<Material> tools = new HashSet<Material>(50);
	public boolean debug = false;
	public HashSet<String> warnings = new HashSet<String>(10);
	
	public boolean display = true;
	
	private Database database;
	
	public YamlConfiguration messages;
	
	/* Hooking into plugins */
	/*
	//PreciousStones
	private PreciousStones preciousStones;
	//Towny
	private Towny towny;
	//Residence
	private Residence residence;
	//WorldGuard
	private WorldGuardPlugin worldGuardPlugin;
	//GriefPrevention
	private GriefPrevention griefPrevention; 
	//Lockette
	private Lockette lockette;
	//LWC
	private LWC lwc;
	*/
	private ChatListener chatListener;
	private HeroChatListener heroChatListener;
	private ClickListener clickListener;
	private BlockListener blockListener = new BlockListener(this);
	private MoveListener moveListener = new MoveListener(this);
	private ChunkListener chunkListener = new ChunkListener(this);
	private QuitListener quitListener = new QuitListener(this);
	private WorldListener worldListener = new WorldListener(this);
	private JoinListener joinListener = new JoinListener(this);
	
	private int itemWatcherID;
	public boolean lock;
	public boolean sneak;
	
	private Metrics metrics;
	
	private LogWatcher logWatcher;
	
	public void onEnable(){
		/* Create plugin folder */
		if(!this.getDataFolder().exists()){
			this.getDataFolder().mkdir();
		}
		/* Create config file */
		File configFile = new File(this.getDataFolder(), "config.yml");
		if(!configFile.exists()){
			//Copy config with comments
			getLogger().info("Generating config.yml");
			this.saveDefaultConfig();
		}
		else{
			//We've run before. Load ourself.
			reloadConfig(); //Reload it just in case this plugin was manually reloaded
			getConfig().options().copyDefaults(true);
			saveConfig();
		}
		
		//Load quick variables
		this.display = this.getConfig().getBoolean("shop.display-items");
		this.sneak = this.getConfig().getBoolean("shop.sneak-only");
		this.lock = this.getConfig().getBoolean("shop.lock");
		
		//Load messages.yml
		File messageFile = new File(this.getDataFolder(), "messages.yml");
		if(!messageFile.exists()){
			getLogger().info("Creating messages.yml");
			this.saveResource("messages.yml", true);
		}
		
		this.messages = YamlConfiguration.loadConfiguration(messageFile);
		this.messages.options().copyDefaults(true);
		
		InputStream defMessageStream = this.getResource("messages.yml");
		if(defMessageStream != null){
			YamlConfiguration defMessages = YamlConfiguration.loadConfiguration(defMessageStream);
			this.messages.setDefaults(defMessages);
		}
		else{
			this.getLogger().severe("Messages.yml not found inside plugin! This will cause errors! Update!");
		}
		Util.parseColours(this.messages);
		
		/* Hook into other plugins */
		
		if(Bukkit.getPluginManager().getPlugin("Vault") == null){
			getLogger().severe(ChatColor.RED + "You don't have Vault installed!");
			getLogger().severe(ChatColor.RED + "Download it from: ");
			getLogger().severe(ChatColor.RED + "http://dev.bukkit.org/server-mods/vault");
			getLogger().severe(ChatColor.RED + "And place it in your plugins folder!");
			getLogger().severe(ChatColor.RED + "This plugin will not function (at all) until you install vault.");
		}
		else{
			getLogger().info("Hooking Vault");
			if(!setupEconomy()){
				getLogger().severe(ChatColor.YELLOW + "Vault was found, but does not have an economy to hook into!");
				getLogger().severe(ChatColor.YELLOW + "Download an economy plugin such as:");
				getLogger().severe(ChatColor.YELLOW + "BOSEconomy, EssentialsEcon, 3Co, MultiCurrency, MineConomy, CraftConomy");
				getLogger().severe(ChatColor.YELLOW + "from http://dev.bukkit.org!");
				getLogger().severe(ChatColor.YELLOW + "This plugin will not function (at all) until you install an economy.");
			}
			else{
				getLogger().info(ChatColor.GREEN + "Vault hooked!");
			}
		}
		
		/*
		Plugin plug;
		
		//PreciousStones
		if(getConfig().getBoolean("plugins.preciousstones")){
			plug = Bukkit.getPluginManager().getPlugin("PreciousStones");
			if(plug != null){
				this.preciousStones = (PreciousStones) plug;
			}
		}
		//Towny
		if(getConfig().getBoolean("plugins.towny")){
			plug = Bukkit.getPluginManager().getPlugin("Towny");
			if(plug != null){
				this.towny = (Towny) plug;
			}	
		}
		//Lockette
		if(getConfig().getBoolean("plugins.lockette")){
			plug = Bukkit.getPluginManager().getPlugin("Lockette");
			if(plug != null){
				this.lockette = (Lockette) plug;
			}
		}
		//WorldGuard
		if(getConfig().getBoolean("plugins.worldguard")){
			plug = Bukkit.getPluginManager().getPlugin("WorldGuard");
			if(plug != null){
				this.worldGuardPlugin = (WorldGuardPlugin) plug;
			}
		}
		//GriefPrevention
		if(getConfig().getBoolean("plugins.griefprevention")){
			plug = Bukkit.getPluginManager().getPlugin("GriefPrevention");
			if(plug != null){
				this.griefPrevention = (GriefPrevention) plug;
			}
		}
		//Residence
		if(getConfig().getBoolean("plugins.residence")){
			plug = Bukkit.getPluginManager().getPlugin("Residence");
			if(plug != null){
				this.residence = (Residence) plug;
			}
		}
		//LWC
		if(getConfig().getBoolean("plugins.lwc")){
			plug = Bukkit.getPluginManager().getPlugin("LWC");
			if(plug != null){
				this.lwc = (LWC) lwc;
			}
		}
		*/
		//Create the shop manager.
		this.shopManager = new ShopManager(this);
		
		if(this.display){
			/**
			 * Display item handler thread
			 */
			getLogger().info("Starting item scheduler");
			ItemWatcher itemWatcher = new ItemWatcher(this);
			itemWatcherID = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, itemWatcher, 600, 600);
		}
		
		if(this.getConfig().getBoolean("log-actions")){
			/**
			 * Logging handler
			 */
			this.logWatcher = new LogWatcher(this, new File(this.getDataFolder(), "qs.log"));
			logWatcher.taskId = Bukkit.getScheduler().scheduleAsyncRepeatingTask(this, this.logWatcher, 150, 150);
		}
		
		/* Start database - Also creates DB file. */
		this.database = new Database(this, this.getDataFolder() + File.separator + "shops.db");
		
		/* Creates DB table 'shops' */
		if(!getDB().hasTable("shops")){
			try {
				getDB().createShopsTable();
			} catch (SQLException e) {
				e.printStackTrace();
				getLogger().severe("Could not create shops table");
			}
		}
		if(!getDB().hasTable("messages")){
			try{
				getDB().createMessagesTable();
			}
			catch (SQLException e) {
				e.printStackTrace();
				getLogger().severe("Could not create messages table");
			}
		}
		
		//Make the database up to date
		getDB().checkColumns();
		
		/* Load shops from database to memory */
		int count = 0; //Shops count
		Connection con = database.getConnection();
		try {
			getLogger().info("Loading shops from database...");
			
			PreparedStatement ps = con.prepareStatement("SELECT * FROM shops");
			ResultSet rs = ps.executeQuery();
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
					getDB().writeToBuffer("DELETE FROM shops WHERE x = "+x+" AND y = "+y+" AND z = "+z+" AND world = '"+rs.getString("world")+"'");
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
		//Safe to initialize now - It accesses config!
		this.clickListener = new ClickListener(this);
		if(Bukkit.getPluginManager().getPlugin("Herochat") != null){
			this.getLogger().info("Found Herochat... Hooking!");
			this.heroChatListener = new HeroChatListener(this);
			Bukkit.getServer().getPluginManager().registerEvents(heroChatListener, this);
		}
		else{
			this.chatListener = new ChatListener(this);
			Bukkit.getServer().getPluginManager().registerEvents(chatListener, this);
		}
		Bukkit.getServer().getPluginManager().registerEvents(clickListener, this);
		Bukkit.getServer().getPluginManager().registerEvents(blockListener, this);
		Bukkit.getServer().getPluginManager().registerEvents(moveListener, this);
		
		if(this.display){
			Bukkit.getServer().getPluginManager().registerEvents(chunkListener, this);
		}
		Bukkit.getServer().getPluginManager().registerEvents(quitListener, this);
		Bukkit.getServer().getPluginManager().registerEvents(worldListener, this);
		Bukkit.getServer().getPluginManager().registerEvents(joinListener, this);
		
		//Command handlers
		QS commandExecutor = new QS(this);
		getCommand("qs").setExecutor(commandExecutor);
		getCommand("shop").setExecutor(commandExecutor);
		
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
	public void onDisable(){
		if(this.display){
			Bukkit.getScheduler().cancelTask(itemWatcherID);
		}
		if(logWatcher != null){
			Bukkit.getScheduler().cancelTask(logWatcher.taskId);
			logWatcher.close(); //Closes the file
		}
		
		/* Remove all display items, and any dupes we can find */
		shopManager.clear();
		
		/* Empty the buffer */
		new BufferWatcher(this).run();
		this.database.stopBuffer();
		
		this.actions.clear();
		
		this.tools.clear();
		this.warnings.clear();
		
		this.reloadConfig();
	}
	/**
	 * Returns the vault economy
	 * @return The vault economy
	 */
	public Economy getEcon(){
		return economy;
	}
	
	public void log(String s){
		if(this.logWatcher == null) return;
		Date date = Calendar.getInstance().getTime();
		Timestamp time = new Timestamp(date.getTime());
		this.logWatcher.add("["+time.toString()+"] "+ s);
	}
	
	public String getMessage(String loc, String... args){
		String raw = this.messages.getString(loc);
		
		if(raw == null || raw.isEmpty()){
			return "Invalid message: " + loc;
		}
		if(args == null){
			return raw;
		}
		
		for(int i = 0; i < args.length; i++){
			raw = raw.replace("{"+i+"}", args[i]);
		}
		return raw;
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
	 * @return Returns the database handler for queries etc.
	 */
	public Database getDB(){
		return this.database;
	}
	
	public void debug(String s){
		if(!debug) return;
		this.getLogger().info(ChatColor.YELLOW + "[Debug] " + s);
	}
	
	/**
	 * Gets the chest (or null) directly next to a block. Does not check vertical or diagonal.
	 * @param b The block to check next to.
	 * @return The chest.
	 */
	public Block getChestNextTo(Block b){
		Block[] c = new Block[4];
		c[0] = (b.getLocation().add(1, 0, 0).getBlock());
		c[1] = (b.getLocation().add(-1, 0, 0).getBlock());
		c[2] = (b.getLocation().add(0, 0, 1).getBlock());
		c[3] = (b.getLocation().add(0, 0, -1).getBlock());

		for(Block d : c){
			if(d.getType() == Material.CHEST){
				return d;
			}
		}
		return null;
	}
	
	
	/*
	public WorldGuardPlugin getWorldGuard(){
		return this.worldGuardPlugin;
	}
	public Lockette getLockette(){
		return this.lockette;
	}
	public PreciousStones getPreciousStones(){
		return this.preciousStones;
	}
	public Towny getTowny(){
		return this.towny;
	}
	public GriefPrevention getGriefPrevention(){
		return this.griefPrevention;
	}
	public Residence getResidence(){
        return this.residence;
	}
	public LWC getLWC(){
		return this.lwc;
	}*/
	/**
	 * Checks other plugins to make sure they can use the chest they're making a shop.
	 * @param p The player to check
	 * @param b The block to check
	 * @return True if they're allowed to place a shop there.
	 */
	public boolean canBuildShop(Player p, Block b, BlockFace bf){
		PlayerInteractEvent event = new PlayerInteractEvent(p, Action.RIGHT_CLICK_BLOCK, new ItemStack(Material.AIR), b, bf);
		Bukkit.getPluginManager().callEvent(event);
		if(event.isCancelled()){
			return false;
		}
		else{
			return true;
		}
		/*
		if(getWorldGuard() != null){
			if(!getWorldGuard().canBuild(p, b)){
				//Can't build.
				return false;
			}
		}
		
		if(getLockette() != null){
			if(!Lockette.isUser(b, p.getName(), true)){
				//Can't use
				return false;
			}
		}
		
		if(getPreciousStones() != null){
			List<Field> fields = getPreciousStones().getForceFieldManager().getSourceFields(b.getLocation(), FieldFlag.PREVENT_USE);
				for(Field field : fields){
					if(!field.isAllowed(p.getName()) && !field.isOwner(p.getName())){
						//Not ps-allowed
						return false;
					}
				}
		}
		if(getTowny() != null){
			TownBlock tb = TownyUniverse.getTownBlock(b.getLocation());
			if(tb != null){
				try {
					if(!tb.getTown().getResidents().contains(TownyUniverse.getDataSource().getResident(p.getName()))){
						//Not a resident of the town. (Maybe I should check individual plots? How?)
						return false;
					}
				} catch (Exception e) {
				} 
			}
		}
		if(getGriefPrevention() != null){
			Claim claim = getGriefPrevention().dataStore.getClaimAt(b.getLocation(), false, null);
			if(claim != null && claim.allowContainers(p) != null){
				//Not trusted with containers.
				return false;
			}
			
		}
		if(getResidence() != null){
			ClaimedResidence res = Residence.getResidenceManager().getByLoc(b.getLocation());
			if(res!=null){
				if(!res.getPermissions().playerHas(p.getName(), "container", false)&&!Residence.getPermissionManager().isResidenceAdmin(p)){
					return false;
				}
			}
		}
		
		if(getLWC() != null){
			if(!getLWC().canAccessProtection(p, b)){
				return false;
			}
		}*/
	}
	
	/**
	 * External API Component.
	 * Returns true if the location is a shop block.
	 * If you want to use the shop, use QuickShop.getShopManager().getShop(Location) instead.
	 * @param loc The location to check
	 * @return true is it's a shop, false if it's not.
	 * 
	 * 
	 */
	public boolean isShop(Location loc){
		return this.getShopManager().getShop(loc.getBlock().getLocation()) != null;
	}
	
	/**
	 * Returns the ShopManager.  This is used for fetching, adding and removing shops.
	 * @return The ShopManager.
	 */
	public ShopManager getShopManager(){
		return this.shopManager;
	}
	
	public List<String> getMessages(String player){
		player = player.toLowerCase();
		
		List<String> messages = new ArrayList<String>(5);
		
		String q = "SELECT * FROM messages WHERE owner = '"+player+"' ORDER BY time ASC";
		
		try{
			PreparedStatement ps = getDB().getConnection().prepareStatement(q);
			ResultSet rs = ps.executeQuery();
			
			while(rs.next()){
				messages.add(rs.getString("message"));
			}
		}
		catch(SQLException e){
			e.printStackTrace();
			this.getLogger().info("Could not load messages for " + player);
		}
		return messages;
	}
	
	public void addMessage(String player, String msg){
		player = player.toLowerCase();
		
		String q = "INSERT INTO messages (owner, message, time) VALUES ('"+player+"','"+msg+"','"+System.currentTimeMillis()+"')";
		
		getDB().writeToBuffer(q);
	}
	
	public void deleteMessages(String player){
		getDB().writeToBuffer("DELETE FROM messages WHERE owner = '"+player.toLowerCase()+"'");
	}
	
	public String format(double n){
		return this.economy.format(n);
	}
}