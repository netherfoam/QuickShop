package org.maxgamer.QuickShop.Economy;

import org.bukkit.Bukkit;

public class Economy_Vault implements Economy_Core{
	private net.milkbowl.vault.economy.Economy vault;
	public Economy_Vault(){
		setupEconomy();
	}
	
	/**
	 * Sets up the vault economy for hooking into & purchases.
	 * @return True is success
	 */
	private boolean setupEconomy(){
		org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> economyProvider = Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
        	vault = economyProvider.getProvider();
        }

        return (vault != null);
    }
	
	@Override
	public boolean isValid(){
		return vault != null;
	}

	@Override
	public boolean deposit(String name, double amount) {
		return vault.depositPlayer(name, amount).transactionSuccess();
	}

	@Override
	public boolean withdraw(String name, double amount) {
		return vault.withdrawPlayer(name, amount).transactionSuccess();
	}

	@Override
	public boolean transfer(String from, String to, double amount) {
		if(vault.getBalance(from) >= amount){ //Does the payer have enough money?
			if(vault.withdrawPlayer(from, amount).transactionSuccess()){ //Try and take money from their account.
				if(vault.depositPlayer(to, amount).transactionSuccess() == false){ //Successfully took money from their account
					vault.depositPlayer(from, amount); //Couldn't pay the other guy for some reason though, so return the cash
					return false;
				}
				return true; //Successfully took money from payer and successfully gave money to receiver
			}
			return false; //Failed to withdraw money
		}
		return false; //Not a high enough balance.
	}

	@Override
	public double getBalance(String name) {
		return vault.getBalance(name);
	}

	@Override
	public String format(double balance) {
		try{
			return vault.format(balance);
		}
		catch(NumberFormatException e){
			return "$" + balance;
		}
	}
	
}