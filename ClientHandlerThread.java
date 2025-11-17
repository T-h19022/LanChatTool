
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

/**
 * 服务器线程类：每个客户端对应一个线程，负责读取消息、转发消息、处理上下线
 */
public class ClientHandlerThread extends Thread {

    private Socket clientSocket; // 当前客户端的Socket
    private Map<String, Socket> onlineUsers; // 所有在线用户
    private String username; // 当前客户端的用户名
    private BufferedReader in; // 读取客户端消息的输入流
    private PrintWriter out; // 向客户端发送消息的输出流
// 构造方法：初始化Socket、在线用户集合

    public ClientHandlerThread(Socket clientSocket, Map<String, Socket> onlineUsers) {
        this.clientSocket = clientSocket;
        this.onlineUsers = onlineUsers;
    }

    @Override
    public void run() {
        try {
// 1. 初始化输入流（读客户端消息）和输出流（发消息给客户端）
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));

            out = new PrintWriter(clientSocket.getOutputStream(), true); // autoFlush=true，无需-手动flush

// 2. 读取客户端发送的用户名（客户端连接后首先发送用户名）
            username = in.readLine();
            if (username == null || username.trim().isEmpty()) {
                out.println("用户名不能为空！");
                clientSocket.close();
                return;
            }
// 检查用户名是否已存在
            if (onlineUsers.containsKey(username.trim())) {
                out.println("用户名已被占用，请重新输入！");
                clientSocket.close();
                return;
            }
            username = username.trim();
// 3. 用户名合法：添加到在线用户集合，广播上线通知，同步用户列表
            onlineUsers.put(username, clientSocket);
            System.out.println("用户上线：" + username + "（当前在线：" + onlineUsers.size() + "-人）");

            broadcast(MessageType.USER_ONLINE + "|" + username); // 广播上线通知
            syncUserList(); // 同步所有客户端的在线用户列表
// 4. 循环读取客户端发送的消息，处理并转发
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("收到消息：" + message);
                processMessage(message); // 解析并处理消息
            }
        } catch (Exception e) {
// 客户端异常断开（如关闭窗口、网络中断）
            System.out.println("用户异常断开：" + username);
        } finally {
// 5. 客户端下线：移除在线用户，广播下线通知，同步用户列表，关闭资源
            if (username != null && onlineUsers.containsKey(username)) {
                onlineUsers.remove(username);
                System.out.println("用户下线：" + username + "（当前在线：" + onlineUsers.size() + "人）");

                broadcast(MessageType.USER_OFFLINE + "|" + username); // 广播下线通知
                syncUserList(); // 同步用户列表
            }
// 关闭流和Socket
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 处理客户端消息（区分群聊、私聊）
     */
    private void processMessage(String message) {
// 群聊消息：[GROUP]|发送者|内容
        if (message.startsWith(MessageType.GROUP_MSG)) {
            broadcast(message); // 直接广播给所有在线用户
        } // 私聊消息：[PRIVATE]|发送者|接收者|内容
        else if (message.startsWith(MessageType.PRIVATE_MSG)) {
            String[] parts = message.split("\\|"); // 分割消息（注意转义|）
// 私聊消息格式需满足：[PRIVATE]|发送者|接收者|内容（共4部分）
            if (parts.length == 4) {
                String sender = parts[1];
                String receiver = parts[2];
                String content = parts[3];
// 找到接收者的Socket
                Socket receiverSocket = onlineUsers.get(receiver);
                if (receiverSocket != null) {
                    try {
// 向接收者发送私聊消息
                        PrintWriter receiverOut = new PrintWriter(receiverSocket.getOutputStream(), true);
                        receiverOut.println(message);
// 向发送者回显消息（让发送者确认消息已发送）
                        out.println(message);
                    } catch (IOException e) {
                        out.println("私聊失败：接收者已离线！");
                    }
                } else {
// 接收者不在线，提示发送者
                    out.println("私聊失败：" + receiver + "不在线！");
                }
            } else {
                out.println("私聊消息格式错误！");
            }
        }
    }

    /**
     * 广播消息：向所有在线用户发送消息
     */
    private void broadcast(String message) {
        for (Socket socket : onlineUsers.values()) {
            try {
                PrintWriter userOut = new PrintWriter(socket.getOutputStream(), true);
                userOut.println(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 同步在线用户列表：向所有客户端发送当前在线用户名
     */
    private void syncUserList() {
// 拼接在线用户名（用逗号分隔）
        String userList = String.join(",", onlineUsers.keySet());
// 发送用户列表消息：[USER_LIST]|用户名1,用户名2
        broadcast(MessageType.USER_LIST + "|" + userList);
    }
}
