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
			socket = new DatagramSocket(null); 

			// tell the OS to forcefully reuse this port even if it thinks it's busy
			socket.setReuseAddress(true); 

			// bind it to the port
			socket.bind(new java.net.InetSocketAddress(PORT));
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
		System.out.println("SERVER HEARD: " + message);
		//handle join
		if (message.startsWith("CONNECT")) {
			String playerName = message.split(" ")[1];

			// prevent duplicate connections from same person/port
			boolean alreadyExists = false;
			for(ClientConnection c : clients) {
				if(c.address.equals(address) && c.port == port) alreadyExists = true;
			}

			if (!alreadyExists && clients.size() < maxPlayers) {
				clients.add(new ClientConnection(address, port, playerName));
				System.out.println("[SERVER] " + playerName + " connected.");

				// --- DYNAMIC LOBBY UPDATE ---
				// announce the new roster every time someone joins, so the counter goes 1/4, 2/4, etc.
				List<String> names = new ArrayList<>();
				for (ClientConnection c : clients) {
					names.add(c.name);
				}
				String roster = String.join(",", names);

				broadcast("LOBBY_UPDATE " + maxPlayers + " " + roster);
				
				if (clients.size() == maxPlayers) {
					System.out.println("[SERVER] Lobby is full! Waiting for Host to start.");
				}
			}
		}
		// gameplay data
		else if (message.startsWith("PLAYER") || 
				message.startsWith("BOMB_PASS") || 
				message.startsWith("TIME") || 
				message.equals("ELIMINATE")||
				message.startsWith("SPAWN") ||
				message.startsWith("APPLY") ||
				message.startsWith("REMOVE")||
				message.startsWith("BREAK_SHIELD")||
				message.startsWith("FORCE_START")||
				message.startsWith("INTERMISSION") ||
				message.equals("RESTART_ROUND") ||
				message.startsWith("LOBBY_CHAT")) {

			broadcast(message);
		}
		//disconnects
		else if (message.startsWith("DISCONNECT")) {
			String leftName = message.split(" ")[1];
			
			// Find the specific ClientConnection object in your list
			ClientConnection leavingClient = null;
			for (ClientConnection c : clients) {
				if (c.name.equals(leftName)) {
					leavingClient = c;
					break;
				}
			}
			
			if (leavingClient != null) {
				// Remove the ghost from the Server's memory!
				clients.remove(leavingClient);
				
				System.out.println("[SERVER] " + leftName + " left the lobby.");
				
				// Rebuild the roster without them
				List<String> remainingNames = new ArrayList<>();
				for (ClientConnection c : clients) {
					remainingNames.add(c.name);
				}
				String roster = String.join(",", remainingNames);
				
				// Broadcast the updated roster to everyone still waiting
				broadcast("LOBBY_UPDATE " + maxPlayers + " " + roster);
			}
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