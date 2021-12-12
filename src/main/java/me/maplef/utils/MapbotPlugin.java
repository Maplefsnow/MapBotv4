package me.maplef.utils;

import me.maplef.plugins.Hitokoto;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public interface MapbotPlugin {
    static MessageChain onEnable(Long groupID, Long senderID, String[] args){
        return new MessageChainBuilder().build();
    }

    static Map<String, Object> register() throws Exception{
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("commandStr_1", Hitokoto.class.getMethod("onEnable", Long.class, Long.class, String[].class));
        commands.put("commandStr_2", Hitokoto.class.getMethod("onEnable", Long.class, Long.class, String[].class));

        usages.put("commandStr_1", "how to use this command...");
        usages.put("commandStr_2", "how to use this command...");

        info.put("name", "pluginName");
        info.put("commands", commands);
        info.put("usages", usages);
        info.put("author", "Unknown");
        info.put("description", "Descriptions...");
        info.put("version", "1.0-beta");
        return info;
    }
}
