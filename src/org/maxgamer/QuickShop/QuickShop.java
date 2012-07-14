package org.maxgamer.QuickShop;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.maxgamer.QuickShop.Listeners.BlockListener;
import org.maxgamer.QuickShop.Listeners.ChatListener;
import org.maxgamer.QuickShop.Listeners.ClickListener;
import org.maxgamer.QuickShop.Listeners.MoveListener;
//import org.maxgamer.QuickShop.Listeners.PickupListener;

public class QuickShop extends JavaPlugin{
	private Economy economy;
	private HashMap<Location, Shop> shops = new HashMap<Location, Shop>(30);
	private HashMap<String, Info> actions = new HashMap<String, Info>(30);
	private HashSet<Material> tools = new HashSet<Material>(50);
	
	private HashSet<Item> spawnedItems = new HashSet<Item>(50);
	
	private ChatListener chatListener = new ChatListener(this);
	private ClickListener clickListener = new ClickListener(this);
	private BlockListener blockListener = new BlockListener(this);
	private MoveListener moveListener = new MoveListener(this);
	//private PickupListener pickupListener = new PickupListener(this);
	
	public void onEnable(){
		getLogger().info("Loading");
		setupEconomy();
		Bukkit.getServer().getPluginManager().registerEvents(chatListener, this);
		Bukkit.getServer().getPluginManager().registerEvents(clickListener, this);
		Bukkit.getServer().getPluginManager().registerEvents(blockListener, this);
		Bukkit.getServer().getPluginManager().registerEvents(moveListener, this);
		//Bukkit.getServer().getPluginManager().registerEvents(pickupListener, this);
		getLogger().info("Loaded.");
		
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
		
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){
			@Override
			public void run() {
				for(Item item : spawnedItems){
					item.setTicksLived(1);
				}
			}
			
		}, 0, 1200);
	}
	public void onDisable(){
		
	}
	public Economy getEcon(){
		return economy;
	}
	public HashMap<Location, Shop> getShops(){
		return this.shops;
	}
	public HashMap<String, Info> getActions(){
		return this.actions;
	}
	
	 private boolean setupEconomy(){
	        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
	        if (economyProvider != null) {
	            economy = economyProvider.getProvider();
	        }

	        return (economy != null);
    }
	 
	public Shop getShop(Location loc){
		return this.shops.get(loc);
	}
	 
	public boolean isTool(Material mat){
		return this.tools.contains(mat);
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
		return this.spawnedItems.contains(item);
	}
	public HashSet<Item> getProtectedItems(){
		return this.spawnedItems;
	}
}