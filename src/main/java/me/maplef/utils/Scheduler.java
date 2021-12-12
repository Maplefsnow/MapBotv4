package me.maplef.utils;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

public class Scheduler {
    static final SchedulerFactory schedulerFactory = new StdSchedulerFactory();
    public static org.quartz.Scheduler scheduler;

    static {
        try {
            scheduler = schedulerFactory.getScheduler();
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    public static void registerJob(String name, String cron, Class<? extends Job> cls) throws SchedulerException {
        ScheduleBuilder<CronTrigger> builder = CronScheduleBuilder.cronSchedule(cron);
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(name)
                .withSchedule(builder)
                .startNow()
                .build();
        JobDetail detail = JobBuilder.newJob(cls)
                .withIdentity(name)
                .build();
        scheduler.scheduleJob(detail, trigger);
    }
}
