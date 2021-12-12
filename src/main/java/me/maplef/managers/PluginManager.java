package me.maplef.managers;

import me.maplef.exceptions.CommandNotFoundException;
import me.maplef.exceptions.InvalidSyntaxException;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Map;

public class PluginManager {
    private static final String[] pluginsName = {"Hitokoto", "CatImage", "CheckTPS", "CheckMoney", "CheckOnlineTime", "Weather", "ListPlayers",
            "BindIDAndQQ", "CheckLocation", "StopServer", "WorldNews"};

    public static MessageChain commandHandler(String command, Long groupID, Long senderID , String[] args) throws Exception{
        switch (command){
            case "help": case "菜单": {
                MessageChainBuilder helpMsg = new MessageChainBuilder();
                helpMsg.append(new At(senderID)).append(" 你好，以下是我支持的命令\n(其中\"<>\"内的为必填参数，\"[]\"内的为可选参数)\n——————\n");
                for(String pluginName : pluginsName) {
                    Class<?> clazz = Class.forName("me.maplef.Plugins." + pluginName);
                    Object classObj = clazz.getDeclaredConstructor().newInstance();
                    Map<String, Object> pluginInfo = (Map<String, Object>) clazz.getMethod("register").invoke(classObj);
                    Map<String, String> pluginUsage = (Map<String, String>) pluginInfo.get("usages");

                    for (Map.Entry<String, String> entry : pluginUsage.entrySet())
                        helpMsg.append(entry.getValue()).append("\n");
                }
                return helpMsg.build();
            }

            default: {
                for(String pluginName : pluginsName){
                    Class<?> clazz = Class.forName("me.maplef.Plugins." + pluginName);
                    Object classObj = clazz.getDeclaredConstructor().newInstance();
                    Map<String, Object> pluginInfo = (Map<String, Object>) clazz.getMethod("register").invoke(classObj);

                    Map<String, Method> pluginCommands = (Map<String, Method>) pluginInfo.get("commands");
                    if(pluginCommands.containsKey(command)){
                        try{
                            return (MessageChain) pluginCommands.get(command).invoke(classObj, groupID, senderID, args);
                        } catch (InvocationTargetException e){
                            MessageChainBuilder errorMsg = new MessageChainBuilder();
                            errorMsg.append(new At(senderID)).append(" ");

                            if(e.getTargetException() instanceof InvalidSyntaxException){
                                Map<String, String> pluginUsage = (Map<String, String>) pluginInfo.get("usages");
                                errorMsg.append(" 语法错误\n用法: ").append(pluginUsage.get(command));
                            } else if(e.getTargetException() instanceof SQLException){
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












