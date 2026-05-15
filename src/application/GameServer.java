package application;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class GameServer {

	private DatagramSocket socket;
	private final int PORT = 4444;
	private boolean isRunning = false;
	// 1. Add the variable here
	private final int maxPlayers;

	// 2. Update constructor to require it
	public GameServer(int maxPlayers) {
		this.maxPlayers = maxPlayers;
		try {
			socket = new DatagramSocket(PORT);
			System.out.println("Pass The Bomb Server started on port " + PORT + " for " + maxPlayers + " players.");
		} catch (Exception e) {
			System.err.println("Could not bind to port " + PORT);
			e.printStackTrace();
		}
	}

	// Store connected clients
	private final List<ClientConnection> clients = new ArrayList<>();

	// Simple container to hold IP and Port for bouncing messages
	private static class ClientConnection {
		InetAddress address;
		int port;
		String name;

		ClientConnection(InetAddress address, int port, String name) {
			this.address = address;
			this.port = port;
			this.name = name;
		}
	}

	// Call this to begin listening in the background
	public void start() {
		if (socket == null) return;

		isRunning = true;
		Thread serverThread = new Thread(() -> {
			byte[] buffer = new byte[512]; // Buffer for incoming data

			while (isRunning) {
				try {
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
					socket.receive(packet);

					// Convert packet to text
					String message = new String(packet.getData(), 0, packet.getLength()).trim();
					processIncomingMessage(message, packet.getAddress(), packet.getPort());

				} catch (Exception e) {
					if (isRunning) e.printStackTrace();
				}
			}
		});

		// Make it a daemon so it dies automatically when you close the main game window
		serverThread.setDaemon(true); 
		serverThread.start();
	}

	private void processIncomingMessage(String message, InetAddress address, int port) {
		// 1. Handle New Connections
		if (message.startsWith("CONNECT")) {
			String playerName = message.split(" ")[1];

			// Prevent duplicate connections from same person/port
			boolean alreadyExists = false;
			for(ClientConnection c : clients) {
				if(c.address.equals(address) && c.port == port) alreadyExists = true;
			}

			if (!alreadyExists && clients.size() < maxPlayers) {
				clients.add(new ClientConnection(address, port, playerName));
				System.out.println("[SERVER] " + playerName + " connected.");

				// --- THE FIX IS HERE ---
				if (clients.size() == maxPlayers) {
					System.out.println("[SERVER] Lobby full. Broadcasting roster...");

					// 1. Create a comma-separated list of all names
					List<String> names = new ArrayList<>();
					for (ClientConnection c : clients) {
						names.add(c.name);
					}
					String roster = String.join(",", names);

					// 2. Broadcast "START Host,Alice,Bob"
					broadcast("START " + roster);
				}
			}
		} 
		// 2. Handle Gameplay Data
		else if (message.startsWith("PLAYER") || 
				message.startsWith("BOMB_PASS") || 
				message.startsWith("TIME") || 
				message.equals("ELIMINATE")) {

			broadcast(message);
		}
	}

	// Sends a message to all connected IP addresses
	private void broadcast(String message) {
		byte[] data = message.getBytes();
		for (ClientConnection client : clients) {
			try {
				DatagramPacket packet = new DatagramPacket(data, data.length, client.address, client.port);
				socket.send(packet);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// Safely shut down the server
	public void stop() {
		isRunning = false;
		if (socket != null && !socket.isClosed()) {
			socket.close();
			System.out.println("Server shut down.");
		}
	}
}