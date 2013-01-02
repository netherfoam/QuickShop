package org.maxgamer.QuickShop.Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQL implements DatabaseCore{
	private String url;
	private String user;
	private String pass;
	
	private Connection connection;
	
	public MySQL(String url, String user, String pass){
		this.url = url;
		this.user = user;
		this.pass = pass;
	}
	public MySQL(String host, String user, String pass, String database, String port){
		this("jdbc:mysql://"+host+":"+port+"/"+database, user, pass);
	}
	
	
	/**
	 * Gets the database connection for
	 * executing queries on.
	 * @return The database connection
	 */
	public Connection getConnection(){
		try{
			//If we have a current connection, fetch it
			if(this.connection != null && !this.connection.isClosed()){
				return this.connection;
			}
			else{
				this.connection = DriverManager.getConnection(this.url, user, pass);
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