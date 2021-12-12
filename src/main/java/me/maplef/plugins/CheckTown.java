package me.maplef.plugins;
import me.maplef.utils.DatabaseOperator;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import java.sql.*;
import java.util.Locale;

public class CheckTown implements Listener {
    public String SendTownInfo(String townName) {
        try{
            Connection c = DatabaseOperator.c; Statement stmt = c.createStatement();

            ResultSet townRes = stmt.executeQuery("SELECT * FROM TOWN;");

            String engName = "", chnName = "", author = "", mainLocation = "", netherLocation = "";
            while(townRes.next()){
                if(townRes.getString("ENGNAME").contains(townName.toLowerCase(Locale.ROOT)) || townRes.getString("CHNNAME").contains(townName)){
                    engName = townRes.getString("ENGNAME");
                    chnName = townRes.getString("CHNNAME");
                    author = townRes.getString("AUTHOR");
                    mainLocation = townRes.getString("MAINLOCATION");
                    netherLocation = townRes.getString("NETHERLOCATION");
                }
            }
            if(engName.equals("")){
                return "not found";
            }
            String msg = String.format("[%s]\n镇长: %s\n主坐标: %s\n地狱坐标: %s\n居民: ", chnName, author, mainLocation, netherLocation);

            ResultSet playerRes = stmt.executeQuery("SELECT * FROM PLAYER;");
            while(playerRes.next()){
                if(playerRes.getString("ADDRESS") != null && playerRes.getString("ADDRESS").equals(engName)){
                    msg = msg.concat(playerRes.getString("NAME")).concat(", ");
                }
            }

            stmt.close(); townRes.close(); playerRes.close();

            msg = msg.substring(0, msg.length() - 2);
            return msg;
        } catch (Exception e){
            Bukkit.getLogger().warning(e.getClass().getName() + ": " + e.getMessage());
            return "fail";
        }
    }
}
