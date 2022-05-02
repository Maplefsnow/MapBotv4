package me.maplef.mapbotv4.exceptions;

public class MessageLengthOutOfBoundException extends Exception{
    public MessageLengthOutOfBoundException(){
        super("本条消息过长，将不转发至服务器");
    }
}
