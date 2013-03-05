package org.maxgamer.QuickShop.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.maxgamer.QuickShop.QuickShop;
import org.maxgamer.QuickShop.ShopManager;
import org.maxgamer.QuickShop.Database.Database;
import org.maxgamer.QuickShop.Shop.ChestShop;
import org.maxgamer.QuickShop.Shop.Shop;
import org.maxgamer.QuickShop.Shop.ShopChunk;
import org.maxgamer.QuickShop.Shop.ShopType;

public class Converter{
	/**
	 * Attempts to convert the quickshop database, if necessary.
	 * @return -1 for failure, 0 for no changes, 1 for success converting.
	 */
	public static int convert(){
		Database database = QuickShop.instance.getDB();
		
		if(database.hasColumn("shops", "itemString")){
			//Convert.
			try{
				convertDatabase_2_9();
				convertDatabase_3_4();
				convertDatabase_3_8();
				return 1;
			}
			catch(Exception e){
				e.printStackTrace();
				return -1;
			}
		}
		
		try{
			Connection con = database.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM shops");
			ResultSet rs = ps.executeQuery();
			
			String colType = rs.getMetaData().getColumnTypeName(3);
			ps.close();
			rs.close();
			
			if(rs.next()){
				try{
					rs.getString("item");
					if(!colType.equalsIgnoreCase("BLOB")){
						System.out.println("Item column type: " + colType + ", converting to BLOB.");
						
						//We're using the old format
						try{
							convertDatabase_3_4();
							convertDatabase_3_8();
							return 1;
						}
						catch(Exception e){
							e.printStackTrace();
							return -1;
						}
					}
				}
				catch(SQLException e){
					//No item table column.
					//No upgrade necessary.
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
			return -1;
		}
		
		try{
			if(database.hasColumn("shops", "item")){
				convertDatabase_3_8();
				return 1;
			}
		}
		catch(Exception e){
			e.printStackTrace();
			return -1;
		}
		
		return 0;
	}
	
	public static void convertDatabase_3_8() throws Exception{
		Database database = QuickShop.instance.getDB();
		ShopManager shopManager = QuickShop.instance.getShopManager();
		
		Connection con = database.getConnection();
		System.out.println("Converting shops to 3.8 format...");
		//Step 1: Load existing shops.
		PreparedStatement ps = con.prepareStatement("SELECT * FROM shops");
		ResultSet rs = ps.executeQuery();
		int shops = 0;
		System.out.println("Loading shops...");
		while(rs.next()){
			int x = rs.getInt("x");
			int y = rs.getInt("y");
			int z = rs.getInt("z");
			String worldName = rs.getString("world");
			try{
				World world = Bukkit.getWorld(worldName);
	
				ItemStack item = Util.getItemStack(rs.getBytes("item"));				
				
				String owner = rs.getString("owner");
				double price = rs.getDouble("price");
				Location loc = new Location(world, x, y, z);
				
				int type = rs.getInt("type");
				Shop shop = new ChestShop(loc, price, item, owner);
				shop.setUnlimited(rs.getBoolean("unlimited"));
				shop.setShopType(ShopType.fromID(type));
				
				shopManager.loadShop(rs.getString("world"), shop);
				shops++;
			}
			catch(Exception e){
				System.out.println("Error loading a shop! Coords: "+worldName+" (" + x + ", " + y + ", " + z + ") - Skipping it...");
			}
		}
		ps.close();
		rs.close();
		
		System.out.println("Loading complete. Backing up and deleting shops table...");
		//Step 2: Delete shops table.
		File existing = new File(QuickShop.instance.getDataFolder(), "shops.db");
		File backup = new File(existing.getAbsolutePath() + ".3.7.bak");
		
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
		Statement st = database.getConnection().createStatement();
		String createTable = 
		"CREATE TABLE shops (" + 
				"owner  TEXT(20) NOT NULL, " +
				"price  double(32, 2) NOT NULL, " +
				"itemConfig  BLOB NOT NULL, " +
				"x  INTEGER(32) NOT NULL, " +
				"y  INTEGER(32) NOT NULL, " +
				"z  INTEGER(32) NOT NULL, " +
				"world VARCHAR(32) NOT NULL, " +
				"unlimited  boolean, " +
				"type  boolean, " +
				"PRIMARY KEY (x, y, z, world) " +
				");";
		st.execute(createTable);
		
		//Step 4: Export the new data into the table
		for(Entry<String, HashMap<ShopChunk, HashMap<Location, Shop>>> worlds : shopManager.getShops().entrySet()){
			String world = worlds.getKey();
			for(Entry<ShopChunk, HashMap<Location, Shop>> chunks : worlds.getValue().entrySet()){
				for(Shop shop : chunks.getValue().values()){
					ps = con.prepareStatement("INSERT INTO shops (owner, price, itemConfig, x, y, z, world, unlimited, type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
					ps.setString(1, shop.getOwner());
					ps.setDouble(2, shop.getPrice());
					
					ps.setString(3, Util.serialize(shop.getItem()));
					
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
	
	/** 
	 * Converts the database to v 2.9 format. 
	 * This changes the itemString column to item, and stores it as NBT in a text field.
	 * @throws SQLException */
	public static void convertDatabase_2_9() throws Exception{
		Database database = QuickShop.instance.getDB();
		ShopManager shopManager = QuickShop.instance.getShopManager();
		
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
			String worldName = rs.getString("world");
			try{
				World world = Bukkit.getWorld(worldName);
	
				ItemStack item = Util.makeItem(rs.getString("itemString"));				
				
				String owner = rs.getString("owner");
				double price = rs.getDouble("price");
				Location loc = new Location(world, x, y, z);
				
				int type = rs.getInt("type");
				Shop shop = new ChestShop(loc, price, item, owner);
				shop.setUnlimited(rs.getBoolean("unlimited"));
				shop.setShopType(ShopType.fromID(type));
				
				shopManager.loadShop(rs.getString("world"), shop);
				shops++;
			}
			catch(Exception e){
				System.out.println("Error loading a shop! Coords: "+worldName+" (" + x + ", " + y + ", " + z + ") - Skipping it...");
			}
		}
		ps.close();
		rs.close();
		
		System.out.println("Loading complete. Backing up and deleting shops table...");
		//Step 2: Delete shops table.
		File existing = new File(QuickShop.instance.getDataFolder(), "shops.db");
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
		Statement st = database.getConnection().createStatement();
		String createTable = 
		"CREATE TABLE shops (" + 
				"owner  TEXT(20) NOT NULL, " +
				"price  double(32, 2) NOT NULL, " +
				"item  BLOB NOT NULL, " +
				"x  INTEGER(32) NOT NULL, " +
				"y  INTEGER(32) NOT NULL, " +
				"z  INTEGER(32) NOT NULL, " +
				"world VARCHAR(32) NOT NULL, " +
				"unlimited  boolean, " +
				"type  boolean, " +
				"PRIMARY KEY (x, y, z, world) " +
				");";
		st.execute(createTable);
		
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
	 * 
	 * This changes the item column to a blob and stores the data as
	 * a byte array instead.
	 * @throws SQLException */
	public static void convertDatabase_3_4() throws Exception{
		Database database = QuickShop.instance.getDB();
		ShopManager shopManager = QuickShop.instance.getShopManager();
		
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
			String worldName = rs.getString("world");
			
			try{
				World world = Bukkit.getWorld(worldName);
	
				ItemStack item = Util.getItemStack(rs.getString("item"));
				
				String owner = rs.getString("owner");
				double price = rs.getDouble("price");
				Location loc = new Location(world, x, y, z);
				
				int type = rs.getInt("type");
				Shop shop = new ChestShop(loc, price, item, owner);
				shop.setUnlimited(rs.getBoolean("unlimited"));
				shop.setShopType(ShopType.fromID(type));
				
				shopManager.loadShop(rs.getString("world"), shop);
				shops++;
			}
			catch(Exception e){
				System.out.println("Error loading a shop! Coords: "+worldName+" (" + x + ", " + y + ", " + z + ") - Skipping it...");
			}
		}
		ps.close();
		rs.close();
		
		System.out.println("Loading complete. Backing up and deleting shops table...");
		//Step 2: Delete shops table.
		File existing = new File(QuickShop.instance.getDataFolder(), "shops.db");
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
		Statement st = database.getConnection().createStatement();
		String createTable = 
		"CREATE TABLE shops (" + 
				"owner  TEXT(20) NOT NULL, " +
				"price  double(32, 2) NOT NULL, " +
				"item  BLOB NOT NULL, " +
				"x  INTEGER(32) NOT NULL, " +
				"y  INTEGER(32) NOT NULL, " +
				"z  INTEGER(32) NOT NULL, " +
				"world VARCHAR(32) NOT NULL, " +
				"unlimited  boolean, " +
				"type  boolean, " +
				"PRIMARY KEY (x, y, z, world) " +
				");";
		st.execute(createTable);
		
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