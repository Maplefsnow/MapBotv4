package me.maplef.mapbotv4.plugins;

import me.maplef.mapbotv4.Main;
import me.maplef.mapbotv4.MapbotPlugin;
import me.maplef.mapbotv4.exceptions.GroupNotAllowedException;
import me.maplef.mapbotv4.exceptions.PlayerNotFoundException;
import me.maplef.mapbotv4.managers.ConfigManager;
import me.maplef.mapbotv4.utils.DatabaseOperator;
import net.mamoe.mirai.message.data.*;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Pay implements MapbotPlugin {
    public boolean pay(String payerName, String payeeName, double money) throws Exception {
        UUID payerUUID, payeeUUID;

        try{
            payerUUID = UUID.fromString(DatabaseOperator.queryPlayer(payerName).get("UUID").toString());
        } catch (PlayerNotFoundException e){
            throw new Exception("未找到付款人");
        }
        try{
            payeeUUID = UUID.fromString(DatabaseOperator.queryPlayer(payeeName).get("UUID").toString());
        } catch (PlayerNotFoundException e){
            throw new Exception("未找到收款人");
        }

        OfflinePlayer payer = Bukkit.getOfflinePlayer(payerUUID);
        OfflinePlayer payee = Bukkit.getOfflinePlayer(payeeUUID);

        Economy econ = Main.getEconomy();

        double payerMoney = econ.getBalance(payer);
        if(payerMoney < money)
            throw new Exception("账户余额不足");

        EconomyResponse r = econ.withdrawPlayer(payer, money);
        if(r.transactionSuccess()){
            r = econ.depositPlayer(payee, money);
            if(r.transactionSuccess()) return true;
            else throw new Exception(r.errorMessage);
        } else throw new Exception(r.errorMessage);
    }

    @Override
    public MessageChain onEnable(@NotNull Long groupID, @NotNull Long senderID, Message[] args, @Nullable QuoteReply quoteReply) throws Exception {
        FileConfiguration config = new ConfigManager().getConfig();

        if(groupID != config.getLong("player-group") && groupID != config.getLong("op-group")) throw new GroupNotAllowedException();

        if(args.length == 0)
            return MessageUtils.newChain(new At(senderID)).plus(" 请指定收款人和付款金额");
        else if(args.length == 1)
            return MessageUtils.newChain(new At(senderID)).plus(" 请输入付款金额");

        double money;
        try{
            money = Double.parseDouble(args[1].contentToString());
            if(money <= 0) return MessageUtils.newChain(new At(senderID)).plus(" 请给定一个正实数");
        } catch (NumberFormatException e){
            return MessageUtils.newChain(new At(senderID)).plus(" 请给定一个正实数");
        }

        String payerName = DatabaseOperator.queryPlayer(senderID).get("NAME").toString();

        try{
            if(pay(payerName, args[0].contentToString(), money))
                return MessageUtils.newChain(new At(senderID)).plus(" 支付成功");
            else return MessageUtils.newChain(new At(senderID)).plus(" 支付失败");
        } catch (Exception e){
            return MessageUtils.newChain(new At(senderID)).plus(e.getMessage());
        }
    }

    @Override
    public Map<String, Object> register() throws NoSuchMethodException {
        Map<String, Object> info = new HashMap<>();
        Map<String, Method> commands = new HashMap<>();
        Map<String, String> usages = new HashMap<>();

        commands.put("pay", Pay.class.getMethod("onEnable", Long.class, Long.class, Message[].class, QuoteReply.class));

        usages.put("pay", "#pay <收款人> <金额> - 向指定玩家付款");

        info.put("name", "pay");
        info.put("commands", commands);
        info.put("usages", usages);
        info.put("author", "Maplef");
        info.put("description", "付款");
        info.put("version", "1.0");

        return info;
    }
}
