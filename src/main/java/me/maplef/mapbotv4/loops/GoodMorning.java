package me.maplef.mapbotv4.loops;

import me.maplef.mapbotv4.managers.ConfigManager;
import me.maplef.mapbotv4.plugins.Hitokoto;
import me.maplef.mapbotv4.plugins.Weather;
import me.maplef.mapbotv4.plugins.WorldNews;
import me.maplef.mapbotv4.utils.BotOperator;
import org.bukkit.configuration.file.FileConfiguration;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

public class GoodMorning implements Job{
    ConfigManager configManager = new ConfigManager();
    FileConfiguration config = configManager.getConfig();
    FileConfiguration messages = configManager.getMessageConfig();

    @Override
    public void execute(JobExecutionContext context){
        WorldNews worldNews = new WorldNews();

        long groupID = config.getLong("player-group");

        BotOperator.sendGroupMessage(groupID, "现在是北京时间早上7点整，早上好！小枫4号随时为您效劳");
        BotOperator.sendGroupMessage(groupID, worldNews.SendNews(groupID));
        try {
            BotOperator.sendGroupMessage(groupID, String.format("这是今天的天气早报：\n%s", new Weather().WeatherMessage(config.getString("daily-greetings.morning.city"))));
        } catch (Exception e) {
            e.printStackTrace();
        }
        BotOperator.sendGroupMessage(groupID, Hitokoto.HitokotoMessage());
        BotOperator.sendGroupMessage(groupID, "早安，" + messages.getString("server-name"));
    }
}
