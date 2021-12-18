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

            if(config.getBoolean("daily-greetings.morning.enabled"))
                Scheduler.registerJob("Morning", String.valueOf(morning_cron), GoodMorning.class);
            if(config.getBoolean("daily-greetings.night.enabled"))
                Scheduler.registerJob("Night", String.valueOf(night_cron), GoodNight.class);
            if(config.getBoolean("tps-check.enabled"))
                Scheduler.registerJob("tpsCheck", String.valueOf(tpsCheck_cron), TPSCheck.class);
            if(config.getBoolean("inner-player-group-auto-manage.invite.enabled"))
                Scheduler.registerJob("InnerGroupInvite", String.valueOf(inner_group_invite_cron), InnerGroupInvite.class);
            if(config.getBoolean("inner-player-group-auto-manage.kick.enabled"))
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
