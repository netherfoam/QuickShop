package org.maxgamer.QuickShop.Metrics;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.maxgamer.QuickShop.QuickShop;
import org.maxgamer.QuickShop.Metrics.Metrics.Graph;
import org.maxgamer.QuickShop.Shop.ShopPurchaseEvent;

public class ShopListener implements Listener{
	int sales = 0;
	int purchases = 0;
	
	public ShopListener(){
		Metrics metrics = QuickShop.instance.getMetrics();
		
		Graph graph = metrics.createGraph("Sales vs Purchases");

		graph.addPlotter(new Metrics.Plotter("Sales") {
			@Override
			public int getValue() {
				return sales;
			}
		});
		
		graph.addPlotter(new Metrics.Plotter("Purchases") {
			@Override
			public int getValue() {
				return purchases;
			}
		});
	}
	
	@EventHandler
	public void onPurchase(ShopPurchaseEvent e){
		if(e.getShop().isSelling()) sales += e.getAmount();
		else purchases += e.getAmount();
	}
}