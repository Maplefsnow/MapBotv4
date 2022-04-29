package me.maplef.mapbotv4.listeners;

import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.exceptions.CommandNotFoundException;
import me.maplef.mapbotv4.exceptions.PlayerNotFoundException;
import me.maplef.mapbotv4.managers.PluginManager;
import me.maplef.mapbotv4.plugins.WelcomeNew;
import me.maplef.mapbotv4.utils.BotOperator;
import me.maplef.mapbotv4.utils.CU;
import me.maplef.mapbotv4.utils.DatabaseOperator;
import me.maplef.mapbotv4.utils.HttpClient4;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.PermissionDeniedException;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.*;
import net.mamoe.mirai.message.data.*;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Pattern;

public class PlayerGroupListeners extends SimpleListenerHost {
    final FileConfiguration config = Main.getPlugin(Main.class).getConfig();
    final FileConfiguration messages = Main.getInstance().getMessageConfig();

    private final Bot bot = BotOperator.getBot();

    private final Long botAcc = config.getLong("bot-account");
    private final Long playerGroup = config.getLong("player-group");
    private final Long opGroup = config.getLong("op-group");
    private final Long checkInGroup = config.getLong("check-in-group");

    static MessageChain repeatedMessage = null;
    static int repeatCount = 0;

    private final String commandPattern = "^" + config.getString("command-prefix") + "[\\u4E00-\\u9FA5A-Za-z0-9_]+(\\s[\\u4E00-\\u9FA5A-Za-z0-9_\\s]+)?$";

    @EventHandler
    public void onCommandReceive(GroupMessageEvent e){
        if(!Pattern.matches(commandPattern, e.getMessage().contentToString())) return;

        ArrayList<String> msgSplit = new ArrayList<>(List.of(e.getMessage().contentToString().split(" ")));
        String command = msgSplit.get(0).substring(1);
        msgSplit.remove(0);
        int size = msgSplit.size();
        String[] args = msgSplit.toArray(new String[size]);

        MessageChainBuilder message = new MessageChainBuilder();
        try {
            message.append(PluginManager.commandHandler(command, e.getGroup().getId(), e.getSender().getId(), args));
        } catch (CommandNotFoundException ex) {
            message.append(ex.getMessage());
        } catch (Exception ex){
            ex.printStackTrace();
        }
        BotOperator.sendGroupMessage(e.getGroup().getId(), message.build());
    }

    @EventHandler
    public void onMessageForward(GroupMessageEvent e){
        if(e.getGroup().getId() != playerGroup) return;
        if(Pattern.matches(commandPattern, e.getMessage().contentToString())) return;

        QuoteReply quoteMessage = e.getMessage().get(QuoteReply.Key);
        Long quoteFromID = null; String quotePlayerNAME = null; MessageChain quoteOriginalMessage = null;
        if(quoteMessage != null){
            MessageSource quoteSource = quoteMessage.getSource();
            quoteOriginalMessage = quoteSource.getOriginalMessage();
            try {
                quoteFromID = quoteSource.getFromId();
                quotePlayerNAME = (String) DatabaseOperator.query(quoteSource.getFromId()).get("NAME");
            } catch (SQLException | PlayerNotFoundException ex) {
                ex.printStackTrace();
            }
        }

        StringBuilder msgStringBuilder = new StringBuilder();
        ArrayList<String> atList = new ArrayList<>();
        for(Message message : e.getMessage()){
            if(message instanceof At){
                String atNum = String.valueOf(((At) message).getTarget());
                if(atNum.equals(botAcc.toString())) continue;

                String atID = "";
                try {
                    atID = (String) DatabaseOperator.query(Long.parseLong(atNum)).get("NAME");
                    atList.add(atID);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                } catch (PlayerNotFoundException ignored){}
                msgStringBuilder.append(String.format("&f&l@%s&r", atID));
            } else if(message instanceof AtAll){
                msgStringBuilder.append("&a&l@全体成员&r");
                atList.add("ALL");
            } else {
                msgStringBuilder.append(message.contentToString());
            }
        }

        String msgString = msgStringBuilder.toString();
        if(!Objects.requireNonNull(bot.getGroup(opGroup)).contains(e.getSender().getId()))
            msgString = msgString.replace("§", "");

        if(config.getBoolean("block-words.enabled")){
            List<String> blocklist = config.getStringList("block-words.blocklist");
            for(String blockedWord : blocklist){
                if(msgString.contains(blockedWord)) return;
            }
        }

        if(config.getBoolean("message-length-limit.enable")){
            if(msgString.length() > config.getInt("message-length-limit.maximum-length")){
                if(config.getBoolean("message-length-limit.ignore-ops")){
                    if(!Objects.requireNonNull(bot.getGroup(opGroup)).contains(e.getSender().getId())){
                        BotOperator.sendGroupMessage(e.getGroup().getId(), "本条消息过长，将不转发至服务器");
                        return;
                    }
                } else {
                    BotOperator.sendGroupMessage(e.getGroup().getId(), "本条消息过长，将不转发至服务器");
                    return;
                }
            }
        }

        for(Player player : Bukkit.getServer().getOnlinePlayers()){
            int sendFlag = 1;
            try {
                sendFlag = (Integer) DatabaseOperator.query(player.getName()).get("MSGREC");
            } catch (SQLException ex) {
                Bukkit.getLogger().warning(ex.getClass() + ": " + ex.getMessage());
            } catch (PlayerNotFoundException ignored){}
            if(sendFlag == 0) continue;

            if(atList.contains(player.getName()) || atList.contains("ALL"))
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 10f, 1.5f);

            String msgHead = Objects.requireNonNull(config.getString("message-head-format")).replace("{SENDER}", e.getSender().getNameCard());

            if(quoteMessage != null){
                HoverEvent<Component> quoteHover; Component quoteInfo = null;

                if(quoteFromID.equals(botAcc)){
                    String[] msgSplit = quoteOriginalMessage.contentToString().split(": ", 2);
                    try{
                        DatabaseOperator.query(msgSplit[0]);
                        quoteHover = HoverEvent.showText(Component.text(String.format("%s:\n%s", msgSplit[0], msgSplit[1])));
                        quoteInfo = Component.text(CU.t(String.format("&7&o[回复%s的消息]&r", msgSplit[0]))).hoverEvent(quoteHover);
                    } catch (PlayerNotFoundException ex){
                        quoteHover = HoverEvent.showText(Component.text(String.format("小叶子:\n%s", quoteOriginalMessage.contentToString())));
                        quoteInfo = Component.text(CU.t("&7&o[回复小叶子的消息]&r")).hoverEvent(quoteHover);
                    } catch (SQLException ex){
                        ex.printStackTrace();
                    }
                } else {
                    quoteHover = HoverEvent.showText(Component.text(String.format("%s:\n%s", quotePlayerNAME, quoteOriginalMessage.contentToString())));
                    quoteInfo = Component.text(CU.t(String.format("&7&o[回复%s的消息]&r", quotePlayerNAME))).hoverEvent(quoteHover);
                }

                assert quoteInfo != null;
                player.sendMessage(Component.text(CU.t(msgHead)).append(quoteInfo).append(Component.text(" " + CU.t(msgString))));
            } else {
                player.sendMessage(Component.text(CU.t(msgHead + msgString)));
            }
        }
    }

    @EventHandler
    public void onRepeat(GroupMessageEvent e){
        if(!config.getBoolean("bot-repeater.enabled")) return;
        if(e.getGroup().getId() != playerGroup) return;

        if(repeatedMessage == null){
            repeatedMessage = e.getMessage();
            repeatCount = 1;
        } else {
            if(e.getMessage().contentToString().equals(repeatedMessage.contentToString()) && !repeatedMessage.contentToString().equals("[图片]")) repeatCount += 1;
            else{
                repeatedMessage = e.getMessage();
                repeatCount = 1;
            }
        }

        if(repeatCount >= config.getInt("bot-repeater.frequency")){
            BotOperator.sendGroupMessage(e.getGroup().getId(), e.getMessage());
            repeatedMessage = null;
            repeatCount = 0;
        }
    }

    @EventHandler
    public void onRandom(GroupMessageEvent e){
        if(e.getGroup().getId() != playerGroup) return;

        Random random = new Random();
        int score = random.nextInt(100);
        if(score < config.getInt("bot-speak-possibility")){
            int col = random.nextInt(messages.getStringList("bot-greetings").size());
            String msg = messages.getStringList("bot-greetings").get(col);
            BotOperator.sendGroupMessage(playerGroup, msg);
        }
    }

    @EventHandler
    public void onBlockedWordReceive(GroupMessageEvent e){
        if(e.getGroup().getId() != playerGroup) return;
        if(!config.getBoolean("block-words.enabled")) return;

        String message = e.getMessage().contentToString();
        List<String> blocklist = config.getStringList("block-words.blocklist");
        for(String blockedWord : blocklist){
            if(message.contains(blockedWord)){
                try {
                    MessageSource.recall(e.getMessage());
                } catch (PermissionDeniedException ignored){}
                if(!Objects.requireNonNull(config.getString("block-words.warning-message")).isEmpty()){
                    BotOperator.sendGroupMessage(e.getGroup().getId(),
                            MessageUtils.newChain(new At(e.getSender().getId()), new PlainText(" " + config.getString("block-words.warning-message"))));
                }
                break;
            }
        }
    }

    @EventHandler
    public void onNewCome(MemberJoinEvent e){
        if(e.getGroupId() != playerGroup) return;

        BotOperator.sendGroupMessage(e.getGroupId(), WelcomeNew.WelcomeMessage());
        Bukkit.getServer().broadcast(Component.text(CU.t(messages.getString("message-prefix") + messages.getString("welcome-new-message.player-group.server"))));
        if(Objects.requireNonNull(bot.getGroup(checkInGroup)).contains(e.getMember().getId())){
            BotOperator.sendGroupMessage(checkInGroup, Objects.requireNonNull(messages.getString("congratulation-message")).replace("{PLAYER}", e.getMember().getNick()));
        }
    }

    @EventHandler
    public void onExitPlayerGroup(MemberLeaveEvent e){
        if(e.getGroupId() != playerGroup) return;

        Long QQ = e.getMember().getId();
        String ID = null;
        String playerGroupMsg, opGroupMsg;

        try{
            ID = DatabaseOperator.query(QQ).get("NAME").toString();
        } catch (SQLException ex){
            ex.printStackTrace();
        } catch (PlayerNotFoundException ignored){}

        if(ID == null){
            opGroupMsg = Objects.requireNonNull(messages.getString("exit-player-group-message.op-group"))
                    .replace("{PLAYER}", e.getMember().getNameCard())
                    .replace("{SERVERNAME}", Objects.requireNonNull(messages.getString("server-name")))
                    + "，未检测到该玩家的绑定ID行为";

            playerGroupMsg = Objects.requireNonNull(messages.getString("exit-player-group-message.player-group"))
                    .replace("{PLAYER}", e.getMember().getNameCard())
                    .replace("{SERVERNAME}", Objects.requireNonNull(messages.getString("server-name")));
        } else {
            String whitelistDelCommand = String.format("whitelist remove %s", ID);
            new BukkitRunnable(){
                @Override
                public void run(){
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), whitelistDelCommand);
                }
            }.runTask(Main.getPlugin(Main.class));

            opGroupMsg = Objects.requireNonNull(messages.getString("exit-player-group-message.op-group"))
                    .replace("{PLAYER}", ID)
                    .replace("{SERVERNAME}", Objects.requireNonNull(messages.getString("server-name")))
                    + "，ID绑定已解除，白名单已移除";

            playerGroupMsg = Objects.requireNonNull(messages.getString("exit-player-group-message.player-group"))
                    .replace("{PLAYER}", ID)
                    .replace("{SERVERNAME}", Objects.requireNonNull(messages.getString("server-name")));
        }

        BotOperator.sendGroupMessage(playerGroup, playerGroupMsg);
        BotOperator.sendGroupMessage(opGroup, opGroupMsg);
    }

    @EventHandler
    public void onJoinGroupRequest(MemberJoinRequestEvent e){
        if(!config.getBoolean("player-group-auto-manage.enable")) return;
        if(e.getGroupId() != playerGroup) return;

        Bukkit.getServer().getLogger().info(e.getMessage());

        if(!e.getMessage().contains("IV")){
            e.reject(false, Objects.requireNonNull(config.getString("player-group-auto-manage.reject-message")));
            BotOperator.sendGroupMessage(opGroup, "已拒绝 " + e.component7() + " 入群");
            return;
        }

        String code = e.getMessage().split("\n")[1].substring(3);
        Bukkit.getServer().getLogger().info(code);
        String url = "https://copa.mrzzj.top/invitecode/check.php?InviteCode=" + code;
        String resString = HttpClient4.doGet(url);

        if (resString.equals("OK")) {
            e.accept();
            BotOperator.sendGroupMessage(opGroup, "已同意 " + e.component7() + " 入群");
        }
        else {
            e.reject(false, Objects.requireNonNull(config.getString("player-group-auto-manage.reject-message")));
            BotOperator.sendGroupMessage(opGroup, "已拒绝 " + e.component7() + " 入群");
        }
    }

    @EventHandler
    public void onTest(GroupMessageEvent e){
        if(e.getGroup().getId() != opGroup) return;

        if(e.getMessage().contentToString().contains("test")){
            Bukkit.getServer().getLogger().info("tested!");
        }
    }
}
