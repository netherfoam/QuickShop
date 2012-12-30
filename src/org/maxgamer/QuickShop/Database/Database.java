package org.maxgamer.QuickShop.Database;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.maxgamer.QuickShop.QuickShop;

public class Database {
	private DatabaseCore dbCore;
	private Buffer buffer;
	
	private DatabaseWatcher dbw;
	private BukkitTask task;
	
	public Database(File file){
		this.dbCore = new SQLite(file);
		this.buffer = new Buffer(this);
		this.dbw = new DatabaseWatcher(this);
	}
	
	/**
	 * Reschedules the db watcher
	 */
	public void scheduleWatcher(){
		this.task = Bukkit.getScheduler().runTaskLater(QuickShop.instance, this.dbw, 300);
	}
	
	public BukkitTask getTask(){
		return task;
	}
	public void setTask(BukkitTask task){
		this.task = task; 
	}
	
	public DatabaseWatcher getDatabaseWatcher(){
		return this.dbw;
	}
	
	public Buffer getBuffer(){
		return this.buffer;
	}
	
	/**
	 * Returns true if the table exists
	 * @param table The table to check for
	 * @return True if the table is found
	 */
	public boolean hasTable(String table){
		String query = "SELECT * FROM " + table + " LIMIT 0,1";
		try {
			PreparedStatement ps = this.getConnection().prepareStatement(query);
			ps.executeQuery();
			
			return true;
		} catch (SQLException e) {
			return false;
		}
	}
	/** Returns true if the given table has the given column. Case sensitive */
	public boolean hasColumn(String table, String column){
		String query = "SELECT * FROM " + table + " LIMIT 0,1";
		try{
			PreparedStatement ps = this.getConnection().prepareStatement(query);
			ResultSet rs = ps.executeQuery();
			for(int i = 1; i < rs.getMetaData().getColumnCount(); i++){
				String name = rs.getMetaData().getColumnName(i);
				if(name.equals(column)){
					return true;
				}
			}
			return false; //Column was not found...
		} 
		catch(SQLException e){
			return false;
		}
	}
	
	/** Queues the given query in the buffer to be executed in the near future. */
	public void execute(String q){
		this.getBuffer().addString(q);
	}

	/**
	 * Gets the database connection for
	 * executing queries on.
	 * @return The database connection
	 */
	public Connection getConnection(){
		return this.dbCore.getConnection();
	}
	
	public String escape(String s){
		return this.dbCore.escape(s);
	}
	
	/** Debugs the database and prints the columns */
	public void debug(){
		try{
			ResultSet rs = this.getConnection().prepareStatement("SELECT * FROM shops").executeQuery();
			StringBuilder sb = new StringBuilder(rs.getMetaData().getColumnName(1));
			
			for(int i = 2; i <= rs.getMetaData().getColumnCount(); i++){
				sb.append(" | " + rs.getMetaData().getColumnName(i));
			}
			
			System.out.println("Columns: " + sb.toString());
		}
		catch(SQLException e){
			System.out.println("Could not fetch DB columns");
		}
	}
}
