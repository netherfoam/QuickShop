package org.maxgamer.QuickShop;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionType;
import org.maxgamer.QuickShop.Command.QS;
import org.maxgamer.QuickShop.Database.Database;
import org.maxgamer.QuickShop.Listeners.BlockListener;
import org.maxgamer.QuickShop.Listeners.ChatListener;
import org.maxgamer.QuickShop.Listeners.ChunkListener;
import org.maxgamer.QuickShop.Listeners.ClickListener;
import org.maxgamer.QuickShop.Listeners.MoveListener;
import org.maxgamer.QuickShop.Listeners.QuitListener;
import org.maxgamer.QuickShop.Shop.Info;
import org.maxgamer.QuickShop.Shop.Shop;
import org.maxgamer.QuickShop.Shop.Shop.ShopType;
import org.maxgamer.QuickShop.Shop.ShopChunk;
import org.maxgamer.QuickShop.Watcher.BufferWatcher;
import org.maxgamer.QuickShop.Watcher.ItemWatcher;

import com.bekvon.bukkit.residence.Residence;
import com.palmergames.bukkit.towny.Towny;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.griefcraft.lwc.LWC;
import org.yi.acru.bukkit.Lockette.Lockette;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.sacredlabyrinth.Phaed.PreciousStones.FieldFlag;
import net.sacredlabyrinth.Phaed.PreciousStones.PreciousStones;
import net.sacredlabyrinth.Phaed.PreciousStones.vectors.Field;


import net.milkbowl.vault.economy.Economy;

public class QuickShop extends JavaPlugin{
	private Economy economy;
	private HashMap<ShopChunk, HashMap<Location, Shop>> shopChunks = new HashMap<ShopChunk, HashMap<Location, Shop>>(10);
	
	private HashMap<String, Info> actions = new HashMap<String, Info>(30);
	private HashSet<Material> tools = new HashSet<Material>(50);
	public boolean debug = false;
	public HashSet<String> warnings = new HashSet<String>(10);
	
	private Database database;
	
	/* Hooking into plugins */
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
	
	private ChatListener chatListener = new ChatListener(this);
	private ClickListener clickListener;
	private BlockListener blockListener = new BlockListener(this);
	private MoveListener moveListener = new MoveListener(this);
	private ChunkListener chunkListener = new ChunkListener(this);
	private QuitListener quitListener = new QuitListener(this);
	
	private int itemWatcherID;
	public boolean lock;
	public boolean sneak;
	
	public void onEnable(){
		getLogger().info("Hooking Vault");
		setupEconomy();
		
		//Safe to initialize now - It accesses config!
		this.clickListener = new ClickListener(this);
		getLogger().info("Registering Listeners");
		Bukkit.getServer().getPluginManager().registerEvents(chatListener, this);
		Bukkit.getServer().getPluginManager().registerEvents(clickListener, this);
		Bukkit.getServer().getPluginManager().registerEvents(blockListener, this);
		Bukkit.getServer().getPluginManager().registerEvents(moveListener, this);
		Bukkit.getServer().getPluginManager().registerEvents(chunkListener, this);
		Bukkit.getServer().getPluginManager().registerEvents(quitListener, this);
		
		QS commandExecutor = new QS(this);
		getCommand("qs").setExecutor(commandExecutor);
		getCommand("shop").setExecutor(commandExecutor);
		
		/* Create plugin folder */
		if(!this.getDataFolder().exists()){
			this.getDataFolder().mkdir();
		}
		/* Create config file */
		File configFile = new File(this.getDataFolder(), "config.yml");
		if(!configFile.exists()){
			//Copy config with comments
			getLogger().info("Generating config");
			this.saveDefaultConfig();
		}
		else{
			getConfig().options().copyDefaults(true);
			saveConfig();
		}
		
		/* Hook into other plugins */
		Plugin plug;
		
		if(getConfig().getBoolean("plugins.preciousstones")){
			plug = Bukkit.getPluginManager().getPlugin("PreciousStones");
			if(plug != null){
				this.preciousStones = (PreciousStones) plug;
			}
		}
		
		if(getConfig().getBoolean("plugins.towny")){
			plug = Bukkit.getPluginManager().getPlugin("Towny");
			if(plug != null){
				this.towny = (Towny) plug;
			}	
		}
		
		if(getConfig().getBoolean("plugins.lockette")){
			plug = Bukkit.getPluginManager().getPlugin("Lockette");
			if(plug != null){
				this.lockette = (Lockette) plug;
			}
		}
		
		if(getConfig().getBoolean("plugins.worldguard")){
			plug = Bukkit.getPluginManager().getPlugin("WorldGuard");
			if(plug != null){
				this.worldGuardPlugin = (WorldGuardPlugin) plug;
			}
		}
		
		if(getConfig().getBoolean("plugins.griefprevention")){
			plug = Bukkit.getPluginManager().getPlugin("GriefPrevention");
			if(plug != null){
				this.griefPrevention = (GriefPrevention) plug;
			}
		}
		
		if(getConfig().getBoolean("plugins.residence")){
			plug = Bukkit.getPluginManager().getPlugin("Residence");
			if(plug != null){
				this.residence = (Residence) plug;
			}
		}
		if(getConfig().getBoolean("plugins.lwc")){
			plug = Bukkit.getPluginManager().getPlugin("LWC");
			if(plug != null){
				this.lwc = (LWC) lwc;
			}
		}
		
		/* Start database - Also creates DB file. */
		this.database = new Database(this, this.getDataFolder() + File.separator + "shops.db");
		
		getLogger().info("Loading tools");
		loadTools();
		
		/* Creates DB table 'shops' */
		if(!getDB().hasTable()){
			try {
				getDB().createTable();
			} catch (SQLException e) {
				e.printStackTrace();
				getLogger().severe("Could not create database table");
			}
		}
		getDB().checkColumns();
		
		/* Load shops from database to memory */
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
				/* Delete invalid shops, if we know of any */
				if(world != null && loc.getBlock().getType() != Material.CHEST){
					getLogger().info("Shop is not a chest in " +rs.getString("world") + " at: " + x + ", " + y + ", " + z + ".  Removing from DB.");
					getDB().writeToBuffer("DELETE FROM shops WHERE x = "+x+" AND y = "+y+" AND z = "+z+" AND world = '"+rs.getString("world")+"'");
				}
				
				int type = rs.getInt("type");
				
				Shop shop = new Shop(loc, price, item, owner);
				shop.setUnlimited(rs.getBoolean("unlimited"));
				
				shop.setShopType(ShopType.fromID(type));
				
				this.addShop(shop);
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
			getLogger().severe("Could not load shops.");
		}
		getLogger().info("Loaded ? shops.");
		/**
		 * Display item handler thread
		 */
		getLogger().info("Starting item scheduler");
		ItemWatcher itemWatcher = new ItemWatcher(this);
		itemWatcherID = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, itemWatcher, 150, 150);
		
		
		this.sneak = this.getConfig().getBoolean("shop.sneak-only");
		this.lock = this.getConfig().getBoolean("shop.lock");
	}
	public void onDisable(){
		Bukkit.getScheduler().cancelTask(itemWatcherID);
		/* Remove all display items, and any dupes we can find */
		/*
		for(Shop shop : this.shops.values()){
			shop.getDisplayItem().removeDupe();
			shop.getDisplayItem().remove();
		}*/
		
		for(HashMap<Location, Shop> inChunk : this.shopChunks.values()){
			for(Shop shop : inChunk.values()){
				shop.getDisplayItem().removeDupe();
				shop.getDisplayItem().remove();
			}
			inChunk.clear();
		}
		this.shopChunks.clear();
		//this.shops.clear();
		
		/* Empty the buffer */
		new BufferWatcher().run();
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
	
	public void addShop(Shop shop){
		//this.shops.put(shop.getLocation(), shop);
		Location loc = shop.getLocation();
		Chunk chunk = loc.getChunk();
		ShopChunk shopChunk = new ShopChunk(loc.getWorld(), chunk.getX(), chunk.getZ());
		
		
		HashMap<Location, Shop> shopsInChunk = this.shopChunks.get(shopChunk);
		
		if(shopsInChunk == null){
			//Theres no shops in this chunk yet.
			shopsInChunk = new HashMap<Location, Shop>(1);
			this.shopChunks.put(shopChunk, shopsInChunk);
		}
		
		shopsInChunk.put(loc, shop);
	}
	
	public HashMap<Location, Shop> getShopsInChunk(Chunk c){
		ShopChunk shopChunk = new ShopChunk(c.getWorld(), c.getX(), c.getZ());
		return this.shopChunks.get(shopChunk);
	}
	
	/**
	 * Deletes a shop from the memory, NOT the database
	 * @param shop The shop to delete
	 */
	public void removeShop(Shop shop){
		//this.getShops().remove(shop.getLocation());
		this.getShopsInChunk(shop.getLocation().getChunk()).remove(shop.getLocation());
	}
	
	 /**
	  * Fetches a shop in a particular location, or null.
	  * @param loc The location to check.
	  * @return The shop at the location.
	  */
	public Shop getShop(Location loc){
		//return this.shops.get(loc);
		Chunk chunk = loc.getChunk();
		
		ShopChunk shopChunk = new ShopChunk(chunk.getWorld(), chunk.getX(), chunk.getZ());
		
		HashMap<Location, Shop> inChunk = this.shopChunks.get(shopChunk);
		if(inChunk == null) return null;
		return inChunk.get(loc);
		
	}
	public HashMap<ShopChunk, HashMap<Location, Shop>> getShops(){
		return this.shopChunks;
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
			int level = Integer.parseInt(itemInfo[i+1]);
			
			Enchantment ench = Enchantment.getByName(itemInfo[i]);
			if(ench == null) continue; //Invalid
			if (ench.canEnchantItem(item)){
				if(level <= 0) continue;
				level = Math.min(ench.getMaxLevel(), level);
				
				item.addEnchantment(ench, level);
			}
			
		}
		return item;
	}
	
	public void debug(String s){
		if(!debug) return;
		this.getLogger().info(ChatColor.YELLOW + "[Debug] " + s);
	}
	
	/**
	 * Converts an itemstack into a string for database storage.  See makeItem(String itemString) for 
	 * reversing this.
	 * @param item The item to model it off of.
	 * @return A new string with the properties of the item.
	 */
	public String makeString(ItemStack item){
		String itemString = item.getType().toString() + ":" + item.getData().getData() + ":" + item.getDurability() + ":" + item.getAmount();
		
		for(Entry<Enchantment, Integer> ench : item.getEnchantments().entrySet()){
			itemString += ":" + ench.getKey().getName() + ":" + ench.getValue();
		}
		return itemString;
	}
	
	/**
	 * Stores all the tools in a hashset to easily check if an item is a tool.
	 * Data on tools is converted to durability %.
	 */
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
	
	/**
	 * Converts a given material and data value into a format similar to Material.<?>.toString().
	 * Upper case, with underscores.  Includes material name in result.
	 * @param mat The base material.
	 * @param damage The durability/damage of the item.
	 * @return A string with the name of the item.
	 */
	public String getDataName(Material mat, short damage){
		int id = mat.getId();
		switch(id){
		case 35: 
			switch((int) damage){
			case 0: return "WHITE_WOOL";
			case 1: return "ORANGE_WOOL";
			case 2: return "MAGENTA_WOOL";
			case 3: return "LIGHT_BLUE_WOOL";
			case 4: return "YELLOW_WOOL";
			case 5: return "LIME_WOOL";
			case 6: return "PINK_WOOL";
			case 7: return "GRAY_WOOL";
			case 8: return "LIGHT_GRAY_WOOL";
			case 9: return "CYAN_WOOL";
			case 10: return "PURPLE_WOOL";
			case 11: return "BLUE_WOOL";
			case 12: return "BROWN_WOOL";
			case 13: return "GREEN_WOOL";
			case 14: return "RED_WOOL";
			case 15: return "BLACK_WOOL";
			}
			return mat.toString();
		case 351:
			switch((int) damage){
			case 0: return "INK_SAC";
			case 1: return "ROSE_RED";
			case 2: return "CACTUS_GREEN";
			case 3: return "COCOA_BEANS";
			case 4: return "LAPIS_LAZULI";
			case 5: return "PURPLE_DYE";
			case 6: return "CYAN_DYE";
			case 7: return "LIGHT_GRAY_DYE";
			case 8: return "GRAY_DYE";
			case 9: return "PINK_DYE";
			case 10: return "LIME_DYE";
			case 11: return "DANDELION_YELLOW";
			case 12: return "LIGHT_BLUE_DYE";
			case 13: return "MAGENTA_DYE";
			case 14: return "ORANGE_DYE";
			case 15: return "BONE_MEAL";
			}
			return mat.toString();
		case 98:
			switch((int) damage){
			case 0: return "STONE_BRICKS";
			case 1: return "MOSSY_STONE_BRICKS";
			case 2: return "CRACKED_STONE_BRICKS";
			case 3: return "CHISELED_STONE_BRICKS";
			}
			return mat.toString();
		case 373:
			//Special case
			if(damage == 64) return "MUNDANE_POTION";
			Potion pot = null;
			try{
				pot = Potion.fromDamage(damage);
			}
			catch(IllegalArgumentException ex){
				pot = new Potion(PotionType.WATER);
			}
			
			String prefix = "";
			String suffix = "";
			if(pot.getLevel() > 0) suffix += "_" + pot.getLevel();
			if(pot.hasExtendedDuration()) prefix += "EXTENDED_";
			if(pot.isSplash()) prefix += "SPLASH_";
			
			switch((int) pot.getNameId()){
			case 0: return prefix + "WATER_BOTTLE" + suffix;
			case 1: return prefix + "POTION_OF_REGENERATION" + suffix;
			case 2: return prefix + "POTION_OF_SWIFTNESS" + suffix;
			case 3: return prefix + "POTION_OF_FIRE_RESISTANCE" + suffix;
			case 4: return prefix + "POTION_OF_POISON" + suffix;
			case 5: return prefix + "POTION_OF_HEALING" + suffix;
			case 6: return prefix + "CLEAR_POTION" + suffix;
			case 7: return prefix + "CLEAR_POTION" + suffix;
			case 8: return prefix + "POTION_OF_WEAKNESS" + suffix;
			case 9: return prefix + "POTION_OF_STRENGTH" + suffix;
			case 10: return prefix + "POTION_OF_SLOWNESS" + suffix;
			case 11: return prefix + "DIFFUSE_POTION" + suffix;
			case 12: return prefix + "POTION_OF_HARMING" + suffix;
			case 13: return prefix + "ARTLESS_POTION" + suffix;
			case 14: return prefix + "THIN_POTION" + suffix;
			case 15: return prefix + "THIN_POTION" + suffix;
			case 16: return prefix + "AWKWARD_POTION" + suffix;
			case 32: return prefix + "THICK_POTION" + suffix;
			}
			return mat.toString();
		case 6:
			switch((int) damage){
			case 0: return "OAK_SAPLING";
			case 1: return "PINE_SAPLING";
			case 2: return "BIRCH_SAPLING";
			case 3: return "JUNGLE_TREE_SPALING";
			}
			return mat.toString();
		
		case 5:
			switch((int) damage){
			case 0: return "OAK_PLANKS";
			case 1: return "PINE_PLANKS";
			case 2: return "BIRCH_PLANKS";
			case 3: return "JUNGLE_PLANKS";
			}
			return mat.toString();
		case 17:
			switch(damage){
			case 0: return "OAK_LOG";
			case 1: return "PINE_LOG";
			case 2: return "BIRCH_LOG";
			case 3: return "JUNGLE_LOG";
			}
			return mat.toString();
		case 18:
			damage = (short) (damage%4);
			switch(damage){
			case 0: return "OAK_LEAVES";
			case 1: return "PINE_LEAVES";
			case 2: return "BIRCH_LEAVES";
			case 3: return "JUNGLE_LEAVES";
			}
		case 263:
			switch(damage){
			case 0: return "COAL";
			case 1: return "CHARCOAL";
			}
			return mat.toString();
		case 24:
			switch((int) damage){
			case 0: return "SANDSTONE";
			case 1: return "CHISELED_SANDSTONE";
			case 2: return "SMOOTH_SANDSTONE";
			}
			return mat.toString();
		case 31:
			switch((int) damage){
			case 0: return "DEAD_SHRUB";
			case 1: return "TALL_GRASS";
			case 2: return "FERN";
			}
			return mat.toString();
		case 44:
			switch((int) damage){
			case 0: return "STONE_SLAB";
			case 1: return "SANDSTONE_SLAB";
			case 2: return "WOODEN_SLAB";
			case 3: return "COBBLESTONE_SLAB";
			case 4: return "BRICK_SLAB";
			case 5: return "STONE_BRICK_SLAB";
			}
			return mat.toString();
		case 383:
			switch((int) damage){
			case 50: return "CREEPER_EGG";
			case 51: return "SKELETON_EGG";
			case 52: return "SPIDER_EGG";
			case 53: return "GIANT_EGG";
			case 54: return "ZOMBIE_EGG";
			case 55: return "SLIME_EGG";
			case 56: return "GHAST_EGG";
			case 57: return "ZOMBIE_PIGMAN_EGG";
			case 58: return "ENDERMAN_EGG";
			case 59: return "CAVE_SPIDER_EGG";
			case 60: return "SILVERFISH_EGG";
			case 61: return "BLAZE_EGG";
			case 62: return "MAGMA_CUBE_EGG";
			case 63: return "ENDER_DRAGON_EGG";
			case 90: return "PIG_EGG";
			case 91: return "SHEEP_EGG";
			case 92: return "COW_EGG";
			case 93: return "CHICKEN_EGG";
			case 94: return "SQUID_EGG";
			case 95: return "WOLF_EGG";
			case 96: return "MOOSHROOM_EGG";
			case 97: return "SNOW_GOLEM_EGG";
			case 98: return "OCELOT_EGG";
			case 99: return "IRON_GOLEM_EGG";
			case 120: return "VILLAGER_EGG";
			case 200: return "ENDER_CRYSTAL_EGG";
			case 14: return "PRIMED_TNT_EGG";
			}
			return mat.toString();
		case 76:
			return "REDSTONE_TORCH";
		case 115:
			return "NETHER_WART";
		case 30:
			return "COBWEB";
		case 102:
			return "GLASS_PANE";
		case 101:
			return "IRON_BARS";
		case 58:
			return "CRAFTING_TABLE";
		case 123:
			return "REDSTONE_LAMP";
		}
		if(damage == 0 || isTool(mat)) return mat.toString();
		return mat.toString()+ ":" + damage;
	}
	
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
	}
	/**
	 * Checks other plugins to make sure they can use the chest they're making a shop.
	 * @param p The player to check
	 * @param b The block to check
	 * @return True if they're allowed to place a shop there.
	 */
	public boolean canBuildShop(Player p, Block b){
		
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
		}
		
		return true;
	}
}