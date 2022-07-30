package me.maplef.mapbotv4.loops;

import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.managers.ConfigManager;
import me.maplef.mapbotv4.utils.BaiduAuthService;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import java.io.File;
import java.io.IOException;

public class BaiduAccessTokenUpdate implements Job {
    ConfigManager configManager = new ConfigManager();

    public void updateAuth(){
        FileConfiguration config = configManager.getConfig();
        String API_KEY = config.getString("cat-images.upload-image.cat-detect.api-key");
        String SECRET_KEY = config.getString("cat-images.upload-image.cat-detect.secret-key");

        String access_token = BaiduAuthService.getAuth(API_KEY, SECRET_KEY);
        config.set("cat-images.upload-image.cat-detect.access-token", access_token);
        try {
            config.save(new File(Main.getInstance().getDataFolder(), "config.yml"));
        } catch (IOException e) {
            Bukkit.getServer().getLogger().warning(String.format("[%s] 更新 Baidu_token 出错", Main.getInstance().getDescription().getName()));
        }
    }

    @Override
    public void execute(JobExecutionContext context){
        updateAuth();
    }
}
