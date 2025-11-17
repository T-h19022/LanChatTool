import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Enumeration;
import java.net.NetworkInterface;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.SocketException;

/**
 * 伺服器主類：監聽端口、接收客戶端連接、管理線上用戶
 */
public class ChatServer {

    // 伺服器端口（可自行修改，需與客戶端保持一致）
    private static final int SERVER_PORT = 8888;

    // 線上用戶集合：用戶名 → 客戶端 Socket（已使用同步 Map，執行緒安全）
    public static Map<String, Socket> onlineUsers = Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) {
        try {
            // 1. 建立 ServerSocket，監聽指定端口
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("=== LAN chat server is started ===");
            System.out.println("Listening port:：" + SERVER_PORT);
            System.out.println("Server IP (for client connection):" + getLocalIp());
            System.out.println("======================================\n");

            // 2. 持續循環接收客戶端連接（每連進一個客戶端，即開啟獨立執行緒處理）
            while (true) {
                Socket clientSocket = serverSocket.accept(); // 阻塞等待客戶端連接
                System.out.println("新客戶端連線：" + clientSocket.getInetAddress().getHostAddress());

                // 3. 為該客戶端啟動獨立處理執行緒（傳入 Socket 與線上用戶集合）
                new ClientHandlerThread(clientSocket, onlineUsers).start();
            }
        } catch (Exception e) {
            System.err.println("Server startup failed：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 取得本機局域網 IP（客戶端連接時必須輸入此 IP）
     * @return 局域網 IPv4 地址，異常時返回 127.0.0.1
     */
    private static String getLocalIp() {
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface ni = en.nextElement();
                Enumeration<InetAddress> ipEnum = ni.getInetAddresses();
                while (ipEnum.hasMoreElements()) {
                    InetAddress ip = ipEnum.nextElement();
                    // 排除本地回環地址（127.0.0.1）與 IPv6，只取局域網 IPv4
                    if (!ip.isLoopbackAddress() && ip instanceof Inet4Address) {
                        return ip.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            // 精確捕捉網路介面異常
            e.printStackTrace();
        }
        // 若無法取得局域網 IP（例如僅有回環或異常），返回 127.0.0.1（僅適合單機測試）
        return "127.0.0.1";
    }
}