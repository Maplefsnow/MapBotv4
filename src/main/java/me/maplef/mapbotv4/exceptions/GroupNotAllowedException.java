package me.maplef.mapbotv4.exceptions;

public class GroupNotAllowedException extends Exception{
    public GroupNotAllowedException(){
        super("此功能未在本群启用");
    }
}
