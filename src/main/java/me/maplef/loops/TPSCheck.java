package me.maplef.loops;

import me.clip.placeholderapi.PlaceholderAPI;
import me.maplef.utils.BotOperator;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

public class TPSCheck implements Job {
    final FileConfiguration config = me.maplef.Main.getPlugin(me.maplef.Main.class).getConfig();
    private static int tpsLowCount = 0;
    private static boolean tpsWarnFlag = false;
    private final Long opGroup = config.getLong("op-group");
    private final double tpsThreshold = config.getDouble("tps-warning-threshold");

    @Override
    public void execute(JobExecutionContext context){
        String tps;
        if(Bukkit.getServer().getOnlinePlayers().toArray().length == 0){
            return;
        }
        tps = "%server_tps_1%";
        Player player = (Player) Bukkit.getServer().getOnlinePlayers().toArray()[0];
        tps = PlaceholderAPI.setPlaceholders(player, tps);
        if (Float.parseFloat(tps) < tpsThreshold) {
            if(tpsLowCount < 2){
                BotOperator.send(opGroup, String.format("检测到服务器tps已低于%.1f，当前tps: %s", tpsThreshold, tps));
                tpsLowCount++;
            } else {
                if(!tpsWarnFlag){
                    BotOperator.send(opGroup, String.format("检测到服务器tps已低于%.1f，当前tps: %s", tpsThreshold, tps));
                    BotOperator.send(opGroup, String.format("服务器tps已连续3分钟低于%.1f，将暂停tps告警直至tps回升至%.1f以上", tpsThreshold, tpsThreshold));
                    tpsWarnFlag = true;
                }
            }
        } else {
            tpsLowCount = 0; tpsWarnFlag = false;
        }
    }
}
