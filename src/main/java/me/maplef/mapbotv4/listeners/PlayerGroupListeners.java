package me.maplef.mapbotv4.listeners;

import kotlin.coroutines.CoroutineContext;
import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.exceptions.CommandNotFoundException;
import me.maplef.mapbotv4.exceptions.MessageContainsBlockedWordsException;
import me.maplef.mapbotv4.exceptions.MessageLengthOutOfBoundException;
import me.maplef.mapbotv4.exceptions.PlayerNotFoundException;
import me.maplef.mapbotv4.managers.ConfigManager;
import me.maplef.mapbotv4.managers.PluginManager;
import me.maplef.mapbotv4.plugins.WelcomeNew;
import me.maplef.mapbotv4.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.PermissionDeniedException;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MemberJoinEvent;
import net.mamoe.mirai.event.events.MemberJoinRequestEvent;
import net.mamoe.mirai.event.events.MemberLeaveEvent;
import net.mamoe.mirai.message.data.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

public class PlayerGroupListeners extends SimpleListenerHost {
    ConfigManager configManager = new ConfigManager();

    private final Bot bot = BotOperator.getBot();

    static MessageChain repeatedMessage = null;
    static int repeatCount = 0;

    @Override
    public void handleException(@NotNull CoroutineContext context, @NotNull Throwable exception){
        Bukkit.getServer().getLogger().severe(exception.getMessage());
        exception.printStackTrace();
    }

    @EventHandler
    public void onCommandReceive(GroupMessageEvent e){
        FileConfiguration config = configManager.getConfig();

        String commandPattern = "^" + config.getString("command-prefix") + "[\\u4E00-\\u9FA5A-Za-z0-9_]+(\\s([\\u4E00-\\u9FA5A-Za-z0-9_\\[\\]\\s]|[^\\x00-\\xff])+)?$";

        MessageContent messageContent = e.getMessage().get(PlainText.Key);
        if(messageContent == null) return;

        String textString = messageContent.contentToString().trim();
        if(!Pattern.matches(commandPattern, textString)) return;

        String command = textString.split(" ", 2)[0].substring(1);

        List<Message> argsList = new ArrayList<>();
        for(Message message : e.getMessage()){
            if(message instanceof PlainText){
                for(String arg : message.contentToString().split(" "))
                    argsList.add(new PlainText(arg));
            } else {
                argsList.add(message);
            }
        }
        argsList.remove(0); argsList.remove(0);

        QuoteReply quoteReply = e.getMessage().get(QuoteReply.Key);

        Message[] args = argsList.toArray(new Message[0]);
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            MessageChainBuilder message = new MessageChainBuilder();
            try {
                message.append(PluginManager.commandHandler(command, e.getGroup().getId(), e.getSender().getId(), args, quoteReply));
            } catch (CommandNotFoundException ex) {
                message.append(ex.getMessage());
            } catch (Exception ex){
                ex.printStackTrace();
            }
            BotOperator.sendGroupMessage(e.getGroup().getId(), message.build());
        });
    }

    @EventHandler
    public void onMessageForward(GroupMessageEvent e){
        FileConfiguration config = configManager.getConfig();

        MessageContent messageContent = e.getMessage().get(PlainText.Key);
        if(messageContent == null) return;
        String textString = messageContent.contentToString().trim();
        String commandPattern = "^" + config.getString("command-prefix") + "[\\u4E00-\\u9FA5A-Za-z0-9_]+(\\s([\\u4E00-\\u9FA5A-Za-z0-9_\\[\\]\\s]|[^\\x00-\\xff])+)?$";
        if(Pattern.matches(commandPattern, textString)) return;

        if(!config.getBoolean("message-forward.group-to-server")) return;
        if(e.getGroup().getId() != config.getLong("player-group")) return;

        ClickEvent clickMsgHead = null; Component msgHeadComponent;
        try{
            clickMsgHead = ClickEvent.suggestCommand("@" + DatabaseOperator.queryPlayer(e.getSender().getId()).get("NAME") + " ");
        } catch (Exception ignored){}
        String msgHead = config.getString("message-head-format", "&f<&b{SENDER}&f> ").replace("{SENDER}", e.getSender().getNameCard());
        msgHeadComponent = Component.text(CU.t(msgHead)).clickEvent(clickMsgHead);

        try {
            Component msgContextComponent = QQMessageHandler.handle(e.getSender().getId(), e.getMessage());
            for(Player player : Bukkit.getServer().getOnlinePlayers()) {
                int sendFlag = 1;
                try {
                    sendFlag = (Integer) DatabaseOperator.queryPlayer(player.getName()).get("MSGREC");
                } catch (SQLException ex) {
                    Bukkit.getLogger().warning(ex.getClass() + ": " + ex.getMessage());
                } catch (PlayerNotFoundException ignored) {}
                if (sendFlag == 0) continue;

                player.sendMessage(msgHeadComponent.append(msgContextComponent));
            }
        } catch (MessageLengthOutOfBoundException ex) {
            BotOperator.sendGroupMessage(e.getGroup().getId(), "?????????????????????????????????????????????");
        } catch (MessageContainsBlockedWordsException ignored){}
    }

    @EventHandler
    public void onRepeat(GroupMessageEvent e){
        FileConfiguration config = configManager.getConfig();

        if(!config.getBoolean("bot-repeater.enabled")) return;
        if(e.getGroup().getId() != config.getLong("player-group")) return;
        MessageContent messageContent = e.getMessage().get(PlainText.Key);
        if(messageContent == null) return;
        String textString = messageContent.contentToString().trim();
        String commandPattern = "^" + config.getString("command-prefix") + "[\\u4E00-\\u9FA5A-Za-z0-9_]+(\\s([\\u4E00-\\u9FA5A-Za-z0-9_\\[\\]\\s]|[^\\x00-\\xff])+)?$";
        if(Pattern.matches(commandPattern, textString)) return;

        if(repeatedMessage == null){
            repeatedMessage = e.getMessage();
            repeatCount = 1;
        } else if(Pattern.matches(commandPattern, e.getMessage().contentToString())){
            repeatedMessage = null;
            repeatCount = 0;
        } else {
            if(e.getMessage().contentToString().equals(repeatedMessage.contentToString()) && !repeatedMessage.contentToString().equals("[??????]")) repeatCount += 1;
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
    public void onRandomSpeak(GroupMessageEvent e){
        FileConfiguration config = configManager.getConfig();
        FileConfiguration messages = configManager.getMessageConfig();

        if(e.getGroup().getId() != config.getLong("player-group")) return;
        if(repeatCount > 0) return;

        Random random = new Random();
        int score = random.nextInt(100);
        if(score < config.getInt("bot-speak-possibility")){
            int col = random.nextInt(messages.getStringList("bot-greetings").size());
            String msg = messages.getStringList("bot-greetings").get(col);
            BotOperator.sendGroupMessage(config.getLong("player-group"), msg);
        }
    }

    @EventHandler
    public void onBlockedWordReceive(GroupMessageEvent e){
        FileConfiguration config = configManager.getConfig();

        if(e.getGroup().getId() != config.getLong("player-group")) return;
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
    public void onAutoReply(GroupMessageEvent e){
        FileConfiguration config = configManager.getConfig();
        FileConfiguration autoReply = configManager.getAutoReplyConfig();

        if(e.getGroup().getId() != config.getLong("player-group")) return;
        if(!autoReply.getBoolean("enable-in-group")) return;

        String message = e.getMessage().contentToString();
        Set<String> rules = autoReply.getKeys(false);

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            for(String ruleKey : rules){
                String mode = autoReply.getString("ip-ask.mode", "similarity");

                String reply;
                switch (mode) {
                    case "similarity" -> {
                        List<String> triggers = autoReply.getStringList(ruleKey + ".trigger");
                        for(String trigger : triggers){
                            double similarity = TextComparator.getLSTSimilarity(message, trigger) * 60
                                    + TextComparator.getCosSimilarity(message, trigger) * 40;
                            if(similarity >= autoReply.getInt(ruleKey + ".similarity", 100)){
                                reply = autoReply.getString(ruleKey + ".reply", "null");
                                BotOperator.sendGroupMessage(e.getGroup().getId(), reply);
                            }
                        }
                    }
                    case "contains" -> {
                        List<String> triggers = autoReply.getStringList(ruleKey + ".trigger");
                        for(String trigger : triggers){
                            if(message.contains(trigger)){
                                reply = autoReply.getString(ruleKey + ".reply", "null");
                                BotOperator.sendGroupMessage(e.getGroup().getId(), reply);
                                break;
                            }
                        }
                    }
                }
            }
        });
    }

    @EventHandler
    public void onNewCome(MemberJoinEvent e){
        FileConfiguration config = configManager.getConfig();
        FileConfiguration messages = configManager.getMessageConfig();

        if(e.getGroupId() != config.getLong("player-group")) return;

        BotOperator.sendGroupMessage(e.getGroupId(), new WelcomeNew().WelcomeMessage());
        Bukkit.getServer().broadcast(Component.text(CU.t(messages.getString("message-prefix") + messages.getString("welcome-new-message.player-group.server"))));
        if(Objects.requireNonNull(bot.getGroup(config.getLong("check-in-group"))).contains(e.getMember().getId())){
            BotOperator.sendGroupMessage(config.getLong("check-in-group"), Objects.requireNonNull(messages.getString("congratulation-message")).replace("{PLAYER}", e.getMember().getNick()));
        }
    }

    @EventHandler
    public void onExitPlayerGroup(MemberLeaveEvent e){
        FileConfiguration config = configManager.getConfig();
        FileConfiguration messages = configManager.getMessageConfig();

        if(e.getGroupId() != config.getLong("player-group")) return;

        Long QQ = e.getMember().getId();
        String ID = null;
        String playerGroupMsg, opGroupMsg;

        try{
            ID = DatabaseOperator.queryPlayer(QQ).get("NAME").toString();
        } catch (SQLException ex){
            ex.printStackTrace();
        } catch (PlayerNotFoundException ignored){}

        if(ID == null){
            opGroupMsg = Objects.requireNonNull(messages.getString("exit-player-group-message.op-group"))
                    .replace("{PLAYER}", e.getMember().getNameCard())
                    .replace("{SERVERNAME}", Objects.requireNonNull(messages.getString("server-name")))
                    + "?????????????????????????????????ID??????";

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

            try {
                String bindDelCommand = String.format("DELETE FROM PLAYER WHERE NAME = '%s';", ID);
                PreparedStatement ps = new DatabaseOperator().getConnect().prepareStatement(bindDelCommand);
                ps.execute(); ps.close();
            } catch (SQLException ex){
                Bukkit.getServer().getLogger().warning(ex.getMessage());
            }

            opGroupMsg = Objects.requireNonNull(messages.getString("exit-player-group-message.op-group"))
                    .replace("{PLAYER}", ID)
                    .replace("{SERVERNAME}", Objects.requireNonNull(messages.getString("server-name")))
                    + "???ID????????????????????????????????????";

            playerGroupMsg = Objects.requireNonNull(messages.getString("exit-player-group-message.player-group"))
                    .replace("{PLAYER}", ID)
                    .replace("{SERVERNAME}", Objects.requireNonNull(messages.getString("server-name")));
        }

        BotOperator.sendGroupMessage(config.getLong("player-group"), playerGroupMsg);
        BotOperator.sendGroupMessage(config.getLong("op-group"), opGroupMsg);
    }

    @EventHandler
    public void onJoinGroupRequest(MemberJoinRequestEvent e){
        FileConfiguration config = configManager.getConfig();

        if(!config.getBoolean("player-group-auto-manage.enable")) return;
        if(e.getGroupId() != config.getLong("player-group")) return;

        Bukkit.getServer().getLogger().info(e.getMessage());

        if(!e.getMessage().contains("IV")){
            e.reject(false, Objects.requireNonNull(config.getString("player-group-auto-manage.reject-message")));
            BotOperator.sendGroupMessage(config.getLong("op-group"), "????????? " + e.component7() + " ??????");
            return;
        }

        String code = e.getMessage().split("\n")[1].substring(3);
        Bukkit.getServer().getLogger().info(code);
        String url = "https://copa.mrzzj.top/invitecode/check.php?InviteCode=" + code;
        String resString = HttpUtils.doGet(url);

        if (resString.equals("OK")) {
            e.accept();
            BotOperator.sendGroupMessage(config.getLong("op-group"), "????????? " + e.component7() + " ??????");
        }
        else {
            e.reject(false, Objects.requireNonNull(config.getString("player-group-auto-manage.reject-message")));
            BotOperator.sendGroupMessage(config.getLong("op-group"), "????????? " + e.component7() + " ??????");
        }
    }

    @EventHandler
    public void onTest(GroupMessageEvent e){
        FileConfiguration config = configManager.getConfig();

        if(e.getGroup().getId() != config.getLong("op-group")) return;

        if(e.getMessage().contentToString().contains("test")){
            Bukkit.getServer().getLogger().info("[Mapbot] tested!");
        }
    }
}
