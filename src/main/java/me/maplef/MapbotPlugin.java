package me.maplef;

import net.mamoe.mirai.message.data.MessageChain;

import java.util.Map;

public interface MapbotPlugin {
    MessageChain onEnable(Long groupID, Long senderID, String[] args) throws Exception;

    Map<String, Object> register() throws NoSuchMethodException;
}
