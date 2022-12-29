package me.maplef.mapbotv4.plugins;

import me.maplef.mapbotv4.MapbotPlugin;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.QuoteReply;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class BiliHelper implements MapbotPlugin {
    @Override
    public MessageChain onEnable(@NotNull Long groupID, @NotNull Long senderID, Message[] args, @Nullable QuoteReply quoteReply) throws Exception {
        return null;
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException {
        return null;
    }
}
