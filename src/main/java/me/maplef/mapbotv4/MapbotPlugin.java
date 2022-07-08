package me.maplef.mapbotv4;

import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;

import java.util.Map;

public interface MapbotPlugin {
    MessageChain onEnable(Long groupID, Long senderID, Message[] args) throws Exception;

    Map<String, Object> register() throws NoSuchMethodException;
}
