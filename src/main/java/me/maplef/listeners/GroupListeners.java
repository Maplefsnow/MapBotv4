package me.maplef.listeners;

import me.maplef.exceptions.CommandNotFoundException;
import me.maplef.exceptions.PlayerNotFoundException;
import me.maplef.managers.PluginManager;
import me.maplef.loops.InnerGroupInvite;
import me.maplef.Main;
import me.maplef.plugins.WelcomeNew;
import me.maplef.utils.BotOperator;
import me.maplef.utils.CU;
import me.maplef.utils.DatabaseOperator;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MemberJoinEvent;
import net.mamoe.mirai.event.events.MemberJoinRequestEvent;
import net.mamoe.mirai.event.events.MemberLeaveEvent;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.regex.Pattern;

public class GroupListeners extends SimpleListenerHost {
    final FileConfiguration config = me.maplef.Main.getPlugin(me.maplef.Main.class).getConfig();
    final FileConfiguration messages = Main.getInstance().getMessageConfig();

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

                Bukkit.getLogger().info("message: " + message);
                Bukkit.getLogger().info("atNum: " + atNum);

                if(atNum.equals(botAcc.toString())) continue;

                try {
                    atID = (String) DatabaseOperator.query(Long.parseLong(atNum)).get("NAME");
                    continue;
                } catch (SQLException | PlayerNotFoundException ex) {
                    ex.printStackTrace();
                }
            }

            msg = msg.concat(message.contentToString());
        }

        if(msg.length() > config.getInt("message-length-limit")){
            BotOperator.send(e.getGroup().getId(), "本条消息过长，将不转发至服务器");
            return;
        }

        for(Player player : Bukkit.getServer().getOnlinePlayers()){
            String linkedMsg;
            if(!atID.isEmpty()){
                if(player.getName().equals(atID)){
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1f, 1.5f);
                    linkedMsg = String.format("<&b%s&f> &a&l@%s&f%s", e.getSender().getNameCard(), atID, msg);
                } else
                    linkedMsg = String.format("<&b%s&f> &f&l@%s&f%s", e.getSender().getNameCard(), atID, msg);
            } else
                linkedMsg = String.format("<&b%s&f> %s", e.getSender().getNameCard(), msg);

            int sendFlag = 1;

            try {
                sendFlag = (Integer) DatabaseOperator.query(player.getName()).get("MSGREC");
            } catch (SQLException | PlayerNotFoundException ex) {
                Bukkit.getLogger().warning(ex.getClass() + ": " + ex.getMessage());
            }

            if(sendFlag == 1) player.sendMessage(CU.t(linkedMsg));
        }
    }

    @EventHandler
    public void onRepeat(GroupMessageEvent e){
        if(e.getGroup().getId() != playerGroup) return;
        messageRecorder.push(e.getMessage());
        if(messageRecorder.size() == 3){
            if(messageRecorder.get(0).equals(messageRecorder.get(1)) && messageRecorder.get(1).equals(messageRecorder.get(2))){
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
        Bukkit.getServer().broadcastMessage(CU.t("&f[&b&l小枫4号&f] &a有新猫猫加入猫猫大陆啦，快在群里欢迎ta吧！"));
    }

    @EventHandler
    public void onExitPlayerGroup(MemberLeaveEvent e){
        if(e.getGroupId() != playerGroup) return;

        String QQ = e.getMember().getId() + "";
        String ID = "";
        String reportMsg;

        Connection c = DatabaseOperator.c;
        try (Statement stmt = c.createStatement();
             ResultSet res = stmt.executeQuery("SELECT * FROM PLAYER;")){
            while (res.next()){
                if(res.getString("QQ").equals(QQ)){
                    ID = res.getString("NAME");
                    stmt.executeUpdate(String.format("DELETE FROM PLAYER WHERE QQ = '%s';", QQ));
                    break;
                }
            }
        } catch (Exception exception){
            Bukkit.getLogger().warning(exception.getClass().getName() + ": " + exception.getMessage());
        }

        if(ID.isEmpty()){
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
    public void onJoinGroupRequest(MemberJoinRequestEvent e){
        if(e.getGroupId() != innerGroup) return;

        Bukkit.getLogger().info(e.component3());
        Bukkit.getLogger().info(e.component6());
        Bukkit.getLogger().info(e.component7());

        String code = e.getMessage().substring(e.getMessage().length() - 6);
        if(code.equals(InnerGroupInvite.inviteCode)) e.accept();
        else e.reject(false, "请输入正确的凭证以进入内群qwq");
    }

    @EventHandler
    public void onTest(GroupMessageEvent e){
        if(e.getGroup().getId() != opGroup) return;

        if(e.getMessage().contentToString().contains("test")){
            Bot bot = BotOperator.bot;
            try{
                Objects.requireNonNull(Objects.requireNonNull(Objects.requireNonNull(bot.getGroup(playerGroup)).get(1329785932))).sendMessage("test message: qwq");
            } catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }
}
