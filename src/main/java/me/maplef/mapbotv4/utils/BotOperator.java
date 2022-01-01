package me.maplef.mapbotv4.utils;

import net.mamoe.mirai.Bot;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.utils.BotConfiguration;
import org.bukkit.Bukkit;

import java.util.Objects;

public class BotOperator {
    private static Bot bot;

    public static void login(Long qq, String password){
        bot = BotFactory.INSTANCE.newBot(qq, password, new BotConfiguration(){{
            noNetworkLog();
            noBotLog();
        }});
        bot.login();
    }

    public static void close(){
        bot.close();
    }

    public static void send(Long groupID, MessageChain message){
        try{
            Objects.requireNonNull(bot.getGroup(groupID)).sendMessage(message);
        } catch (NullPointerException e){
            Bukkit.getLogger().info("Mapbot正在登陆中，登陆期间的消息将不会转发");
        }
    }

    public static void send(Long groupID, String message){
        try{
            Objects.requireNonNull(bot.getGroup(groupID)).sendMessage(message);
        } catch (NullPointerException e){
            Bukkit.getLogger().info("Mapbot正在登陆中，登陆期间的消息将不会转发");
        }
    }

    public static void send(Long groupID, Message message){
        try{
            Objects.requireNonNull(bot.getGroup(groupID)).sendMessage(message);
        } catch (NullPointerException e){
            Bukkit.getLogger().info("Mapbot正在登陆中，登陆期间的消息将不会转发");
        }
    }

    public static Bot getBot() {
        return bot;
    }
}

