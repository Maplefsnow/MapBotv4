package me.maplef.mapbotv4.utils;

import me.maplef.mapbotv4.exceptions.PlayerNotFoundException;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DatabaseOperator {
    public static final Connection c = connect();

    public static void init() throws SQLException{
        Statement stmt = c.createStatement();
        String sqlCommand = "CREATE TABLE PLAYER (" +
                "    NAME    TEXT    NOT NULL," +
                "    QQ      TEXT    NOT NULL," +
                "    UUID    TEXT," +
                "    KEEPINV BOOLEAN DEFAULT (0)," +
                "    MSGREC  BOOLEAN DEFAULT (1)," +
                "    PRIMARY KEY (" +
                "        NAME" +
                "    )" +
                ");";
        stmt.executeUpdate(sqlCommand);
        stmt.close();
    }

    private static Connection connect() {
        String url = "jdbc:sqlite:.\\plugins\\MapBot\\database.db";
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            Bukkit.getLogger().warning(e.getClass().getName() + ": " + e.getMessage());
        }
        return conn;
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
