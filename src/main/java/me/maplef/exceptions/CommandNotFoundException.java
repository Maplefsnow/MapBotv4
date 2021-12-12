package me.maplef.exceptions;

public class CommandNotFoundException extends Exception{
    public CommandNotFoundException(){
        super("未知的指令");
    }
}
