package com.playercommunication;

import com.playercommunication.channel.MessageChannel;
import com.playercommunication.channel.NetworkChannel;
import com.playercommunication.config.ConfigLoader;
import java.lang.management.ManagementFactory;

/**
 * @author Tharmaraj Marimuthu
 * Player communication between separate JVM processes.
 * Network : TCP_NODELAY, small buffers, TOS=LOWDELAY for low latency.
 */
public class MultiProcessCommunication {

	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("Usage: java MultiProcessCommunication <playerId> <isInitiator> [initialMessage]");
			System.exit(1);
		}

		boolean isInitiator = Boolean.parseBoolean(args[1]);
		String playerId = args[0];
		String initialMessage = args.length > 2 ? args[2] : null;

		if (initialMessage == null || initialMessage.isEmpty()) {
			initialMessage = "Chit_Chat"; // Default message
		}

		int port = ConfigLoader.getNetworkPort();
		String host = ConfigLoader.getNetworkHost();

		// Display process info
		String jvmName = ManagementFactory.getRuntimeMXBean().getName();
		String pid = jvmName.split("@")[0];

		System.out.println("=== Multi Process Communication ===");
		System.out.printf("[%s] PID: %s%n", playerId, pid);
		System.out.printf("[%s] Role: %s%n", playerId, isInitiator ? "Initiator(Client)" : "Receiver(Server)");
		System.out.println("Transport: TCP/IP Socket (" + host + ":"+port+")");

		try{
			// Create network channel (client for initiator, server for responder)
			MessageChannel channel = isInitiator
					? new NetworkChannel(playerId,host, port)
					: new NetworkChannel(playerId, port);
			
			// Create Player instance
			Player player = new Player(playerId, channel, isInitiator, initialMessage);

			// Start communication
			player.communicate();

			// Cleanup resources
			channel.shutdown();
		}
		catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}
