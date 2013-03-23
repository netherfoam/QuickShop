package org.maxgamer.QuickShop.Database_old;

import java.sql.PreparedStatement;
import java.sql.SQLException;
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
			while(db.getBuffer().queries.size() > 0){
				BufferStatement bs = db.getBuffer().queries.remove(0);
				if(bs == null){
					System.out.println("Null statement found! Skipping!");
					continue;
				}
				history.add(bs.toString());
				
				PreparedStatement ps = bs.prepareStatement(db.getConnection());
				ps.execute();
			}
			//We can release this now
			db.getBuffer().locked = false;
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
