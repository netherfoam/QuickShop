package org.maxgamer.QuickShop.Watcher;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.maxgamer.QuickShop.QuickShop;
import org.maxgamer.QuickShop.Database.Database;

/**
 * @author Netherfoam
 * Maintains the buffer, and safely puts it in the database when possible.
 */
public class BufferWatcher implements Runnable{
	private QuickShop plugin;
	public BufferWatcher(QuickShop plugin){
		this.plugin = plugin;
	}
	
	public void run(){
		//TODO: Can this be optimised as an argument?
		//QuickShop plugin = (QuickShop) Bukkit.getPluginManager().getPlugin("QuickShop");
		
		Database db = plugin.getDB();
		
		Connection con = db.getConnection();
		try {
			Statement st = con.createStatement();
			while(plugin.getDB().queriesInUse){
				//Nothing
			}
			
			plugin.getDB().queriesInUse = true;
			for(String q : plugin.getDB().queries){
				st.addBatch(q);
			}
			plugin.getDB().queries.clear();
			
			db.bufferWatcherID = 0;
			
			plugin.getDB().queriesInUse = false;
			
			st.executeBatch();
			st.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
			plugin.getLogger().severe("Could not execute query");
			db.bufferWatcherID = 0;
		}
	}
}