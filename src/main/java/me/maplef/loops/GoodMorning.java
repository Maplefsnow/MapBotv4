package me.maplef.loops;

import me.maplef.plugins.Hitokoto;
import me.maplef.plugins.Weather;
import me.maplef.plugins.WorldNews;
import me.maplef.utils.BotOperator;
import org.bukkit.configuration.file.FileConfiguration;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

public class GoodMorning implements Job{
    final FileConfiguration config = me.maplef.Main.getPlugin(me.maplef.Main.class).getConfig();

    @Override
    public void execute(JobExecutionContext context){
        WorldNews worldNews = new WorldNews();

        long groupID = config.getLong("player-group");

        BotOperator.send(groupID, "现在是北京时间早上7点整，早上好！小枫4号随时为您效劳");
        BotOperator.send(groupID, worldNews.SendNews(groupID));
        try {
            BotOperator.send(groupID, String.format("这是今天的天气早报：\n%s", Weather.WeatherMessage("苏州")));
        } catch (Exception e) {
            e.printStackTrace();
        }
        BotOperator.send(groupID, Hitokoto.HitokotoMessage());
        BotOperator.send(groupID, "早安，猫猫大陆");
    }
}
