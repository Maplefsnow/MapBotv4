package me.maplef.plugins;

import me.maplef.Main;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.bukkit.configuration.file.FileConfiguration;

public class WelcomeNew{
    static final FileConfiguration welcomeNew = Main.getInstance().getMessageConfig();

    public static MessageChain WelcomeMessage() {
        StringBuilder msg = new StringBuilder();

        int size = welcomeNew.getStringList("welcome-new-message").size();
        for(int i = 0; i < size; i++)
            msg.append(welcomeNew.getStringList("welcome-new-message").get(i)).append("\n");

        return new MessageChainBuilder().append(msg.toString().trim()).build();
    }
}
