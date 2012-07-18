package org.maxgamer.QuickShop.Database;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.bukkit.Bukkit;
import org.maxgamer.QuickShop.QuickShop;

public class Database{
	QuickShop plugin;
	File file;
	/**
	 * Creates a new database handler.
	 * @param plugin The plugin creating the database.
	 * @param file The name of the DB file, including path.
	 */
	public Database(QuickShop plugin, String file){
		this.plugin = plugin;
		this.file = new File(file);
	}
	/**
	 * Returns a new connection to execute SQL statements on.
	 * @return A new connection to execute SQL statements on.
	 */
	public Connection getConnection(){
		//Handles first creation
		if(!this.getFile().exists()){
			plugin.getLogger().info("Database does not exist");
			try {
				this.getFile().createNewFile();
				Class.forName("org.sqlite.JDBC");
				Connection dbCon = DriverManager.getConnection("jdbc:sqlite:" + this.getFile());
				return dbCon;
			} 
			catch (IOException e) {
				e.printStackTrace();
				plugin.getLogger().info("Could not create file " + this.getFile().toString());
			} 
			catch (ClassNotFoundException e) {
				e.printStackTrace();
				plugin.getLogger().info("You need the SQLite JBDC library.  Put it in MinecraftServer/lib folder.");
			} catch (SQLException e) {
				e.printStackTrace();
				plugin.getLogger().info("SQLite exception on initialize " + e);
			}
		}
		try{
			Class.forName("org.sqlite.JDBC");
			return DriverManager.getConnection("jdbc:sqlite:" + this.getFile());
		}
		catch(SQLException e){
			e.printStackTrace();
			plugin.getLogger().info("SQLite exception on initialize.");
		}
		catch(ClassNotFoundException e){
			e.printStackTrace();
			plugin.getLogger().info("SQLite library not found, was it removed?");
		}
		return null;
	}
	/**
	 * @return Returns the database file
	 */
	public File getFile(){
		return this.file;
	}
	
	/**
	 * @return Returns true if the shops table exists 
	 */
	public boolean hasTable(){
		try {
			PreparedStatement ps = this.getConnection().prepareStatement("SELECT name FROM sqlite_master WHERE type='table' AND name='shops';");
			ResultSet rs = ps.executeQuery();
			while(rs.next()){
				ps.close();
				return true;
			}
			ps.close();
			return false;
			
		} catch (SQLException e) {
			return false;
		}
	}
	
	/**
	 * Writes a query to the buffer safely.
	 * @param s The String to write to the buffer (In SQL syntax... E.g. UPDATE table SET x = 'y', owner = 'bob'
	 */
	public void writeToBuffer(final String s){
		Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, new Runnable(){
			
			@Override
			public void run() {
				QuickShop plugin = (QuickShop) Bukkit.getPluginManager().getPlugin("QuickShop");
				
				while(plugin.queriesInUse){
					//Wait
				}
				
				plugin.queriesInUse = true;
				plugin.queries.add(s);
				plugin.queriesInUse = false;
			}
			
		}, 0);
	}
	
	/**
	 * Creates the database table 'shops'.
	 * @throws SQLException If the connection is invalid.
	 */
	public void createTable() throws SQLException{
		Statement st = getConnection().createStatement();
		String createTable = 
		"CREATE TABLE \"shops\" (" + 
				"\"owner\"  TEXT(20) NOT NULL, " +
				"\"price\"  INTEGER(32) NOT NULL, " +
				"\"itemString\"  TEXT(200) NOT NULL, " +
				"\"x\"  INTEGER(32) NOT NULL, " +
				"\"y\"  INTEGER(32) NOT NULL, " +
				"\"z\"  INTEGER(32) NOT NULL, " +
				"\"world\"  TEXT(30) NOT NULL, " +
				"PRIMARY KEY ('x', 'y','z','world') " +
				");";
		st.execute(createTable);
	}
}