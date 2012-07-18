package org.maxgamer.QuickShop;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.bukkit.Bukkit;
import org.maxgamer.QuickShop.Database.Database;

public class BufferWatcher implements Runnable{
	public void run(){
		QuickShop plugin = (QuickShop) Bukkit.getPluginManager().getPlugin("QuickShop");
		
		Database db = plugin.getDB();
		
		Connection con = db.getConnection();
		try {
			Statement st = con.createStatement();
			while(plugin.queriesInUse){
				//Nothing
			}
			
			plugin.queriesInUse = true;
			for(String q : plugin.queries){
				st.addBatch(q);
			}
			plugin.queries.clear();
			plugin.queriesInUse = false;
			
			st.executeBatch();
			st.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
			plugin.getLogger().severe("Could not execute query");
		}
	}
}