package me.maplef.mapbotv4.plugins;

import me.maplef.mapbotv4.managers.ConfigManager;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.bukkit.configuration.file.FileConfiguration;

public class WelcomeNew{
    ConfigManager configManager = new ConfigManager();
    FileConfiguration messages = configManager.getMessageConfig();

    public MessageChain WelcomeMessage() {
        StringBuilder msg = new StringBuilder();

        for(String singleMsg : messages.getStringList("welcome-new-message.player-group.group"))
            msg.append(singleMsg).append("\n");

        return new MessageChainBuilder().append(msg.toString().trim()).build();
    }
}
