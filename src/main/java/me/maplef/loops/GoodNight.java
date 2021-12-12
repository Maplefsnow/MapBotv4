package me.maplef.loops;

import me.maplef.Main;
import me.maplef.utils.BotOperator;
import org.bukkit.configuration.file.FileConfiguration;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.util.List;

public class GoodNight implements Job {
    final FileConfiguration config = me.maplef.Main.getPlugin(me.maplef.Main.class).getConfig();
    final FileConfiguration messages = Main.getInstance().getMessageConfig();
    private final long groupID = config.getLong("player-group");

    @Override
    public void execute(JobExecutionContext context){
        List<String> msg;
        msg = messages.getStringList("good-night-message");
        for(String i : msg){
            BotOperator.send(groupID, i);
        }
    }
}
