package me.maplef.mapbotv4.utils;

import me.maplef.mapbotv4.exceptions.PlayerNotFoundException;
import me.maplef.mapbotv4.managers.ConfigManager;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class DatabaseOperator {
    ConfigManager configManager = new ConfigManager();
    FileConfiguration config = configManager.getConfig();

    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";

    final String MYSQL_HOST = config.getString("mysql-host");
    final String PORT = config.getString("mysql-port");
    final String DB_NAME = config.getString("mysql-database");
    final String DB_URL = "jdbc:mysql://" + MYSQL_HOST + ":" + PORT + "/" + DB_NAME;

    final String USERNAME = config.getString("mysql-username");
    final String PASSWORD = config.getString("mysql-password");

    private final Connection c = connect();

    public void init() throws SQLException{
        if(config.getBoolean("use-mysql")){
            PreparedStatement ps = c.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS PLAYER (" +
                    "    NAME    TEXT    NOT NULL," +
                    "    QQ      TEXT    NOT NULL," +
                    "    UUID    TEXT," +
                    "    MSGREC  BOOLEAN DEFAULT 1" +
                    ");");
            ps.execute();

            ps = c.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS cat_images (" +
                    "`id`               bigint      UNSIGNED NOT NULL AUTO_INCREMENT," +
                    "`uploaded_time`    timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "`uploader`         text        CHARACTER SET utf8 NULL," +
                    "`base64`           longtext    NOT NULL," +
                    "`url`              text        NULL," +
                    "`cat_name`         text        NULL," +
                    "PRIMARY KEY (`id`)" +
                    ");");
            ps.execute();

            ps.close();
        }
    }

    private Connection connect() {
        FileConfiguration config = configManager.getConfig();

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
                e.printStackTrace();
            }
            return conn;
        }
    }

    public static Map<String, Object> queryPlayer(Object arg) throws SQLException, PlayerNotFoundException {
        Map<String, Object> queryRes = new HashMap<>();

        String name = "[-]"; long QQ = -1L;
        if(arg instanceof String) name = (String) arg;
        else if (arg instanceof Long) QQ = (Long) arg;

        try(Statement stmt = new DatabaseOperator().getConnect().createStatement()){
            ResultSet res = stmt.executeQuery("SELECT * FROM PLAYER;");
            while(res.next()){
                if(res.getString("NAME").toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT)) || res.getLong("QQ") == QQ){
                    ResultSetMetaData data = res.getMetaData();
                    for(int i = 1; i <= data.getColumnCount(); ++i)
                        queryRes.put(data.getColumnName(i), res.getObject(data.getColumnName(i)));
                    return queryRes;
                }
            }
        }

        throw new PlayerNotFoundException();
    }

    public void executeCommand(String sqlCommand) throws SQLException{
        try(Statement stmt = c.createStatement()){
            stmt.execute(sqlCommand);
        }
    }

    public Connection getConnect(){
        return c;
    }

    public static Map<String, Object> queryExamine(long QQ) throws SQLException, PlayerNotFoundException {
        Map<String, Object> queryRes = new HashMap<>();

        try(Statement stmt = new DatabaseOperator().getConnect().createStatement()){
            ResultSet res = stmt.executeQuery("SELECT * FROM EXAMINE;");
            while(res.next()){
                if(res.getLong("QQ") == QQ){
                    if (res.getString("CODE").equals("null")) continue;
                    if (res.getString("CODE").equals("已退群")) continue;
                    ResultSetMetaData data = res.getMetaData();
                    for(int i = 1; i <= data.getColumnCount(); ++i)
                        queryRes.put(data.getColumnName(i), res.getObject(data.getColumnName(i)));
                    return queryRes;
                }
            }
        }

        throw new PlayerNotFoundException();
    }

    public static Map<String, Object> queryExaminePlus(String id) throws PlayerNotFoundException, SQLException {
        Map<String, Object> queryRes = new HashMap<>();

        try(Statement stmt = new DatabaseOperator().getConnect().createStatement()){
            ResultSet res = stmt.executeQuery("SELECT * FROM EXAMINE_PLUS;");
            while(res.next()){
                if(Objects.equals(res.getString("MSG"), id)){
                    ResultSetMetaData data = res.getMetaData();
                    for(int i = 1; i <= data.getColumnCount(); ++i)
                        queryRes.put(data.getColumnName(i), res.getObject(data.getColumnName(i)));
                    return queryRes;
                }
            }
        }

        throw new PlayerNotFoundException();
    }
}
