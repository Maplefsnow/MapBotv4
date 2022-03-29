package me.maplef.mapbotv4.managers;

import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.loops.*;
import me.maplef.mapbotv4.utils.Scheduler;
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
            CronExpression morning_cron = new CronExpression(Objects.requireNonNull(config.getString("daily-greetings.morning.cron")));
            CronExpression night_cron = new CronExpression(Objects.requireNonNull(config.getString("daily-greetings.night.cron")));
            CronExpression tpsCheck_cron = new CronExpression(Objects.requireNonNull(config.getString("tps-check.cron")));
            CronExpression inner_group_invite_cron = new CronExpression(Objects.requireNonNull(config.getString("inner-player-group-auto-manage.invite.cron")));
            CronExpression inner_group_kick_cron = new CronExpression(Objects.requireNonNull(config.getString("inner-player-group-auto-manage.kick.cron")));
            CronExpression online_player_record_cron = new CronExpression(Objects.requireNonNull(config.getString("online-player-record.cron")));

            if(config.getBoolean("tps-check.enable"))
                Scheduler.registerJob("tpsCheck", String.valueOf(tpsCheck_cron), TPSCheck.class);
            if(config.getBoolean("daily-greetings.morning.enable"))
                Scheduler.registerJob("Morning", String.valueOf(morning_cron), GoodMorning.class);
            if(config.getBoolean("daily-greetings.night.enable"))
                Scheduler.registerJob("Night", String.valueOf(night_cron), GoodNight.class);
            if(config.getBoolean("inner-player-group-auto-manage.invite.enable"))
                Scheduler.registerJob("InnerGroupInvite", String.valueOf(inner_group_invite_cron), InnerGroupInvite.class);
            if(config.getBoolean("inner-player-group-auto-manage.kick.enable"))
                Scheduler.registerJob("InnerGroupKick", String.valueOf(inner_group_kick_cron), InnerGroupKick.class);
            if(config.getBoolean("online-player-record.enable"))
                Scheduler.registerJob("OnlinePlayerRecorder", String.valueOf(online_player_record_cron), OnlinePlayerRecorder.class);

            Scheduler.scheduler.start();
        } catch (SchedulerException exception){
            Bukkit.getLogger().warning(exception.getClass() + ": " + exception.getMessage());
            try{
                Scheduler.scheduler.shutdown();
            } catch (SchedulerException exception1){
                Bukkit.getLogger().warning(exception1.getClass() + ": " + exception1.getMessage());
            }
        } catch (ParseException e){
            e.printStackTrace();
        }
    }
}
