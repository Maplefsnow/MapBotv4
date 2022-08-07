package me.maplef.mapbotv4.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import me.maplef.mapbotv4.exceptions.MessageContainsBlockedWordsException;
import me.maplef.mapbotv4.exceptions.MessageLengthOutOfBoundException;
import me.maplef.mapbotv4.exceptions.PlayerNotFoundException;
import me.maplef.mapbotv4.managers.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.message.data.*;
import org.bukkit.configuration.file.FileConfiguration;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

public class QQMessageHandler {
    static ConfigManager configManager = new ConfigManager();
    static final FileConfiguration config = configManager.getConfig();

    private static final Bot bot = BotOperator.getBot();
    private static final Long opGroup = config.getLong("op-group");

    private static Component quoteHandler(QuoteReply quoteReply, Component messageComponent){
        MessageSource quoteSource = quoteReply.getSource();
        MessageChain quoteOriginalMessage = quoteSource.getOriginalMessage();
        Long quoteFromID = null; String quotePlayerNAME = null;
        try {
            quoteFromID = quoteSource.getFromId();
            quotePlayerNAME = (String) DatabaseOperator.queryPlayer(quoteSource.getFromId()).get("NAME");
        } catch (SQLException | PlayerNotFoundException ignored) {}

        HoverEvent<Component> quoteHover; Component quoteComponent = null;

        if(quoteFromID.equals(bot.getId())){
            String[] msgSplit = quoteOriginalMessage.contentToString().split(": ", 2);
            try{
                DatabaseOperator.queryPlayer(msgSplit[0]);
                quoteHover = HoverEvent.showText(Component.text(String.format("%s:\n%s", msgSplit[0], msgSplit[1])));
                quoteComponent = Component.text(String.format("[回复%s的消息]", msgSplit[0]))
                        .color(NamedTextColor.GRAY)
                        .decorate(TextDecoration.ITALIC)
                        .hoverEvent(quoteHover);
            } catch (PlayerNotFoundException ex){
                quoteHover = HoverEvent.showText(Component.text(String.format("小叶子:\n%s", quoteOriginalMessage.contentToString())));
                quoteComponent = Component.text("[回复小叶子的消息]")
                        .color(NamedTextColor.GRAY)
                        .decorate(TextDecoration.ITALIC)
                        .hoverEvent(quoteHover);
            } catch (SQLException ex){
                ex.printStackTrace();
            }
        } else {
            quoteHover = HoverEvent.showText(Component.text(String.format("%s:\n%s", quotePlayerNAME, quoteOriginalMessage.contentToString())));
            quoteComponent = Component.text(String.format("[回复%s的消息]", quotePlayerNAME))
                        .color(NamedTextColor.GRAY)
                        .decorate(TextDecoration.ITALIC)
                        .hoverEvent(quoteHover);
        }

        assert quoteComponent != null;
        return quoteComponent.append(Component.text(" ").append(messageComponent)
                .style(Style.empty())
                .hoverEvent(null));
    }

    private static Component lightAppHandler(MessageChain message){
        JSONObject appInfo = JSON.parseObject(message.contentToString());

        String app = appInfo.getString("app"); JSONObject meta = appInfo.getJSONObject("meta");
        switch (app) {
            case "com.tencent.mannounce" -> {
                JSONObject mannounce = meta.getJSONObject("mannounce");
                String text_base64 = mannounce.getString("text");
                byte[] decodedText = Base64.getDecoder().decode(text_base64);
                String text = new String(decodedText, StandardCharsets.UTF_8);

                HoverEvent<Component> textHover = HoverEvent.showText(Component.text(text));

                return Component.text("[群公告] ")
                        .color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
                        .append(Component.text("发送了一条群公告，请及时查看！ ")
                                .color(NamedTextColor.GREEN))
                        .append(Component.text("（预览一下）")
                                .color(NamedTextColor.BLUE).decorate(TextDecoration.UNDERLINED)
                                .hoverEvent(textHover));
            }
            case "com.tencent.miniapp_01" -> {
                JSONObject detail_1 = meta.getJSONObject("detail_1");
                String prompt = appInfo.getString("prompt");
                String desc = detail_1.getString("desc");
                String qqdocurl = detail_1.getString("qqdocurl");

                ClickEvent clickEvent = ClickEvent.openUrl(qqdocurl);
                HoverEvent<Component> urlOpenHover = HoverEvent.showText(Component.text(">点我打开<")
                        .color(NamedTextColor.BLUE).decorate(TextDecoration.UNDERLINED));

                return Component.text(String.format("%s - %s", prompt, desc))
                        .decorate(TextDecoration.UNDERLINED)
                        .hoverEvent(urlOpenHover).clickEvent(clickEvent);
            }
            case "com.tencent.structmsg" -> {
                JSONObject news = meta.getJSONObject("news");
                String prompt = appInfo.getString("prompt");
                String jumpUrl = news.getString("jumpUrl");
                String desc = news.getString("desc");

                ClickEvent clickEvent = ClickEvent.openUrl(jumpUrl);
                HoverEvent<Component> hoverEvent = HoverEvent.showText(Component.text(desc + "\n\n")
                        .append(Component.text(">点击打开<")
                                .color(NamedTextColor.BLUE).decorate(TextDecoration.UNDERLINED)));

                return Component.text(prompt)
                        .decorate(TextDecoration.UNDERLINED)
                        .hoverEvent(hoverEvent).clickEvent(clickEvent);
            }
            default -> {
                System.out.println(message.contentToString());
                return Component.text("[小程序] 不支持的小程序类型，请至群内查看");
            }
        }
    }

    private static Component commonHandler(Long senderID, MessageChain messages) throws MessageLengthOutOfBoundException, MessageContainsBlockedWordsException {
        TextComponent messageComponent = Component.text("");

        ArrayList<String> atList = new ArrayList<>();
        for(Message message : messages){
            if(message instanceof At){
                Long atNum = ((At) message).getTarget();
                if(atNum.equals(bot.getId())) continue;

                String atID = "";
                try {
                    atID = (String) DatabaseOperator.queryPlayer(atNum).get("NAME");
                    atList.add(atID);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                } catch (PlayerNotFoundException ignored){}
                messageComponent = messageComponent.append(Component.text("@"+atID)
                        .color(NamedTextColor.WHITE)
                        .decorate(TextDecoration.BOLD));
            } else if(message instanceof AtAll){
                messageComponent = messageComponent.append(Component.text("@全体成员")
                        .color(NamedTextColor.GREEN)
                        .decorate(TextDecoration.BOLD));
                atList.add("ALL");
            } else {
                if(!Objects.requireNonNull(bot.getGroup(opGroup)).contains(senderID))
                    messageComponent = messageComponent.append(Component.text(message.contentToString().replace("§", ""))
                            .color(NamedTextColor.WHITE));
                else
                    messageComponent = messageComponent.append(Component.text(message.contentToString())
                            .color(NamedTextColor.WHITE));
            }
        }

        if(config.getBoolean("message-length-limit.enable")){
            if(messages.contentToString().length() > config.getInt("message-length-limit.maximum-length")){
                if(config.getBoolean("message-length-limit.ignore-ops")){
                    if(!Objects.requireNonNull(bot.getGroup(opGroup)).contains(senderID)){
                       throw new MessageLengthOutOfBoundException();
                    }
                } else {
                    throw new MessageLengthOutOfBoundException();
                }
            }
        }

        if(config.getBoolean("block-words.enabled")){
            List<String> blocklist = config.getStringList("block-words.blocklist");
            for(String blockedWord : blocklist){
                if(messages.contentToString().contains(blockedWord)) throw new MessageContainsBlockedWordsException();
            }
        }

        return messageComponent;
    }

    public static Component handle(Long senderID, MessageChain message) throws MessageLengthOutOfBoundException, MessageContainsBlockedWordsException{
        Component messageComponent = commonHandler(senderID, message);

        MessageContent messageContent = message.get(MessageContent.Key);
        QuoteReply quoteReply = message.get(QuoteReply.Key);

        if(quoteReply != null){
            return quoteHandler(quoteReply, messageComponent);
        } else if(messageContent instanceof LightApp){
            return lightAppHandler(message);
        } else {
            return messageComponent;
        }
    }
}
