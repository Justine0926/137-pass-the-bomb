package application;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.function.Consumer;
import javafx.application.Platform;

public class GameClient {
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private Thread listenThread;
    private boolean isRunning = true;

    // The handler that processes incoming strings
    private Consumer<String> onMessageReceived;

    public GameClient(String serverIp, String playerName, Consumer<String> onMessageReceived) {
        this.onMessageReceived = onMessageReceived;
        try {
            socket = new DatagramSocket();
            serverAddress = InetAddress.getByName(serverIp);
            
            startListening();
            
            // Tell the server we arrived!
            send("CONNECT " + playerName);
            
        } catch (Exception e) {
            System.err.println("Could not connect to server at " + serverIp);
            e.printStackTrace();
        }
    }

    // --- THE MISSING METHOD ---
    // This allows Main.java to swap the listener from the Lobby to the GameBoard
    public void setOnMessageReceived(Consumer<String> onMessageReceived) {
        this.onMessageReceived = onMessageReceived;
    }

    public void send(String message) {
        if (!isRunning || socket == null) return;
        
        try {
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, 4444); 
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startListening() {
        listenThread = new Thread(() -> {
            byte[] buffer = new byte[512];
            
            while (isRunning) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    String message = new String(packet.getData(), 0, packet.getLength()).trim();
                    
                    Platform.runLater(() -> {
                        if (onMessageReceived != null) {
                            onMessageReceived.accept(message);
                        }
                    });
                    
                } catch (Exception e) {
                    if (isRunning) {
                        System.err.println("Error receiving packet from server:");
                        e.printStackTrace();
                    }
                }
            }
        });
        
        listenThread.setDaemon(true); 
        listenThread.start();
    }

    public void disconnect() {
        isRunning = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}