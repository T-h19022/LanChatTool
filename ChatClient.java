import javax.swing.*;

/**
 * 客户端主类:启动客户端窗口,发起服务器连接
 */
public class ChatClient {
    public static void main(String[] args) {
        
        SwingUtilities.invokeLater(() -> {
            ClientUI clientUI = new ClientUI();
            clientUI.setVisible(true); // 显示窗口

            // 启动后自动发起服务器连接
            boolean connectSuccess = clientUI.connectServer();
            if (!connectSuccess) {
                // 连接失败则关闭窗口
                clientUI.dispose();
                System.exit(0);
            }
        });
    }
}