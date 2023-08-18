package me.maplef.mapbotv4.managers;

import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.MapbotPlugin;
import me.maplef.mapbotv4.exceptions.CommandNotFoundException;
import me.maplef.mapbotv4.exceptions.InvalidSyntaxException;
import net.mamoe.mirai.message.data.*;
import org.bukkit.plugin.PluginDescriptionFile;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

public class PluginManager {
    private final Set<Class<? extends MapbotPlugin>> pluginClasses;

    public PluginManager() {
        Reflections reflections = new Reflections(new ConfigurationBuilder().forPackage("me.maplef.mapbotv4.plugins"));
        this.pluginClasses = reflections.getSubTypesOf(MapbotPlugin.class);
    }

    @SuppressWarnings("unchecked")
    public MessageChain commandHandler(String command, Long groupID, Long senderID , Message[] args, QuoteReply quoteReply) throws Exception{
        switch (command) {
            case "help", "菜单" -> {
                MessageChainBuilder helpMsg = new MessageChainBuilder();
                helpMsg.append(new At(senderID)).append(" 你好，以下是我支持的命令\n(其中\"<>\"内的为必填参数，\"[]\"内的为可选参数)\n——————\n");

                for (Class<? extends MapbotPlugin> singleClass : this.pluginClasses) {
                    Map<String, Object> pluginInfo = (Map<String, Object>) singleClass.getMethod("register").invoke(singleClass.getDeclaredConstructor().newInstance());
                    if(pluginInfo == null) continue;

                    Map<String, String> pluginUsage = (Map<String, String>) pluginInfo.get("usages");

                    for (Map.Entry<String, String> entry : pluginUsage.entrySet())
                        helpMsg.append(entry.getValue()).append("\n");
                }

                return helpMsg.build();
            }
            case "about", "关于" -> {
                PluginDescriptionFile description = Main.getInstance().getDescription();
                String name = description.getName();
                String author = description.getAuthors().get(0);
                String version = description.getVersion();
                String github_url = "https://github.com/Maplefsnow/MapBotv4";

                return MessageUtils.newChain(new PlainText(String.format(
                        """
                                %s v%s
                                Author: %s
                                GitHub: %s
                                技术支持Q群: 835413855""",
                        name, version, author, github_url)));
            }
            case "plugins", "插件" -> {
                StringBuilder pluginInfoStrBuilder = new StringBuilder();

                for (Class<? extends MapbotPlugin> singleClass : this.pluginClasses) {
                    Map<String, Object> pluginInfo = (Map<String, Object>) singleClass.getMethod("register").invoke(singleClass.getDeclaredConstructor().newInstance());
                    if(pluginInfo == null) continue;

                    pluginInfoStrBuilder.append(String.format("%s - %s\nv%s\nAuthor: %s\n\n",
                            pluginInfo.get("name"), pluginInfo.get("description"), pluginInfo.get("version"), pluginInfo.get("author")));
                }

                String pluginInfoStr = pluginInfoStrBuilder.toString();
                pluginInfoStr = pluginInfoStr.trim();

                return MessageUtils.newChain(new PlainText(pluginInfoStr));
            }
            default -> {
                for (Class<? extends MapbotPlugin> singleClass : this.pluginClasses) {
                    Map<String, Object> pluginInfo = (Map<String, Object>) singleClass.getMethod("register").invoke(singleClass.getDeclaredConstructor().newInstance());
                    if(pluginInfo == null) continue;

                    Map<String, Method> pluginCommands = (Map<String, Method>) pluginInfo.get("commands");

                    if (pluginCommands.containsKey(command)) {
                        try {
                            return (MessageChain) pluginCommands.get(command).invoke(singleClass.getDeclaredConstructor().newInstance(), groupID, senderID, args, quoteReply);
                        } catch (InvocationTargetException e) {
                            MessageChainBuilder errorMsg = new MessageChainBuilder();
                            errorMsg.append(new At(senderID)).append(" ");

                            if (e.getTargetException() instanceof InvalidSyntaxException) {
                                Map<String, String> pluginUsage = (Map<String, String>) pluginInfo.get("usages");
                                errorMsg.append(" 语法错误\n用法: ").append(pluginUsage.get(command));
                            } else if (e.getTargetException() instanceof SQLException) {
                                errorMsg.append(" 数据库繁忙，请稍后重试");
                                e.getTargetException().printStackTrace();
                            } else {
                                errorMsg.append(e.getTargetException().getMessage());
                            }

                            return errorMsg.build();
                        }
                    }
                }
            }
        }

        throw new CommandNotFoundException();
    }
}
