package org.maxgamer.QuickShop.Database;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.maxgamer.QuickShop.QuickShop;
import org.maxgamer.QuickShop.Watcher.BufferWatcher;

public class Database{
	QuickShop plugin;
	File file;
	public List<String> queries = new ArrayList<String>(5);
	public boolean queriesInUse = false;
	public int bufferWatcherID;
	private Connection connection;
	
	/**
	 * Creates a new database handler.
	 * @param plugin The plugin creating the database.
	 * @param file The name of the DB file, including path.
	 */
	public Database(QuickShop plugin, String file){
		this.plugin = plugin;
		this.file = new File(file);
		
		/**
		 * Database query handler thread
		 */
		startBufferWatcher();
	}
	
	public void startBufferWatcher(){
		this.bufferWatcherID = Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, new BufferWatcher(this.plugin), 300);
	}
	/**
	 * Returns a new connection to execute SQL statements on.
	 * @return A new connection to execute SQL statements on.
	 */
	public Connection getConnection(){
		try{
			//If we have a current connection, fetch it
			if(this.connection != null && !this.connection.isClosed()){
				return this.connection;
			}
		}
		catch(SQLException e){
			e.printStackTrace();
			plugin.getLogger().severe("Could not retrieve SQLite connection!");
		}
		
		if(this.getFile().exists()){
			//So we need a new connection
			try{
				Class.forName("org.sqlite.JDBC");
				this.connection = DriverManager.getConnection("jdbc:sqlite:" + this.getFile());
				return this.connection;
			}
			catch (ClassNotFoundException e) {
				e.printStackTrace();
				return null;
			} catch (SQLException e) {
				e.printStackTrace();
				return null;
			}
		}
		else{
			//So we need a new file too.
			try {
				//Create the file
				this.getFile().createNewFile();
				//Now we won't need a new file, just a connection.
				//This will return that new connection.
				return this.getConnection();
			} catch (IOException e) {
				e.printStackTrace();
				plugin.getLogger().severe("Could not create database file!");
				return null;
			}
		}
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
	public boolean hasTable(String t){
		try {
			PreparedStatement ps = this.getConnection().prepareStatement("SELECT name FROM sqlite_master WHERE type='table' AND name='"+t+"';");
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
		final Database db = this;
		Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, new Runnable(){
			@Override
			public void run() {
				while(db.queriesInUse){
					//Wait
				}
				
				db.queriesInUse = true;
				db.queries.add(s);
				db.queriesInUse = false;

				//If the buffer isn't running yet, start it.
				if(db.bufferWatcherID == 0){
					db.startBufferWatcher();
				}
			}
			
		}, 0);
	}
	
	/**
	 * Creates the database table 'shops'.
	 * @throws SQLException If the connection is invalid.
	 */
	public void createShopsTable() throws SQLException{
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
				"\"unlimited\"  boolean, " +
				"\"type\"  boolean, " +
				"PRIMARY KEY ('x', 'y','z','world') " +
				");";
		st.execute(createTable);
	}
	
	public void createMessagesTable() throws SQLException{
		Statement st = getConnection().createStatement();
		String createTable = 
		"CREATE TABLE \"messages\" (" + 
				"\"owner\"  TEXT(20) NOT NULL, " +
				"\"message\"  TEXT(200) NOT NULL, " +
				"\"time\"  INTEGER(32) NOT NULL " +
				");";
		st.execute(createTable);
	}
	
	public void checkColumns(){
		PreparedStatement ps = null;
		try {
			ps = this.getConnection().prepareStatement(" ALTER TABLE shops ADD unlimited boolean");
			ps.execute();
			ps.close();
		} catch (SQLException e) {
			plugin.getLogger().info("Found unlimited");
		}
		try {
			ps = this.getConnection().prepareStatement(" ALTER TABLE shops ADD type int");
			ps.execute();
			ps.close();
		} catch (SQLException e) {
			plugin.getLogger().info("Found type column");
		}
	}
	
	public void stopBuffer(){
		Bukkit.getScheduler().cancelTask(bufferWatcherID);
	}
}