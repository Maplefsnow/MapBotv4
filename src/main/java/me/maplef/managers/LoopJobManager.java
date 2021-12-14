package me.maplef.managers;

import me.maplef.Main;
import me.maplef.loops.*;
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
            CronExpression inner_group_invite_cron = new CronExpression(Objects.requireNonNull(config.getString("inner-group-invite-cron")));
            CronExpression inner_group_kick_cron = new CronExpression(Objects.requireNonNull(config.getString("inner-group-kick-cron")));

            Scheduler.registerJob("Morning", String.valueOf(morning_cron), GoodMorning.class);
            Scheduler.registerJob("Night", String.valueOf(night_cron), GoodNight.class);
            Scheduler.registerJob("tpsCheck", String.valueOf(tpsCheck_cron), TPSCheck.class);
            Scheduler.registerJob("InnerGroupInvite", String.valueOf(inner_group_invite_cron), InnerGroupInvite.class);
            Scheduler.registerJob("InnerGroupKick", String.valueOf(inner_group_kick_cron), InnerGroupKick.class);
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
