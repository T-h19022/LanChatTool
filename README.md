# LanChatTool  SE-1B 250429970


## Features
- Core Functions: Group chat (broadcasting messages to all members) and private chat (one - to - one communication with a specified user)
- Auxiliary Functions: Real - time synchronization of online users and system notifications for users going online or offline
- Basic Features: A simple and visual UI, verification of unique usernames, and standardized message formats
- Environment Compatibility: Supports JDK 8 and above, and is compatible with Windows, Mac, and Linux systems

## Environment Preparation
### 1. Required Environment
- JDK Version: JDK 8 or above (JDK 8 is recommended for the best compatibility)
- Development/Running Tools: IntelliJ IDEA, Eclipse, or NetBeans (any tool that supports Java compilation and running)
- Network Requirements: All test devices must be connected to the same local area network (such as the same WiFi or router)

### 2. Environment Verification
Open the terminal (Windows Command Prompt / Mac/Linux Terminal) and enter the following commands to verify the JDK installation:
```bash
java -version  # Normal output is similar to "java version "1.8.0_301""
javac -version  # Normal output is similar to "javac 1.8.0_301""
```

## Project Structure
```
LanChatTool/
└─ src/
   ├─ ChatServer.java       # Server main class (listens on ports, manages connections and users)
   ├─ ClientHandlerThread.java  # Server thread class (handles message forwarding for a single client)
   ├─ ChatClient.java       # Client main class (starts the client application)
   ├─ ClientUI.java         # Client UI class (chat interface and interaction logic)
   └─ MessageType.java      # Message type constant class (unifies formats for group chat, private chat, and notifications)
```

## Quick Start Steps
### 1. Server Startup
1. Place the 5 core `.java` files into the `src/main/java` directory (the IDE will automatically recognize the source code)
2. Compile all Java files (the IDE can automatically complete this through "Clean and Build")
3. Run the main class `ChatServer.java`
4. After successful startup, the terminal will output the server IP (for client connection) and the listening port (default is 8888) 

### 2. Client Startup
1. Run the main class `ChatClient.java` and a login window will pop up
2. Enter a username (it cannot be empty or duplicated) and click Confirm
3. Enter the server IP (copied from the server terminal output) and click Confirm
4. After a successful connection, you can enter the chat interface and switch between group chat and private chat 

## Usage Instructions
### Group Chat Operation
1. Select "Group Chat" from the chat type drop - down box
2. Enter a message in the input box and click "Send" or press the Enter key
3. All online users can receive the group chat message

### Private Chat Operation
1. Select "Private Chat" from the chat type drop - down box
2. Click to select a private chat target from the online user list on the left
3. Enter a message and send it; only the selected user can receive the message

### Notes
- Usernames cannot be duplicated. If you receive a prompt that "the username is already in use", you need to re - enter a username 
- When connecting the client, you must enter the correct server IP; otherwise, the connection cannot be established
- If the server is restarted, all clients need to reconnect
- Close the window to exit the program, and the system will automatically broadcast an offline notification 

## Troubleshooting Common Problems
1. Server Startup Failure: Check if port 8888 is occupied (you can modify the `SERVER_PORT` constant in `ChatServer.java` to change the port) 
2. Client Connection Failure: Confirm that the server has been started, the devices are in the same local area network, and the server IP is entered correctly
3. Message Sending Failure: Check the network connection and confirm that the recipient is not offline
4. Compilation Error "Main Class Not Found": Ensure that the source code directory structure is `src/main/java` and that there are no syntax errors in all files

## Source Code Description
- The message format uses "type|parameter" separation (for example, a group chat message: `[GROUP]|username|message content`) 
- The server uses multi - threading to achieve concurrent processing of multiple clients, ensuring thread safety
- The UI is developed using Swing components and supports features such as automatic line wrapping, window centering, and fixed size 
- All IO streams have closed logic to avoid resource leaks
