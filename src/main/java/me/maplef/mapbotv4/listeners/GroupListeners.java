package me.maplef.mapbotv4.listeners;

import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.MapbotPlugin;
import me.maplef.mapbotv4.exceptions.CommandNotFoundException;
import me.maplef.mapbotv4.exceptions.PlayerNotFoundException;
import me.maplef.mapbotv4.managers.PluginManager;
import me.maplef.mapbotv4.plugins.WelcomeNew;
import me.maplef.mapbotv4.utils.BotOperator;
import me.maplef.mapbotv4.utils.CU;
import me.maplef.mapbotv4.utils.DatabaseOperator;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MemberJoinEvent;
import net.mamoe.mirai.event.events.MemberLeaveEvent;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.reflections.Reflections;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

public class GroupListeners extends SimpleListenerHost {
    final FileConfiguration config = Main.getPlugin(Main.class).getConfig();
    final FileConfiguration messages = Main.getInstance().getMessageConfig();

    private final Bot bot = BotOperator.getBot();

    private final Long botAcc = config.getLong("bot-account");
    private final Long playerGroup = config.getLong("player-group");
    private final Long opGroup = config.getLong("op-group");
    private final Long innerGroup = config.getLong("inner-player-group");

    static final LinkedList<Message> messageRecorder = new LinkedList<>();
    private final String commandPattern = "^#[\\u4E00-\\u9FA5A-Za-z0-9_]+(\\s[\\u4E00-\\u9FA5A-Za-z0-9_\\s]+)?$";

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
        BotOperator.send(e.getGroup().getId(), message.build());
    }

    @EventHandler
    public void onMessageReceive(GroupMessageEvent e){
        if(e.getGroup().getId() != playerGroup) return;
        if(Pattern.matches(commandPattern, e.getMessage().contentToString())) return;

        String msg = "", atID = "";

        for(Message message : e.getMessage()){
            if(message.contentToString().startsWith("@")){
                String atNum = message.contentToString().substring(1);
                if(atNum.equals(botAcc.toString())) continue;

                try {
                    atID = (String) DatabaseOperator.query(Long.parseLong(atNum)).get("NAME");
                    continue;
                } catch (SQLException ex) {
                    ex.printStackTrace();
                } catch (PlayerNotFoundException ignored){}
            }

            msg = msg.concat(message.contentToString());
        }

        if(msg.length() > config.getInt("message-length-limit")){
            BotOperator.send(e.getGroup().getId(), "本条消息过长，将不转发至服务器");
            return;
        }

        for(Player player : Bukkit.getServer().getOnlinePlayers()){
            String msgHead;
            if(!atID.isEmpty()){
                if(player.getName().equals(atID)){
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1f, 1.5f);
                    msgHead = String.format("<&b%s&f> &a&l@%s&f", e.getSender().getNameCard(), atID);
                } else
                    msgHead = String.format("<&b%s&f> &f&l@%s&f", e.getSender().getNameCard(), atID);
            } else
                msgHead = String.format("<&b%s&f> ", e.getSender().getNameCard());

            int sendFlag = 1;

            try {
                sendFlag = (Integer) DatabaseOperator.query(player.getName()).get("MSGREC");
            } catch (SQLException ex) {
                Bukkit.getLogger().warning(ex.getClass() + ": " + ex.getMessage());
            } catch (PlayerNotFoundException ignored){}

            if(sendFlag == 1){
                if(!Objects.requireNonNull(bot.getGroup(opGroup)).contains(e.getSender().getId())){
                    msg = msg.replace("§", "");
                    player.sendMessage(CU.t(msgHead) + msg);
                } else {
                    player.sendMessage(CU.t(msgHead + msg));
                }
            }
        }
    }

    @EventHandler
    public void onRepeat(GroupMessageEvent e){
        if(e.getGroup().getId() != playerGroup) return;
        messageRecorder.push(e.getMessage());
        if(messageRecorder.size() == 3){
            if(messageRecorder.get(0).contentEquals(messageRecorder.get(1), false) &&
                messageRecorder.get(1).contentEquals(messageRecorder.get(2), false)){
                messageRecorder.clear();
                BotOperator.send(e.getGroup().getId(), e.getMessage());
            }

            messageRecorder.clear();
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
            BotOperator.send(playerGroup, msg);
        }
    }

    @EventHandler
    public void onNewCome(MemberJoinEvent e){
        if(e.getGroupId() != playerGroup) return;
        BotOperator.send(e.getGroupId(), WelcomeNew.WelcomeMessage());
        Bukkit.getServer().broadcastMessage(CU.t(messages.getString("message-prefix") + messages.getString("welcome-new-message.server")));
    }

    @EventHandler
    public void onExitPlayerGroup(MemberLeaveEvent e){
        if(e.getGroupId() != playerGroup) return;

        Long QQ = e.getMember().getId();
        String ID = null;
        String reportMsg;

        try{
            ID = DatabaseOperator.query(QQ).get("NAME").toString();
        } catch (SQLException ex){
            ex.printStackTrace();
        } catch (PlayerNotFoundException ignored){}

        if(ID == null){
            reportMsg = String.format("玩家 %s 已退出猫猫大陆，未检测到该玩家的绑定ID行为", e.getMember().getNameCard());
        } else {
            String whitelistDelCommand = String.format("whitelist remove %s", ID);
            new BukkitRunnable(){
                @Override
                public void run(){
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), whitelistDelCommand);
                }
            }.runTask(Main.getPlugin(Main.class));
            reportMsg = String.format("玩家 %s 已退出猫猫大陆，ID绑定已解除，白名单已移除", ID);
        }

        BotOperator.send(opGroup, reportMsg);
    }

    @EventHandler
    public void onTest(GroupMessageEvent e){
        if(e.getGroup().getId() != opGroup) return;

        if(e.getMessage().contentToString().contains("test")){
            Reflections reflections = new Reflections("me.maplef.mapbotv4.plugins");
            Set<Class<? extends MapbotPlugin>> classes = reflections.getSubTypesOf(MapbotPlugin.class);
            for(Class<? extends MapbotPlugin> singleClass : classes)
                Bukkit.getLogger().info(singleClass.getName());
        }
    }
}
