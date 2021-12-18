package me.maplef.mapbotv4.loops;

import org.bukkit.Bukkit;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

public class OnlinePlayerRecorder implements Job {

    @Override
    public void execute(JobExecutionContext context){
        String url = "jdbc:sqlite:.\\plugins\\MapBot\\player_count.db";
        Connection c;
        try {
            c = DriverManager.getConnection(url);
        } catch (SQLException e) {
            Bukkit.getLogger().warning(e.getClass().getName() + ": " + e.getMessage());
            return;
        }

        int count = Bukkit.getServer().getOnlinePlayers().size();

        Date nowTime = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String timeStr = ft.format(nowTime);

        try(Statement stmt = c.createStatement()){
            String sqlCommand = String.format("INSERT INTO COUNT (TIME, NUM) VALUES ('%s', %d)", timeStr, count);
            stmt.executeUpdate(sqlCommand);
            c.close();
        } catch (Exception exception){
            Bukkit.getLogger().warning(exception.getClass().getName() + ": " + exception.getMessage());
        }
    }
}
