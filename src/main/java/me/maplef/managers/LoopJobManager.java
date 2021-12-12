package me.maplef.managers;

import me.maplef.loops.GoodMorning;
import me.maplef.loops.GoodNight;
import me.maplef.loops.OnlinePlayerRecorder;
import me.maplef.loops.TPSCheck;
import me.maplef.Main;
import me.maplef.utils.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.quartz.CronExpression;
import org.quartz.SchedulerException;

import java.text.ParseException;
import java.util.Objects;

public class LoopJobManager {
    static final FileConfiguration config = Main.getInstance().getConfig();

    public static void register(){
        try{
            CronExpression morning_cron = new CronExpression(Objects.requireNonNull(config.getString("good-morning-cron")));
            CronExpression night_cron = new CronExpression(Objects.requireNonNull(config.getString("good-night-cron")));
            CronExpression tpsCheck_cron = new CronExpression(Objects.requireNonNull(config.getString("tps-check-cron")));

            Scheduler.registerJob("Morning", String.valueOf(morning_cron), GoodMorning.class);
            Scheduler.registerJob("Night", String.valueOf(night_cron), GoodNight.class);
            Scheduler.registerJob("tpsCheck", String.valueOf(tpsCheck_cron), TPSCheck.class);
//            Scheduler.registerJob("InnerGroupInvite", "0 0/1 * * * ?", InnerGroupInvite.class);
            Scheduler.registerJob("OnlinePlayerRecorder", "0 0/10 * * * ?", OnlinePlayerRecorder.class);

            Scheduler.scheduler.start();
        } catch (SchedulerException exception){
            try{
                Scheduler.scheduler.shutdown();
            } catch (SchedulerException exception1){
                Bukkit.getLogger().warning(exception1.getClass() + ": " + exception1.getMessage());
            }
            Bukkit.getLogger().warning(exception.getClass() + ": " + exception.getMessage());
        } catch (ParseException e){
            e.printStackTrace();
        }
    }
}
