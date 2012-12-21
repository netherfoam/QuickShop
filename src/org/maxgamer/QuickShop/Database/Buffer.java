package org.maxgamer.QuickShop.Database;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.maxgamer.QuickShop.QuickShop;

public class Buffer {
	private Database db;
	public boolean locked = false;
	
	public List<String> queries = new ArrayList<String>(5);
	
	public Buffer(Database db){
		this.db = db;
	}
	
	/**
	 * Adds a query to the buffer
	 * @param q The query to add.  This should be sanitized beforehand.
	 */
	public void addString(final String q){
		Runnable r = new Runnable(){
			public void run() {
				while(locked){
					try {
						//1 millisecond
						Thread.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				locked = true;
				queries.add(q);
				locked = false;
				
				if(db.getTask() == null){
					//Database watcher isnt running yet, start it again.
					db.scheduleWatcher();
				}
			}
		};
		Bukkit.getScheduler().runTaskAsynchronously(QuickShop.instance, r);
	}
}
