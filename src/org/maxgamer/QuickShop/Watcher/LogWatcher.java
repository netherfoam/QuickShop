package org.maxgamer.QuickShop.Watcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.scheduler.BukkitTask;
import org.maxgamer.QuickShop.QuickShop;

public class LogWatcher implements Runnable{
	private PrintStream ps;
	public int taskId = 0;
	
	private List<String> logs = new ArrayList<String>(5);
	private boolean lock = false;
	public BukkitTask task;
	
	public LogWatcher(QuickShop plugin, File log){
		try {
			if(!log.exists()){
				log.createNewFile();
			}
			FileOutputStream fos = new FileOutputStream(log, true);
			this.ps = new PrintStream(fos);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			plugin.getLogger().severe("Log file not found!");
		} catch (IOException e) {
			e.printStackTrace();
			plugin.getLogger().severe("Could not create log file!");
		}
	}

	@Override
	public void run() {
		while(lock){
			//Nothing
		}
		lock = true;
		
		for(String s : logs){
			ps.println(s);
		}
		logs.clear();
		
		lock = false;
	}
	
	public void add(String s){
		while(lock){
			//Nothing
		}
		lock = true;
		logs.add(s);
		lock = false;
	}
	
	public void close(){
		this.ps.close();
	}
}