package me.maplef.mapbotv4.loops;

import me.maplef.mapbotv4.managers.ConfigManager;
import me.maplef.mapbotv4.utils.BotOperator;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

public class TPSCheck implements Job {
    ConfigManager configManager = new ConfigManager();

    private static int tpsLowCount = 0;
    private static boolean tpsWarnFlag = false;

    @Override
    public void execute(JobExecutionContext context){
        FileConfiguration config = configManager.getConfig();
        long opGroup = config.getLong("op-group");
        double tpsThreshold = config.getDouble("tps-check.threshold");

        double tps = Bukkit.getServer().getTPS()[0];

        if (tps < tpsThreshold) {
            if(tpsLowCount < 2){
                BotOperator.sendGroupMessage(opGroup, String.format("检测到服务器tps已低于%.1f，当前tps: %.1f", tpsThreshold, tps));
                tpsLowCount++;
            } else {
                if(!tpsWarnFlag){
                    BotOperator.sendGroupMessage(opGroup, String.format("检测到服务器tps已低于%.1f，当前tps: %.1f", tpsThreshold, tps));
                    BotOperator.sendGroupMessage(opGroup, String.format("服务器tps已连续3次检测低于%.1f，将暂停tps告警直至tps回升至%.1f以上", tpsThreshold, tpsThreshold));
                    tpsWarnFlag = true;
                }
            }
        } else {
            tpsLowCount = 0; tpsWarnFlag = false;
        }
    }
}
