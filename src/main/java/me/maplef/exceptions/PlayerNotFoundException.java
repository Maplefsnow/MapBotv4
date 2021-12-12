package me.maplef.exceptions;

public class PlayerNotFoundException extends Exception{
    public PlayerNotFoundException(){
        super("未找到该玩家");
    }
}
