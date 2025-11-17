import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Client UI class: implements the chat window (online user list, chat area, input area)
 */
public class ClientUI extends JFrame {
    private JList<String> userList;                    // Online user list
    private DefaultListModel<String> userListModel;    // Data model for user list
    private JTextArea chatArea;                        // Chat message display area
    private JTextField inputField;                     // Message input box
    private JButton sendBtn;                           // Send button
    private JComboBox<String> chatTypeCombo;           // Chat type (Group / Private)
    private Socket socket;                             // Socket connection to server
    private PrintWriter out;                           // Output stream to send messages to server
    private String username;                           // Current client's username
    private BufferedReader in;                         // Input stream to read server messages

    /**
     * Constructor: initialize the UI
     */
    public ClientUI() {
        initUI();                                      // Initialize UI components
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);                             // Window size
        setLocationRelativeTo(null);                   // Center the window
        setTitle("LAN Chat Tool - Not Logged In");
        setResizable(false);                           // Disable window resizing
    }

    /**
     * Initialize UI components (layout, styling, event binding)
     */
    private void initUI() {
        // 1. Main layout: BorderLayout
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // 2. Left panel: Online user list (takes 1/4 width)
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(200, 0)); // Fixed width 200px

        // 2.1 List title
        JLabel userListTitle = new JLabel(" Online Users (0)");
        userListTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        userListTitle.setOpaque(true);
        userListTitle.setBackground(new Color(230, 230, 230));
        leftPanel.add(userListTitle, BorderLayout.NORTH);

        // 2.2 User list (non-editable, supports selecting private chat target)
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Single selection only
        leftPanel.add(new JScrollPane(userList), BorderLayout.CENTER);
        add(leftPanel, BorderLayout.WEST);

        // 3. Right panel: Chat area + input area (takes 3/4 width)
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));

        // 3.1 Chat display area (read-only, with scrollbar)
        chatArea = new JTextArea();
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);        // Auto line wrap
        chatArea.setWrapStyleWord(true);   // Wrap at word boundaries
        rightPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // 3.2 Input panel (chat type + input box + send button)
        JPanel inputPanel = new JPanel(new BorderLayout(10, 10));

        // 3.2.1 Chat type selector (Group / Private)
        String[] chatTypes = {"Group Chat", "Private Chat"};
        chatTypeCombo = new JComboBox<>(chatTypes);
        chatTypeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chatTypeCombo.setPreferredSize(new Dimension(120, 0));
        inputPanel.add(chatTypeCombo, BorderLayout.WEST);

        // 3.2.2 Message input field
        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        inputField.setToolTipText("Type your message... (Press Enter to send)");
        inputPanel.add(inputField, BorderLayout.CENTER);

        // 3.2.3 Send button
        sendBtn = new JButton("Send");
        sendBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sendBtn.setPreferredSize(new Dimension(100, 0));
        inputPanel.add(sendBtn, BorderLayout.EAST);

        rightPanel.add(inputPanel, BorderLayout.SOUTH);
        add(rightPanel, BorderLayout.CENTER);

        // 4. Bind send events (button click or Enter key)
        sendBtn.addActionListener(new SendListener());
        inputField.addActionListener(new SendListener());
    }

    /**
     * Connect to server: enter username and server IP, establish Socket connection
     */
    public boolean connectServer() {
        // 1. Prompt for username
        username = JOptionPane.showInputDialog(this, "Please enter your username:", "Login", JOptionPane.PLAIN_MESSAGE);
        if (username == null || username.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        username = username.trim();

        // 2. Prompt for server IP (pre-filled with local LAN IP)
        Object serverIpObj = JOptionPane.showInputDialog(
                this,
                "Please enter server IP:",
                "Connect to Server",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                getLocalIp()
        );
        if (serverIpObj == null) {
            JOptionPane.showMessageDialog(this, "Connection cancelled.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        String serverIp = serverIpObj.toString().trim();
        if (serverIp.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Server IP cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        int serverPort = 8888;

        // 3. Establish Socket connection
        try {
            socket = new Socket(serverIp, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

            // 4. Send username to server
            out.println(username);
            setTitle("LAN Chat Tool - " + username);

            // 5. Start thread to listen for server messages
            new Thread(this.new ServerMessageReader()).start();
            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Failed to connect to server: " + e.getMessage() + "\nPlease check IP and port.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
    }

    /**
     * Process messages received from server
     */
    private void processServerMessage(String message) {
        if (message.startsWith(MessageType.USER_LIST)) {
            String[] parts = message.split("\\|");
            if (parts.length == 2) {
                String[] users = parts[1].split(",");
                userListModel.clear();
                for (String user : users) {
                    userListModel.addElement(user);
                }
                // Update online count in title
                try {
                    JScrollPane scrollPane = (JScrollPane) userList.getParent().getParent();
                    JPanel leftPanel = (JPanel) scrollPane.getParent();
                    JLabel title = (JLabel) leftPanel.getComponent(0);
                    title.setText(" Online Users (" + users.length + ")");
                } catch (Exception ignored) {}
            }
        } else if (message.startsWith(MessageType.USER_ONLINE)) {
            String[] parts = message.split("\\|");
            if (parts.length == 2) {
                chatArea.append("System: " + parts[1] + " is online!\n");
            }
        } else if (message.startsWith(MessageType.USER_OFFLINE)) {
            String[] parts = message.split("\\|");
            if (parts.length == 2) {
                chatArea.append("System: " + parts[1] + " went offline.\n");
            }
        } else if (message.startsWith(MessageType.GROUP_MSG)) {
            String[] parts = message.split("\\|");
            if (parts.length == 3) {
                if (parts[1].equals(username)) {
                    chatArea.append("[" + parts[1] + " (Me-Group)]: " + parts[2] + "\n");
                } else {
                    chatArea.append("[" + parts[1] + " (Group)]: " + parts[2] + "\n");
                }
            }
        } else if (message.startsWith(MessageType.PRIVATE_MSG)) {
            String[] parts = message.split("\\|");
            if (parts.length == 4) {
                String sender = parts[1];
                String receiver = parts[2];
                String content = parts[3];
                if (sender.equals(username)) {
                    chatArea.append("[" + username + " (Private to " + receiver + ")]: " + content + "\n");
                } else {
                    chatArea.append("[" + sender + " (Private to me)]: " + content + "\n");
                }
            }
        } else {
            chatArea.append("System: " + message + "\n");
        }
        // Auto-scroll to bottom
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    /**
     * Send button / Enter key listener
     */
    private class SendListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String content = inputField.getText().trim();
            if (content.isEmpty()) {
                JOptionPane.showMessageDialog(ClientUI.this, "Message cannot be empty!", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String chatType = (String) chatTypeCombo.getSelectedItem();
            if ("Group Chat".equals(chatType)) {
                String groupMsg = MessageType.GROUP_MSG + "|" + username + "|" + content;
                out.println(groupMsg);
            } else if ("Private Chat".equals(chatType)) {
                String receiver = userList.getSelectedValue();
                if (receiver == null) {
                    JOptionPane.showMessageDialog(ClientUI.this, "Please select a recipient!", "Warning", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String privateMsg = MessageType.PRIVATE_MSG + "|" + username + "|" + receiver + "|" + content;
                out.println(privateMsg);

                // Immediately display sent private message
                chatArea.append("[" + username + " (Private to " + receiver + ")]: " + content + "\n");
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
            }
            inputField.setText("");
        }
    }

    /**
     * Get local LAN IP (used as default in server IP input box)
     */
    private String getLocalIp() {
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface ni = en.nextElement();
                Enumeration<InetAddress> ipEnum = ni.getInetAddresses();
                while (ipEnum.hasMoreElements()) {
                    InetAddress ip = ipEnum.nextElement();
                    if (!ip.isLoopbackAddress() && ip instanceof Inet4Address) {
                        return ip.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }

    /**
     * Clean up resources when window is closed
     */
    @Override
    public void dispose() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.dispose();
    }

    /**
     * Thread to continuously read messages from server
     */
    private class ServerMessageReader implements Runnable {
        @Override
        public void run() {
            String message;
            try {
                while ((message = in.readLine()) != null) {
                    final String msg = message;
                    SwingUtilities.invokeLater(() -> processServerMessage(msg));
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> chatArea.append("Disconnected from server!\n"));
            }
        }
    }
}