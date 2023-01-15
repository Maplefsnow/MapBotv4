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
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.PermissionDeniedException;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.*;
import net.mamoe.mirai.message.data.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class PlayerGroupListeners extends SimpleListenerHost {
    ConfigManager configManager = new ConfigManager();
    FileConfiguration config = configManager.getConfig();
    private final Long opGroup = config.getLong("op-group");
    private final Long playerGroup = config.getLong("player-group");
    private final Long examineGroup = config.getLong("examine-group");

    private final Bot bot = BotOperator.getBot();
    private final PluginManager pluginManager = new PluginManager();

    static MessageChain repeatedMessage = null;
    static int repeatCount = 0;

    @Override
    public void handleException(@NotNull CoroutineContext context, @NotNull Throwable exception){
        Bukkit.getServer().getLogger().severe(exception.getMessage());
    }

    @EventHandler
    public void onCommandReceive(GroupMessageEvent e){
        FileConfiguration config = configManager.getConfig();
        if (e.getGroup().getId() != opGroup && e.getGroup().getId() != playerGroup && e.getGroup().getId() != examineGroup) return;

        String commandPattern = "^" + config.getString("bot-command-prefix") + "[\\u4E00-\\u9FA5A-Za-z0-9_]+(\\s([\\u4E00-\\u9FA5A-Za-z0-9_\\[\\]\\s]|[^\\x00-\\xff])+)?$";
        String mailPattern = "[\\w]+@[A-Za-z0-9]+(\\.[A-Za-z0-9]+){1,2}";

        MessageContent messageContent = e.getMessage().get(PlainText.Key);
        if(messageContent == null) return;

        boolean flag = false;
        String textString = messageContent.contentToString().trim();
        int firstLength = textString.length();
        textString = textString.replaceAll(mailPattern, textString);
        if (textString.length() != firstLength) {
            if (textString.contains("#审核") && e.getGroup().getId() == examineGroup) flag = true;
        }
        if (!flag && !Pattern.matches(commandPattern, textString)) return;

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
                message.append(pluginManager.commandHandler(command, e.getGroup().getId(), e.getSender().getId(), args, quoteReply));
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
        if (e.getGroup().getId() != opGroup && e.getGroup().getId() != playerGroup && e.getGroup().getId() != examineGroup) return;

        MessageContent messageContent = e.getMessage().get(PlainText.Key);
        if(messageContent == null) return;
        String textString = messageContent.contentToString().trim();
        String commandPattern = "^" + config.getString("bot-command-prefix") + "[\\u4E00-\\u9FA5A-Za-z0-9_]+(\\s([\\u4E00-\\u9FA5A-Za-z0-9_\\[\\]\\s]|[^\\x00-\\xff])+)?$";
        if(Pattern.matches(commandPattern, textString)) return;

        if(!config.getBoolean("message-forward.group-to-server.enable")) return;
        if(e.getGroup().getId() != config.getLong("player-group")) return;

        switch (config.getString("message-forward.group-to-server.mode", "all")) {
            case "all" -> {}

            case "prefix" -> {
                String prefix = config.getString("message-forward.group-to-server.prefix", ".");
                if(!textString.startsWith(prefix)) return;
            }

            default -> {
                Bukkit.getServer().getLogger().warning(String.format("[%s] config.yml: message-forward.group-to-server.mode 选择错误，请重新填写", Main.getInstance().getDescription().getName()));
                return;
            }
        }

        ClickEvent clickMsgHead = null; Component msgHeadComponent, msgContextComponent;
        try{
            clickMsgHead = ClickEvent.suggestCommand("@" + DatabaseOperator.queryPlayer(e.getSender().getId()).get("NAME") + " ");
        } catch (Exception ignored){}
        String msgHead = config.getString("message-head-format", "&f<&b{SENDER}&f> ").replace("{SENDER}",getMemberName(e));
        msgHeadComponent = Component.text(CU.t(msgHead)).clickEvent(clickMsgHead);

        try {
            msgContextComponent = QQMessageHandler.handle(e.getSender().getId(), e.getMessage());
        } catch (MessageLengthOutOfBoundException ex) {
            BotOperator.sendGroupMessage(e.getGroup().getId(), "本条消息过长，将不转发至服务器");
            return;
        } catch (MessageContainsBlockedWordsException ignored){return;}

        for(Player player : Bukkit.getServer().getOnlinePlayers()) {
            boolean sendFlag = true;
            try {
                sendFlag = (boolean) DatabaseOperator.queryPlayer(player.getName()).get("MSGREC");
            } catch (SQLException ex) {
                Bukkit.getLogger().warning(ex.getClass() + ": " + ex.getMessage());
            } catch (PlayerNotFoundException ignored) {}
            if (!sendFlag) continue;

            player.sendMessage(msgHeadComponent.append(msgContextComponent));
        }
    }

    @EventHandler
    public void onRepeat(GroupMessageEvent e){
        FileConfiguration config = configManager.getConfig();

        if(!config.getBoolean("bot-repeater.enabled")) return;
        if(e.getGroup().getId() != config.getLong("player-group")) return;
        MessageContent messageContent = e.getMessage().get(PlainText.Key);
        if(messageContent == null) return;
        String textString = messageContent.contentToString().trim();
        String commandPattern = "^" + config.getString("bot-command-prefix") + "[\\u4E00-\\u9FA5A-Za-z0-9_]+(\\s([\\u4E00-\\u9FA5A-Za-z0-9_\\[\\]\\s]|[^\\x00-\\xff])+)?$";
        if(Pattern.matches(commandPattern, textString)) return;

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
            String replyContent = "";

            for(String ruleKey : rules){
                String mode = autoReply.getString(ruleKey + ".mode", "similarity");

                switch (mode) {
                    case "similarity" -> {
                        List<String> triggers = autoReply.getStringList(ruleKey + ".trigger");
                        for (String trigger : triggers) {
                            double similarity = TextComparator.getLSTSimilarity(message, trigger) * 60
                                    + TextComparator.getCosSimilarity(message, trigger) * 40;
                            if (similarity >= autoReply.getInt(ruleKey + ".similarity", 100)) {
                                replyContent = autoReply.getString(ruleKey + ".reply.content", "null");
                            }
                        }
                    }
                    case "contains" -> {
                        List<String> triggers = autoReply.getStringList(ruleKey + ".trigger");
                        for (String trigger : triggers) {
                            if (message.contains(trigger)) {
                                replyContent = autoReply.getString(ruleKey + ".reply.content", "null");
                                break;
                            }
                        }
                    }
                    default -> {
                        Bukkit.getServer().getLogger().warning(String.format("[%s] auto_reply.yml: %s.mode 选择错误，请重新填写", Main.getInstance().getDescription().getName(), ruleKey));
                        return;
                    }
                }

                if(replyContent.isEmpty()) continue;

                switch (autoReply.getString(ruleKey + ".reply.type", "text")) {
                    case "text" -> BotOperator.sendGroupMessage(e.getGroup().getId(), replyContent);

                    case "command" -> {
                        String command = replyContent.split(" ", 2)[0];
                        ArrayList<Message> argsList = new ArrayList<>();
                        for(String str : replyContent.split(" ")) argsList.add(new PlainText(str));
                        argsList.remove(0);

                        MessageChainBuilder cmdRes = new MessageChainBuilder();
                        try {
                            cmdRes.append(pluginManager.commandHandler(command, e.getGroup().getId(), e.getSender().getId(), argsList.toArray(new Message[0]), null));
                        } catch (CommandNotFoundException ex) {
                            cmdRes.append(ex.getMessage());
                        } catch (Exception ex){
                            ex.printStackTrace();
                        }
                        BotOperator.sendGroupMessage(e.getGroup().getId(), cmdRes.build());
                    }

                    default -> Bukkit.getServer().getLogger().warning(String.format("[%s] auto_reply.yml: %s.reply.type 选择错误，请重新填写", Main.getInstance().getDescription().getName(), ruleKey));
                }

                break;
            }
        });
    }

    @EventHandler
    public void onConsoleCommandReceive(GroupMessageEvent e){
        FileConfiguration config = configManager.getConfig();

        MessageContent messageContent = e.getMessage().get(PlainText.Key);
        if(messageContent == null) return;
        String textString = messageContent.contentToString().trim();

        String consoleCommandPrefix = config.getString("console-command-prefix", "/");

        if(!textString.startsWith(consoleCommandPrefix)) return;
        if(!config.getLongList("super-admin-account").contains(e.getSender().getId())) return;

        StringBuilder resBuilder = new StringBuilder();
        Consumer<ComponentLike> consumer = res -> resBuilder.append(PlainTextComponentSerializer.plainText().serialize(res.asComponent())).append("\n");
        CommandSender commandSender = Bukkit.createCommandSender(consumer);
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> Bukkit.dispatchCommand(commandSender, textString.substring(consoleCommandPrefix.length())));

        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            if(resBuilder.isEmpty())
                BotOperator.sendGroupMessage(e.getGroup().getId(), "控制台指令已发送，但未获取到返回信息");
            else
                BotOperator.sendGroupMessage(e.getGroup().getId(), resBuilder.toString().trim());
        }, 5);
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
                    + "，ID绑定已解除，白名单已移除";

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

        try {
            DatabaseOperator.queryExamine(e.getFromId()).get("APPROVED");
        } catch (PlayerNotFoundException ex) {
            e.reject(false, Objects.requireNonNull(config.getString("player-group-auto-manage.reject-message")));
            BotOperator.sendGroupMessage(config.getLong("op-group"), "已拒绝 " + e.component7() + " 入群");
            return;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        String code = e.getMessage().replaceAll(".*\\n.*答案：", "");
        try {
            String InvCode = (String) DatabaseOperator.queryExamine(e.getFromId()).get("CODE");
            if (InvCode.equals(code)) {
                e.accept();
                BotOperator.sendGroupMessage(config.getLong("op-group"), "已同意 " + e.component7() + " 入群");
                String order = String.format("UPDATE EXAMINE SET USED = 1 WHERE QQ = '%s';", e.getFromId());
                new DatabaseOperator().executeCommand(order);
            }
            else {
                e.reject(false, "邀请码错误");
                BotOperator.sendGroupMessage(config.getLong("op-group"), "已拒绝 " + e.component7() + " 入群，该玩家的邀请码为" + InvCode + "，而玩家填入的邀请码为" + code);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @EventHandler
    public void onNudge(NudgeEvent e) {
        FileConfiguration config = configManager.getConfig();
        FileConfiguration messages = configManager.getMessageConfig();

        long groupID = e.component3().getId();

        if(!config.getBoolean("nudge.enable")) return;
        if(groupID != config.getLong("player-group")) return;

        if(e.getTarget().equals(e.getBot())) {
            List<String> botNudgeMsg = messages.getStringList("bot-nudge");
            Random random = new Random();
            BotOperator.sendGroupMessage(groupID, MessageUtils.newChain(new At(e.getFrom().getId()),
                    new PlainText(" " + botNudgeMsg.get(random.nextInt(botNudgeMsg.size())))));
            return;
        }

        Bot bot = e.getBot();
        String targetPlayerName, fromPlayerName;
        try {
             targetPlayerName = (String) DatabaseOperator.queryPlayer(e.getTarget().getId()).get("NAME");
        } catch (PlayerNotFoundException ex) {
            targetPlayerName = Objects.requireNonNull(Objects.requireNonNull(bot.getGroup(groupID)).get(e.getTarget().getId())).getNameCard();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return;
        }
        try {
            fromPlayerName = (String) DatabaseOperator.queryPlayer(e.getFrom().getId()).get("NAME");
        } catch (PlayerNotFoundException ex) {
            fromPlayerName = Objects.requireNonNull(Objects.requireNonNull(bot.getGroup(groupID)).get(e.getFrom().getId())).getNameCard();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return;
        }

        for(Player player : Bukkit.getServer().getOnlinePlayers()) {
            if(player.getName().toLowerCase(Locale.ROOT).equals(targetPlayerName.toLowerCase(Locale.ROOT))) {
                if(config.getBoolean("nudge.target-player-action.message.enable")) {
                    player.sendMessage(Component.text("[戳一戳] ").color(NamedTextColor.GREEN)
                            .append(Component.text(fromPlayerName + e.getAction() + "你" + e.getSuffix()).color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD)));
                }
                if(config.getBoolean("nudge.target-player-action.damage.enable")){
                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                        player.damage(config.getDouble("nudge.target-player-action.damage.value"), player);
                    });
                }
                if(config.getBoolean("nudge.target-player-action.sound.enable"))
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BAMBOO_HIT, (float) config.getDouble("nudge.target-player-action.sound.volume", 1.0f), 1.0f);
                if(config.getBoolean("nudge.target-player-action.animation.enable")) {
                    Location location = player.getLocation();

                    double x = location.getX();
                    double y = location.getY();
                    double z = location.getZ();
                    double r = config.getDouble("nudge.target-player-action.animation.radius");

                    for(int i = 0; i <= 359; i++) {
                        double x1 = x + r * Math.sin((double)i * Math.PI / 180.0);
                        double z1 = z + r * Math.cos((double)i * Math.PI / 180.0);

                        player.spawnParticle(Particle.valueOf(config.getString("nudge.target-player-action.animation.particle", "SNOWBALL")), new Location(player.getWorld(), x1, y, z1),
                                config.getInt("nudge.target-player-action.animation.count", 5));
                    }
                }
            }
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

    public String getMemberName(GroupMessageEvent e) {
        String nameCard = e.getSender().getNameCard();
        if (nameCard.isEmpty()) {
            return e.getSender().getNick();
        }else {
            return nameCard;
        }
    }
}
