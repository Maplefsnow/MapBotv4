package me.maplef.mapbotv4.exceptions;

public class CommandNotFoundException extends Exception{
    public CommandNotFoundException(){
        super("未知的指令");
    }
}
