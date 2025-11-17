
public class MessageType {
// 群聊消息格式：[GROUP]|发送者|消息内容

    public static final String GROUP_MSG = "[GROUP]";
// 私聊消息格式：[PRIVATE]|发送者|接收者|消息内容
    public static final String PRIVATE_MSG = "[PRIVATE]";
// 用户上线通知：[USER_ONLINE]|用户名
    public static final String USER_ONLINE = "[USER_ONLINE]";
// 用户下线通知：[USER_OFFLINE]|用户名
    public static final String USER_OFFLINE = "[USER_OFFLINE]";
// 在线用户列表同步：[USER_LIST]|用户名1,用户名2,用户名3
    public static final String USER_LIST = "[USER_LIST]";
}
