package me.maplef.mapbotv4.plugins;

import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.MapbotPlugin;
import me.maplef.mapbotv4.exceptions.NoPermissionException;
import me.maplef.mapbotv4.utils.BotOperator;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.bukkit.configuration.file.FileConfiguration;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ReloadConfigs implements MapbotPlugin {
    static FileConfiguration config = Main.getInstance().getConfig();
    final long opGroup = config.getLong("op-group");

    @Override
    public MessageChain onEnable(Long groupID, Long senderID, String[] args) throws Exception {
        if(!Objects.requireNonNull(BotOperator.getBot().getGroup(opGroup)).contains(senderID))
            throw new NoPermissionException();

        Main.getInstance().reloadConfig();
        Main.getInstance().registerConfig();

        return new MessageChainBuilder().append("配置文件重载完毕").build();
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException{
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("reload", ReloadConfigs.class.getMethod("onEnable", Long.class, Long.class, String[].class));
        commands.put("重载", ReloadConfigs.class.getMethod("onEnable", Long.class, Long.class, String[].class));

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
