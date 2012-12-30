package org.maxgamer.QuickShop.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;

public class BufferStatement{
	private Object[] values;
	private String query;
	
	/**
	 * Represents a PreparedStatement in a state before preparing it (E.g. No file I/O Required)
	 * @param query The query to execute. E.g. INSERT INTO accounts (user, passwd) VALUES (?, ?)
	 * @param values The values to replace <bold>?</bold> with in <bold>query</bold>.  These are in order.
	 */
	public BufferStatement(String query, Object... values){
		this.query = query;
		this.values = values;
	}
	/**
	 * Returns a prepared statement using the given connection.
	 * Will try to return an empty statement if something went wrong.
	 * If that fails, returns null.
	 * 
	 * This method escapes everything automatically.
	 * 
	 * @param con The connection to prepare this on using con.prepareStatement(..)
	 * @return The prepared statement, ready for execution.
	 */
	public PreparedStatement prepareStatement(Connection con){
		try {
			PreparedStatement ps;
			ps = con.prepareStatement(query);
			for(int i = 1; i <= values.length; i++){
				ps.setObject(i, values[i-1]);
			}
			return ps;
		} 
		catch (SQLException e) {
			e.printStackTrace();
			System.out.println("Could not do query!");
			System.out.println(this.toString());
			
		}
		try {
			return con.prepareStatement("");
		} catch (SQLException e) {
			System.out.println("Could not return an empty statement! Something is REALLY wrong!");
			e.printStackTrace();
			return null;
		}
	}
	/**
	 * @return A string representation of this statement. Returns <italic>"Query: " + query + ", values: " + Arrays.toString(values).</italic>
	 */
	@Override
	public String toString(){
		return "Query: " + query + ", values: " + Arrays.toString(values);
	}
}