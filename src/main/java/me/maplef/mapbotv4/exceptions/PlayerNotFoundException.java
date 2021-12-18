package me.maplef.mapbotv4.exceptions;

public class PlayerNotFoundException extends Exception{
    public PlayerNotFoundException(){
        super("未找到该玩家");
    }
}
