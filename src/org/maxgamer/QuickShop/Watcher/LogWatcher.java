package org.maxgamer.QuickShop.Watcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.maxgamer.QuickShop.QuickShop;

public class LogWatcher implements Runnable{
	private QuickShop plugin;
	private PrintStream ps;
	
	private List<String> logs = new ArrayList<String>(5);
	private boolean lock = false;
	
	public LogWatcher(QuickShop plugin, File log){
		this.plugin = plugin;
		try {
			if(!log.exists()){
				log.createNewFile();
			}
			this.ps = new PrintStream(log);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			plugin.getLogger().severe("Log file not found!");
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
}