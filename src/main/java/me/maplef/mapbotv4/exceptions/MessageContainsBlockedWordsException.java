package me.maplef.mapbotv4.exceptions;

public class MessageContainsBlockedWordsException extends Exception{
    public MessageContainsBlockedWordsException(){
        super("消息包含违禁词");
    }
}
