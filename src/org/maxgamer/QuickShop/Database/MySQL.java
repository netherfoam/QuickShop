package org.maxgamer.QuickShop.Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class MySQL implements DatabaseCore{
	/** The JDBC URL String... jdbc:mysql://host:port/database */
	private String url;
	/** The connection properties... user, pass, autoReconnect.. */
	private Properties info;
	/** The actual connection... possibly expired. */
	private Connection connection;
	
	public MySQL(String url, Properties info){
		this.url = url;
		this.info = info;
	}
	public MySQL(String host, String user, String pass, String database, String port){
		info = new Properties();
		info.put("autoReconnect", true);
		info.put("user", user);
		info.put("password", pass);
		this.url = "jdbc:mysql://"+host+":"+port+"/"+database;
	}
	
	
	/**
	 * Gets the database connection for
	 * executing queries on.
	 * @return The database connection
	 */
	public Connection getConnection(){
		try{
			//If we have a current connection, fetch it
			if(this.connection != null && !this.connection.isClosed()/* && this.connection.isValid(3)*/){
				return this.connection;
			}
			else{
				this.connection = DriverManager.getConnection(this.url, info);
				return this.connection;
			}
		}
		catch(SQLException e){
			e.printStackTrace();
			System.out.println("Could not retrieve SQLite connection!");
		}
		return null;
	}

	/**
	 * Prepares a query for the database by fixing 's (Only works for SQLite)
	 * @param s The string to escape E.g. can't do that :\
	 * @return The escaped string. E.g. can''t do that :\
	 */
	public String escape(String s) {
		s = s.replace("\\", "\\\\");
		s = s.replace("'", "\\'");
		return s;
	}
}