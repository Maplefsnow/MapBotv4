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
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MemberJoinEvent;
import net.mamoe.mirai.event.events.MemberJoinRequestEvent;
import net.mamoe.mirai.event.events.MemberLeaveEvent;
import net.mamoe.mirai.message.data.*;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

public class PlayerGroupListeners extends SimpleListenerHost {
    final FileConfiguration config = Main.getPlugin(Main.class).getConfig();
    final FileConfiguration messages = Main.getInstance().getMessageConfig();

    private final Bot bot = BotOperator.getBot();

    private final Long botAcc = config.getLong("bot-account");
    private final Long playerGroup = config.getLong("player-group");
    private final Long opGroup = config.getLong("op-group");
    private final Long checkInGroup = config.getLong("check-in-group");

    static final LinkedList<Message> messageRecorder = new LinkedList<>();
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
    public void onMessageReceive(GroupMessageEvent e){
        if(e.getGroup().getId() != playerGroup) return;
        if(Pattern.matches(commandPattern, e.getMessage().contentToString())) return;

        String msg = "", atID = "";

        for(Message message : e.getMessage()){
            if(message instanceof At){
                String atNum = String.valueOf(((At) message).getTarget());
                if(atNum.equals(botAcc.toString())) continue;

                try {
                    atID = (String) DatabaseOperator.query(Long.parseLong(atNum)).get("NAME");
                    continue;
                } catch (SQLException ex) {
                    ex.printStackTrace();
                } catch (PlayerNotFoundException ignored){}
            } else if (message instanceof AtAll){
                atID = "全体成员";
            }

            msg = msg.concat(message.contentToString());
        }

        if(msg.length() > config.getInt("message-length-limit")){
            BotOperator.sendGroupMessage(e.getGroup().getId(), "本条消息过长，将不转发至服务器");
            return;
        }

        for(Player player : Bukkit.getServer().getOnlinePlayers()){
            String msgHead;
            if(!atID.isEmpty()){
                if(player.getName().equals(atID) || atID.equals("全体成员")){
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
        if(!config.getBoolean("bot-repeater.enabled")) return;
        if(e.getGroup().getId() != playerGroup) return;

        messageRecorder.push(e.getMessage());
        if(messageRecorder.size() == 3){
            if(messageRecorder.get(0).contentEquals(messageRecorder.get(1), false) &&
                messageRecorder.get(1).contentEquals(messageRecorder.get(2), false)){
                messageRecorder.clear();
                BotOperator.sendGroupMessage(e.getGroup().getId(), e.getMessage());
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
            BotOperator.sendGroupMessage(playerGroup, msg);
        }
    }

    @EventHandler
    public void onNewCome(MemberJoinEvent e){
        if(e.getGroupId() != playerGroup) return;

        BotOperator.sendGroupMessage(e.getGroupId(), WelcomeNew.WelcomeMessage());
        Bukkit.getServer().broadcastMessage(CU.t(messages.getString("message-prefix") + messages.getString("welcome-new-message.player-group.server")));
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

            playerGroupMsg = Objects.requireNonNull(messages.getString("exit-player-group-message.op-group"))
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

            playerGroupMsg = Objects.requireNonNull(messages.getString("exit-player-group-message.player-group"))
                    .replace("{PLAYER}", ID)
                    .replace("{SERVERNAME}", Objects.requireNonNull(messages.getString("server-name")));

            opGroupMsg = Objects.requireNonNull(messages.getString("exit-player-group-message.op-group"))
                    .replace("{PLAYER}", ID)
                    .replace("{SERVERNAME}", Objects.requireNonNull(messages.getString("server-name")))
                    + "，ID绑定已解除，白名单已移除";
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
