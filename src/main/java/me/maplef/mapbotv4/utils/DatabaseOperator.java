package me.maplef.mapbotv4.utils;

import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.exceptions.PlayerNotFoundException;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DatabaseOperator {
    static final FileConfiguration config = Main.getInstance().getConfig();

    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";

    static final String MYSQL_HOST = config.getString("mysql-host");
    static final String PORT = config.getString("mysql-port");
    static final String DB_NAME = config.getString("mysql-database");
    static final String DB_URL = "jdbc:mysql://" + MYSQL_HOST + ":" + PORT + "/" + DB_NAME;

    static final String USERNAME = config.getString("mysql-username");
    static final String PASSWORD = config.getString("mysql-password");

    public static final Connection c = connect();

    public static void init() throws SQLException{
        if(config.getBoolean("use-mysql")){
            PreparedStatement ps = c.prepareStatement("CREATE TABLE IF NOT EXISTS PLAYER (" +
                    "    NAME    TEXT    NOT NULL," +
                    "    QQ      TEXT    NOT NULL," +
                    "    UUID    TEXT," +
                    "    KEEPINV BOOLEAN DEFAULT 0," +
                    "    MSGREC  BOOLEAN DEFAULT 1" +
                    ");");
            ps.execute();
            ps.close();
        } else {
            PreparedStatement ps = c.prepareStatement("CREATE TABLE IF NOT EXISTS PLAYER (" +
                    "    NAME    TEXT    NOT NULL," +
                    "    QQ      TEXT    NOT NULL," +
                    "    UUID    TEXT," +
                    "    KEEPINV BOOLEAN DEFAULT (0)," +
                    "    MSGREC  BOOLEAN DEFAULT (1)," +
                    "    PRIMARY KEY (" +
                    "        NAME" +
                    "    )" +
                    ");");
            ps.execute();
            ps.close();
        }
    }

    private static Connection connect() {
        if(config.getBoolean("use-mysql")){
            Connection conn = null;
            try{
                Class.forName(JDBC_DRIVER);
                conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
            } catch (Exception e){
                e.printStackTrace();
            }
            return conn;
        } else {
            String url = "jdbc:sqlite:.\\plugins\\MapBot\\database.db";
            Connection conn = null;
            try {
                conn = DriverManager.getConnection(url);
            } catch (SQLException e) {
                Bukkit.getLogger().warning(e.getClass().getName() + ": " + e.getMessage());
            }
            return conn;
        }
    }

    public static Map<String, Object> query(Object arg) throws SQLException, PlayerNotFoundException {
        Map<String, Object> queryRes = new HashMap<>();

        String name = "[-]"; long QQ = -1L; boolean found = false;
        if(arg instanceof String) name = (String) arg;
        else if (arg instanceof Long) QQ = (Long) arg;

        try(Statement stmt = c.createStatement()){
            ResultSet res = stmt.executeQuery("SELECT * FROM PLAYER;");
            while(res.next()){
                if(res.getString("NAME").toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT)) || res.getLong("QQ") == QQ){
                    found = true;
                    ResultSetMetaData data = res.getMetaData();
                    for(int i = 1; i <= data.getColumnCount(); ++i)
                        queryRes.put(data.getColumnName(i), res.getObject(data.getColumnName(i)));
                    break;
                }
            }
        }

        if(found) return queryRes;
        else throw new PlayerNotFoundException();
    }

    public static void executeCommand(String sqlCommand) throws SQLException{
        try(Statement stmt = c.createStatement()){
            stmt.execute(sqlCommand);
        }
    }
}
