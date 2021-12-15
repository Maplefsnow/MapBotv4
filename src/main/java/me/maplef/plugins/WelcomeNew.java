package me.maplef.plugins;

import me.maplef.Main;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.bukkit.configuration.file.FileConfiguration;

public class WelcomeNew{
    static final FileConfiguration messages = Main.getInstance().getMessageConfig();

    public static MessageChain WelcomeMessage() {
        StringBuilder msg = new StringBuilder();

        for(String singleMsg : messages.getStringList("welcome-new-message.group"))
            msg.append(singleMsg).append("\n");

        return new MessageChainBuilder().append(msg.toString().trim()).build();
    }
}
