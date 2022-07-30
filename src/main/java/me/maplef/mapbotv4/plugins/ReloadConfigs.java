package me.maplef.mapbotv4.plugins;

import me.maplef.mapbotv4.MapbotPlugin;
import me.maplef.mapbotv4.exceptions.NoPermissionException;
import me.maplef.mapbotv4.managers.ConfigManager;
import me.maplef.mapbotv4.utils.BotOperator;
import net.mamoe.mirai.message.data.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ReloadConfigs implements MapbotPlugin {
    @Override
    public MessageChain onEnable(@NotNull Long groupID, @NotNull Long senderID, Message[] args, @Nullable QuoteReply quoteReply) throws Exception {
        ConfigManager configManager = new ConfigManager();

        FileConfiguration config  = configManager.getConfig();
        long opGroup = config.getLong("op-group");

        if(!Objects.requireNonNull(BotOperator.getBot().getGroup(opGroup)).contains(senderID))
            throw new NoPermissionException();

        ConfigManager.reloadConfig("config.yml");
        ConfigManager.reloadConfig("messages.yml");
        ConfigManager.reloadConfig("auto_reply.yml");

        return MessageUtils.newChain(new At(senderID)).plus(" 配置文件重载完毕");
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException{
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("reload", ReloadConfigs.class.getMethod("onEnable", Long.class, Long.class, Message[].class, QuoteReply.class));
        commands.put("重载", ReloadConfigs.class.getMethod("onEnable", Long.class, Long.class, Message[].class, QuoteReply.class));

        usages.put("reload", "#reload - 重载配置文件");
        usages.put("重载", "#重载 - 重载配置文件");

        info.put("name", "ReloadConfigs");
        info.put("commands", commands);
        info.put("usages", usages);
        info.put("author", "Maplef");
        info.put("description", "获取一言");
        info.put("version", "1.3");

        return info;
    }

}
