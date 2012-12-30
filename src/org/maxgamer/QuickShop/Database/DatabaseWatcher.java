package org.maxgamer.QuickShop.Database;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;

import org.maxgamer.QuickShop.QuickShop;

public class DatabaseWatcher implements Runnable{
	private Database db;
	public DatabaseWatcher(Database db){
		this.db = db;
	}

	/**
	 * What to do every time the scheduled event is called
	 * - AKA, check buffer, run queries 
	 */
	public void run() {
		while(db.getBuffer().locked){
			try {
				//1 millisecond
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		//Lock it for use
		db.getBuffer().locked = true;
		
		LinkedList<String> history = new LinkedList<String>();
		try{
			Statement st = db.getConnection().createStatement();
			
			while(db.getBuffer().queries.size() > 0){
				String q = db.getBuffer().queries.remove(0);
				st.addBatch(q);
				history.add(q);
			}
			//We can release this now
			db.getBuffer().locked = false;
			
			st.executeBatch();
		}
		catch(SQLException e){
			e.printStackTrace();
			QuickShop.instance.getLogger().severe("Could not update database!");
			db.debug();
			QuickShop.instance.getLogger().severe("It was one of the following queries: " + history.toString());
		}
		//Ensure it's released
		db.getBuffer().locked = false;
		db.setTask(null);
		//Dont schedule the next one
		//This will be scheduled by bufferWatcher when a query is added.
	}
}
