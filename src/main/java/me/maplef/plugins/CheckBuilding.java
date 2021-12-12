package me.maplef.plugins;

import me.maplef.utils.DatabaseOperator;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckBuilding {
    public String SendBuildingInfo(String buildingName) {
        Connection c = DatabaseOperator.c;

        try(Statement stmt = c.createStatement();
            ResultSet res = stmt.executeQuery("SELECT * FROM BUILDING;")) {

            String msg = "", name, author, mainLocation, netherLocation;
            while (res.next()){
                if(res.getString("NAME").contains(buildingName)){
                    name = res.getString("NAME");
                    author = res.getString("AUTHOR");
                    mainLocation = res.getString("MAINLOCATION");
                    netherLocation = res.getString("NETHERLOCATION");

                    msg = String.format("[%s]", name);
                    if(author != null) msg = msg.concat(String.format("\n建造者: %s", author));
                    if(mainLocation != null) msg = msg.concat(String.format("\n主坐标: (%s)", mainLocation));
                    if(netherLocation != null) msg = msg.concat(String.format("\n地狱坐标: (%s)", netherLocation));
                    msg = msg.concat("\n");
                }
            }
            if(msg.equals("")) return "未找到该建筑";

            msg = msg.substring(0, msg.length() - 1);

            return msg;
        } catch (Exception e){
            Bukkit.getLogger().warning(e.getClass().getName() + ": " + e.getMessage());
            return "fail";
        }
    }
}