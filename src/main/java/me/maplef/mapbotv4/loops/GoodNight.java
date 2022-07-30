package me.maplef.mapbotv4.loops;

import me.maplef.mapbotv4.managers.ConfigManager;
import me.maplef.mapbotv4.utils.BotOperator;
import org.bukkit.configuration.file.FileConfiguration;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.util.List;

public class GoodNight implements Job {
    ConfigManager configManager = new ConfigManager();

    FileConfiguration config = configManager.getConfig();
    FileConfiguration messages = configManager.getMessageConfig();
    private final long groupID = config.getLong("player-group");

    @Override
    public void execute(JobExecutionContext context){
        List<String> msg;
        msg = messages.getStringList("good-night-message");
        for(String i : msg){
            BotOperator.sendGroupMessage(groupID, i);
        }
    }
}
