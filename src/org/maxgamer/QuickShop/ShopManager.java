package org.maxgamer.QuickShop;

import java.util.HashMap;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.maxgamer.QuickShop.Shop.Shop;
import org.maxgamer.QuickShop.Shop.ShopChunk;

public class ShopManager{
	private QuickShop plugin;
	
	private HashMap<String, HashMap<ShopChunk, HashMap<Location, Shop>>> shops = new HashMap<String, HashMap<ShopChunk, HashMap<Location, Shop>>>(3);
	
	public ShopManager(QuickShop plugin){
		this.plugin = plugin;
	}
	
	/**
	 * Returns a hashmap of World -> Chunk -> Shop
	 * @return a hashmap of World -> Chunk -> Shop
	 */
	public HashMap<String, HashMap<ShopChunk, HashMap<Location, Shop>>> getShops(){
		return this.shops;
	}
	/**
	 * Returns a hashmap of Chunk -> Shop
	 * @param world The name of the world (case sensitive) to get the list of shops from
	 * @return a hashmap of Chunk -> Shop
	 */
	public HashMap<ShopChunk, HashMap<Location, Shop>> getShops(String world){
		plugin.debug("Getting shops in world " + world);
		return this.shops.get(world);
	}
	/**
	 * Returns a hashmap of Shops
	 * @param c The chunk to search. Referencing doesn't matter, only coordinates and world are used.
	 * @return
	 */
	public HashMap<Location, Shop> getShops(Chunk c){
		HashMap<ShopChunk, HashMap<Location, Shop>> inWorld = this.getShops(c.getWorld().getName());
		
		if(inWorld == null){
			plugin.debug("No world shops");
			return null;
		}
		
		ShopChunk shopChunk = new ShopChunk(c.getWorld().getName(), c.getX(), c.getZ());
		plugin.debug("Getting shops in ShopChunk " + shopChunk.getX() + "," + shopChunk.getZ());
		return inWorld.get(shopChunk);
	}
	
	/**
	 * Gets a shop in a specific location
	 * @param loc The location to get the shop from
	 * @return The shop at that location
	 */
	public Shop getShop(Location loc){
		HashMap<Location, Shop> inChunk = getShops(loc.getChunk());
		if(inChunk == null){
			plugin.debug("No shops in that chunk");
			return null;
		}
		//We can do this because WorldListener updates the world reference so the world in loc is the same as world in inChunk.get(loc)
		return inChunk.get(loc);
	}
	
	/**
	 * Adds a shop to the world.  Does NOT require the chunk or world to be loaded
	 * @param world The name of the world
	 * @param shop The shop to add
	 */
	public void addShop(String world, Shop shop){
		HashMap<ShopChunk, HashMap<Location, Shop>> inWorld = this.getShops().get(world);
		plugin.debug("Creating shop...");
		
		//There's no world storage yet. We need to create that hashmap.
		if(inWorld == null){
			plugin.debug("Creating world storage for " + world);
			inWorld = new HashMap<ShopChunk, HashMap<Location, Shop>>(3);
			//Put it in the data universe
			this.getShops().put(world, inWorld);
		}
		
		//Calculate the chunks coordinates.  These are 1,2,3 for each chunk, NOT location rounded to the nearest 16.
		int x = (int) Math.floor((shop.getLocation().getBlockX()) / 16.0);
		int z = (int) Math.floor((shop.getLocation().getBlockZ()) / 16.0);
		
		//Get the chunk set from the world info
		ShopChunk shopChunk = new ShopChunk(world, x, z);
		HashMap<Location, Shop> inChunk = inWorld.get(shopChunk);
		
		//That chunk data hasn't been created yet - Create it!
		if(inChunk == null){
			plugin.debug("Creating chunk storage..");
			inChunk = new HashMap<Location, Shop>(1);
			//Put it in the world
			inWorld.put(shopChunk, inChunk);
		}
		
		//Put the shop in its location in the chunk list.
		inChunk.put(shop.getLocation(), shop);
	}
	
	/**
	 * Removes a shop from the world. Does NOT remove it from the database. 
	 * * REQUIRES * the world to be loaded 
	 * @param shop The shop to remove
	 */
	public void removeShop(Shop shop){
		plugin.debug("Removing shop...");
		Location loc = shop.getLocation();
		String world = loc.getWorld().getName();
		HashMap<ShopChunk, HashMap<Location, Shop>> inWorld = this.getShops().get(world);
		
		int x = (int) Math.floor((shop.getLocation().getBlockX()) / 16.0);
		int z = (int) Math.floor((shop.getLocation().getBlockZ()) / 16.0);
		
		ShopChunk shopChunk = new ShopChunk(world, x, z);
		HashMap<Location, Shop> inChunk = inWorld.get(shopChunk);
		
		inChunk.remove(loc);
	}
	
	/**
	 * Removes all shops from memory and the world. Does not delete them from the database.
	 * Call this on plugin disable ONLY.
	 */
	public void clear(){
		for(HashMap<ShopChunk, HashMap<Location, Shop>> inWorld : this.getShops().values()){
			for(HashMap<Location, Shop> inChunk : inWorld.values()){
				for(Shop shop : inChunk.values()){
					shop.getDisplayItem().removeDupe();
					shop.getDisplayItem().remove();
				}
			}
		}
		
		this.shops.clear();
	}
}