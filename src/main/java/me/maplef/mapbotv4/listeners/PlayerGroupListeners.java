package me.maplef.mapbotv4.listeners;

import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.exceptions.CommandNotFoundException;
import me.maplef.mapbotv4.exceptions.MessageContainsBlockedWordsException;
import me.maplef.mapbotv4.exceptions.MessageLengthOutOfBoundException;
import me.maplef.mapbotv4.exceptions.PlayerNotFoundException;
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

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

public class PlayerGroupListeners extends SimpleListenerHost {
    final FileConfiguration config = Main.getInstance().getConfig();
    final FileConfiguration messages = Main.getInstance().getMessageConfig();
    final FileConfiguration autoReply = Main.getInstance().getAutoReplyConfig();

    private final Bot bot = BotOperator.getBot();

    private final Long playerGroup = config.getLong("player-group");
    private final Long opGroup = config.getLong("op-group");
    private final Long checkInGroup = config.getLong("check-in-group");

    static MessageChain repeatedMessage = null;
    static int repeatCount = 0;

    private final String commandPattern = "^" + config.getString("command-prefix") + "[\\u4E00-\\u9FA5A-Za-z0-9_]+(\\s([\\u4E00-\\u9FA5A-Za-z0-9_\\s]|[^\\x00-\\xff])+)?$";

    @EventHandler
    public void onCommandReceive(GroupMessageEvent e){
        if(!Pattern.matches(commandPattern, e.getMessage().contentToString())) return;

        ArrayList<String> msgSplit = new ArrayList<>(List.of(e.getMessage().contentToString().split(" ")));
        String command = msgSplit.get(0).substring(1);
        msgSplit.remove(0);
        int size = msgSplit.size();
        String[] args = msgSplit.toArray(new String[size]);

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            MessageChainBuilder message = new MessageChainBuilder();
            try {
                message.append(PluginManager.commandHandler(command, e.getGroup().getId(), e.getSender().getId(), args));
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
        if(!config.getBoolean("message-forward.group-to-server")) return;
        if(e.getGroup().getId() != playerGroup) return;
        if(Pattern.matches(commandPattern, e.getMessage().contentToString())) return;

        ClickEvent clickMsgHead = null; Component msgHeadComponent;
        try{
            clickMsgHead = ClickEvent.suggestCommand("@" + DatabaseOperator.query(e.getSender().getId()).get("NAME") + " ");
        } catch (Exception ignored){}
        String msgHead = Objects.requireNonNull(config.getString("message-head-format")).replace("{SENDER}", e.getSender().getNameCard());
        msgHeadComponent = Component.text(CU.t(msgHead)).clickEvent(clickMsgHead);

        try {
            Component msgContextComponent = QQMessageHandler.handle(e.getSender().getId(), e.getMessage());
            for(Player player : Bukkit.getServer().getOnlinePlayers()) {
                int sendFlag = 1;
                try {
                    sendFlag = (Integer) DatabaseOperator.query(player.getName()).get("MSGREC");
                } catch (SQLException ex) {
                    Bukkit.getLogger().warning(ex.getClass() + ": " + ex.getMessage());
                } catch (PlayerNotFoundException ignored) {}
                if (sendFlag == 0) continue;

                player.sendMessage(msgHeadComponent.append(msgContextComponent));
            }
        } catch (MessageLengthOutOfBoundException ex) {
            BotOperator.sendGroupMessage(e.getGroup().getId(), "本条消息过长，将不转发至服务器");
        } catch (MessageContainsBlockedWordsException ignored){}
    }

    @EventHandler
    public void onRepeat(GroupMessageEvent e){
        if(!config.getBoolean("bot-repeater.enabled")) return;
        if(e.getGroup().getId() != playerGroup) return;

        if(repeatedMessage == null){
            repeatedMessage = e.getMessage();
            repeatCount = 1;
        } else if(Pattern.matches(commandPattern, e.getMessage().contentToString())){
            repeatedMessage = null;
            repeatCount = 0;
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
    public void onAutoReply(GroupMessageEvent e){
        if(e.getGroup().getId() != playerGroup) return;
        if(!config.getBoolean("bot-auto-reply")) return;

        System.out.println("qwq!!!");

        String message = e.getMessage().contentToString();
        Set<String> rules = autoReply.getKeys(false);
        System.out.println(rules);

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            for(String ruleKey : rules){
                List<String> triggers = autoReply.getStringList(ruleKey + ".trigger");
                for(String trigger : triggers){
                    System.out.println(TextComparator.getSimilarity(message, trigger));
                    if(TextComparator.getSimilarity(message, trigger) * 100 >= autoReply.getInt(ruleKey + ".similarity", 100)){
                        BotOperator.sendGroupMessage(e.getGroup().getId(), autoReply.getString(ruleKey + ".reply", "null"));
                        return;
                    }
                }
            }
        });
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

            System.out.println(config.getKeys(false));
        }
    }
}
