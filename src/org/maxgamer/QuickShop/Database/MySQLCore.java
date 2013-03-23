package org.maxgamer.QuickShop.Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;


public class MySQLCore implements DatabaseCore{
	private String url;
	/** The connection properties... user, pass, autoReconnect.. */
	private Properties info;
	/** The actual connection... possibly expired. */
	private Connection connection;
	
	public MySQLCore(String host, String user, String pass, String database, String port){
		info = new Properties();
		info.put("autoReconnect", "true");
		info.put("user", user);
		info.put("password", pass);
		info.put("useUnicode", "true");
		info.put("characterEncoding", "utf8");
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
			if(this.connection != null && !this.connection.isClosed()){
				if(this.connection.isValid(10)){
					return this.connection;
				}
				//Else, it is invalid, so we return another connection.
			}
			this.connection = DriverManager.getConnection(this.url, info);
			return this.connection;
		}
		catch(SQLException e){
			e.printStackTrace();
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


	@Override
	public void queue(BufferStatement bs) {
		try{
			PreparedStatement ps = bs.prepareStatement(this.getConnection());
			ps.execute();
			ps.close();
		}
		catch(SQLException e){
			e.printStackTrace();
			return;
		}
	}


	@Override
	public void close() {
		//Nothing, because queries are executed immediately for MySQL
	}
	
	@Override
	public void flush(){
		//Nothing, because queries are executed immediately for MySQL
	}
}