package org.maxgamer.QuickShop.Economy;

import me.meta1203.plugins.satoshis.SatoshisEconAPI;
import me.meta1203.plugins.satoshis.Satoshis;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class Economy_Satoshis implements Economy_Core{
	SatoshisEconAPI econ;
	
	/**
	 * Creates a new Satoshis economy hook.
	*/
	public Economy_Satoshis(){
		Plugin plugin = Bukkit.getPluginManager().getPlugin("Satoshis");
		if(plugin == null){
			throw new NoClassDefFoundError("Satoshis was not found!");
		}
		else{
			Bukkit.getLogger().info("Hooking Economy");
			econ = Satoshis.econ;
		}
	}
	
	@Override
	public boolean isValid(){
		return true;
	}

	/**
	 * Deposits the given amount into the given usernames account.
	 * The account does not have to exist (It will be auto created).
	 * @param name The name of the player. Case insensitive.
	 * @param amount The amount to give the player.
	 * @return True if the deposit was successful
	 */
	@Override
	public boolean deposit(String name, double amount) {
		econ.addFunds(name, amount);
		return true;
	}

	/**
	 * Withdraws the given amount from the given usernames account.
	 * This method will fail if the player has less than the requested
	 * amount, and in this case will return false.
	 * @param name The name of the player. Case insensitive.
	 * @param amount The amount to take from the player
	 * @return True if successful.
	 */
	@Override
	public boolean withdraw(String name, double amount) {
		if(econ.getMoney(name) >= amount){
			econ.subFunds(name, amount);
			return true;
		}
		return false;
	}

	/**
	 * Transfers the give amount from Player1 to Player2.
	 * Returns false if Player1 hasn't got the required balance,
	 * or false if there was an issue paying Player2.
	 * @param from The player who is paying money
	 * @param to The player who is receiving money
	 * @return True if the transaction was a success. False if not.
	 */
	@Override
	public boolean transfer(String from, String to, double amount) {
		if(econ.getMoney(from) >= amount){
			econ.subFunds(from, amount);
			econ.addFunds(to, amount);
			return true;
		}
		return false;
	}

	/**
	 * Fetches the given players balance.
	 * @param name The name of the player
	 * @return Their current account holdings.
	 */
	@Override
	public double getBalance(String name) {
		return econ.getMoney(name);
	}

	/**
	 * Converts the given number into a cash amount.
	 * For example, 15.47 would become $15.47 or 15 Dollars
	 * 47 Cents.
	 * @param balance The given amount, e.g. 15.47
	 * @return The String representation of the currency.
	 */
	@Override
	public String format(double balance) {
		return econ.formatValue(balance, false);
	}
}