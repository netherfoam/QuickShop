package org.maxgamer.QuickShop.Database_old;

import java.sql.Connection;

public interface DatabaseCore{
	/** Returns an active connection to the database */
	public Connection getConnection();
	/** Escapes the given String and returns it */
	public String escape(String s);
}