package me.maplef.mapbotv4.utils;

import me.maplef.mapbotv4.Main;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.network.WrongPasswordException;
import net.mamoe.mirai.utils.BotConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.Objects;

public class BotOperator {
    private static Bot bot;
    static final FileConfiguration config = Main.getInstance().getConfig();

    public static void login(Long qq, String password) throws WrongPasswordException{
        File logPath = new File(Main.getInstance().getDataFolder(), "logs");

        bot = BotFactory.INSTANCE.newBot(qq, password, new BotConfiguration(){{
            redirectNetworkLogToDirectory(logPath);
            redirectBotLogToDirectory(logPath);
            setProtocol(MiraiProtocol.valueOf(config.getString("bot-login-device", "ANDROID_PHONE")));
            setCacheDir(new File("cache"));
        }});
        bot.login();
    }

    public static void sendGroupMessage(Long groupID, MessageChain message){
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try{
                Objects.requireNonNull(bot.getGroup(groupID)).sendMessage(message);
            } catch (NullPointerException e){
                Bukkit.getLogger().info("Mapbot正在登陆中，登陆期间的消息将不会转发");
            }
        });
    }

    public static void sendGroupMessage(Long groupID, String message){
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try{
                Objects.requireNonNull(bot.getGroup(groupID)).sendMessage(message);
            } catch (NullPointerException e){
                Bukkit.getLogger().info("Mapbot正在登陆中，登陆期间的消息将不会转发");
            }
        });
    }

    public static Bot getBot() {
        return bot;
    }
}

