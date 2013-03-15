package org.maxgamer.QuickShop.Database;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.maxgamer.QuickShop.QuickShop;

public class Database {
	private DatabaseCore dbCore;
	private Buffer buffer;
	
	private DatabaseWatcher dbw;
	private BukkitTask task;
	
	private Database(){
		this.buffer = new Buffer(this);
		this.dbw = new DatabaseWatcher(this);
	}
	
	/**
	 * Creates a new SQLite based database.
	 * @param file The SQLite file to use.
	 */
	public Database(File file){
		this();
		this.dbCore = new SQLite(file);
	}
	
	/**
	 * The database core (MySQL.class or SQLite.class at time of writing)
	 * @return The database core (MySQL.class or SQLite.class at time of writing)
	 */
	public DatabaseCore getCore(){ return this.dbCore; }
	
	/**
	 * Creates a new MySQL database connection.
	 * @param host The host to connect to
	 * @param port The port to connect using (usually 3306)
	 * @param dbName The database name (E.g. QuickShop)
	 * @param user The username to connect with
	 * @param pass The password to connect with
	 */
	public Database(String host, String port, String dbName, String user, String pass){
		this();
		this.dbCore = new MySQL(host, user, pass, dbName, port);
	}
	
	/**
	 * Reschedules the db watcher
	 */
	public void scheduleWatcher(){
		this.task = Bukkit.getScheduler().runTaskLaterAsynchronously(QuickShop.instance, this.dbw, 300);
	}
	
	public BukkitTask getTask(){
		return task;
	}
	public void setTask(BukkitTask task){
		this.task = task; 
	}
	
	public DatabaseWatcher getDatabaseWatcher(){
		return this.dbw;
	}
	
	public Buffer getBuffer(){
		return this.buffer;
	}
	
	/**
	 * Returns true if the table exists
	 * @param table The table to check for
	 * @return True if the table is found
	 */
	public boolean hasTable(String table){
		String query = "SELECT * FROM " + table + " LIMIT 0,1";
		try {
			PreparedStatement ps = this.getConnection().prepareStatement(query);
			ps.executeQuery();
			
			return true;
		} catch (SQLException e) {
			return false;
		}
	}
	/** Returns true if the given table has the given column. Case sensitive */
	public boolean hasColumn(String table, String column){
		String query = "SELECT * FROM " + table + " LIMIT 0,1";
		try{
			PreparedStatement ps = this.getConnection().prepareStatement(query);
			ResultSet rs = ps.executeQuery();
			for(int i = 1; i < rs.getMetaData().getColumnCount(); i++){
				String name = rs.getMetaData().getColumnName(i);
				if(name.equals(column)){
					return true;
				}
			}
			return false; //Column was not found...
		} 
		catch(SQLException e){
			return false;
		}
	}
	
	/** Queues the given query in the buffer to be executed in the near future. */
	public void execute(String query, Object... values){
		BufferStatement bs = new BufferStatement(query, values);
		this.getBuffer().addQuery(bs);
		
		if(QuickShop.instance.debug){
			System.out.println("Queuing " + bs.toString());
		}
	}

	/**
	 * Gets the database connection for
	 * executing queries on.
	 * @return The database connection
	 */
	public Connection getConnection(){
		return this.dbCore.getConnection();
	}
	
	public String escape(String s){
		return this.dbCore.escape(s);
	}
	
	/** Debugs the database and prints the columns */
	public void debug(){
		try{
			ResultSet rs = this.getConnection().prepareStatement("SELECT * FROM shops").executeQuery();
			StringBuilder sb = new StringBuilder(rs.getMetaData().getColumnName(1));
			
			for(int i = 2; i <= rs.getMetaData().getColumnCount(); i++){
				sb.append(" | " + rs.getMetaData().getColumnName(i));
			}
			
			System.out.println("Columns: " + sb.toString());
		}
		catch(SQLException e){
			System.out.println("Could not fetch DB columns");
		}
	}
	
	/**
	 * Copies the contents of this database into the given database. 
	 * Does not delete the contents of this database, or change any
	 * settings. The plugin will STILL USE *THIS* database, and not
	 * use the other.  This may take a long time, and will print out
	 * progress reports to System.out
	 * @param db The database to copy data to
	 * @throws SQLException if an error occurs.
	 */
	public void copyTo(Database db) throws SQLException{
		ResultSet rs = getConnection().getMetaData().getTables(null, null, "%", null);
		List<String> tables = new LinkedList<String>();
		while(rs.next()){
			tables.add(rs.getString("TABLE_NAME"));
		}
		rs.close();
		
		getDatabaseWatcher().run(); //Flush the current query set.
		db.createShopsTable();
		db.createMessagesTable();
		
		//For each table
		for(String table : tables){
			if(table.toLowerCase().startsWith("sqlite_autoindex_")) continue;
			System.out.println("Copying " + table);
			//Wipe the old records
			db.getConnection().prepareStatement("DELETE FROM " + table).execute();
			
			//Fetch all the data from the existing database
			rs = getConnection().prepareStatement("SELECT * FROM " + table).executeQuery();
			
			int n = 0;
			
			//Build the query
			String query = "INSERT INTO " + table + " VALUES (";
			//Append another placeholder for the value
			query += "?";
			for(int i = 2; i <= rs.getMetaData().getColumnCount(); i++){
				//Add the rest of the placeholders and values.  This is so we have (?, ?, ?) and not (?, ?, ?, ).
				query += ", ?";
			}
			//End the query
			query += ")";
			
			PreparedStatement ps = db.getConnection().prepareStatement(query);
			while(rs.next()){
				n++;
				
				for(int i = 1; i <= rs.getMetaData().getColumnCount(); i++){
					ps.setObject(i, rs.getObject(i));
				}
				
				ps.addBatch();
				
				if(n % 100 == 0){
					ps.executeBatch();
					System.out.println(n + " records copied...");
				}
			}
			ps.executeBatch();
			//Close the resultset of that table
			rs.close();
		}
		//Success!
		db.getConnection().close();
		this.getConnection().close();
	}
	
	/**
	 * Creates the database table 'shops'.
	 * @throws SQLException If the connection is invalid.
	 */
	public void createShopsTable() throws SQLException{
		Statement st = getConnection().createStatement();
		String createTable = 
		"CREATE TABLE shops (" + 
				"owner  TEXT(20) NOT NULL, " +
				"price  double(32, 2) NOT NULL, " +
				"item  BLOB NOT NULL, " +
				"x  INTEGER(32) NOT NULL, " +
				"y  INTEGER(32) NOT NULL, " +
				"z  INTEGER(32) NOT NULL, " +
				"world VARCHAR(32) NOT NULL, " +
				"unlimited  boolean, " +
				"type  boolean, " +
				"PRIMARY KEY (x, y, z, world) " +
				");";
		st.execute(createTable);
	}
	
	/**
	 * Creates the database table 'messages'
	 * @throws SQLException If the connection is invalid
	 */
	public void createMessagesTable() throws SQLException{
		Statement st = getConnection().createStatement();
		String createTable = 
		"CREATE TABLE messages (" + 
				"owner  TEXT(20) NOT NULL, " +
				"message  TEXT(200) NOT NULL, " +
				"time  BIGINT(32) NOT NULL " +
				");";
		st.execute(createTable);
	}
}
