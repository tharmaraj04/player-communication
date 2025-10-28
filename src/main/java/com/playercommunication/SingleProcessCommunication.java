package com.playercommunication;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.playercommunication.channel.MessageChannel;
import com.playercommunication.channel.SameProcessChannel;
import com.playercommunication.config.ConfigLoader;

/**
 * @author Tharmaraj Marimuthu
 * Threads for two players communicating in a single JVM process.
 * Uses BlockingQueue	 for inter-thread communication.
 * Achieve low-latency communication, bounded buffers.
 * Each player runs in its own thread, sending and receiving messages via shared queues.
 * BlockingQueue ensures thread-safe communication and Blocking operations.
 */
public class SingleProcessCommunication {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String initialMessage = args.length > 0 ? args[0] : null;

		if(initialMessage == null || initialMessage.isEmpty()) {
			initialMessage = "Chit_Chat"; // Default message
		}
		int queueCapacity = ConfigLoader.getQueueCapacity();

		System.out.println("=== Single Process Communication ===");
		System.out.println("Initial Message: " + initialMessage);
		System.out.println("Queue Capacity: " + queueCapacity);

		BlockingQueue<String> initiatorQueue = new ArrayBlockingQueue<>(queueCapacity);
		BlockingQueue<String> responderQueue = new ArrayBlockingQueue<>(queueCapacity);

		// Create channels for each player using the shared queues
		MessageChannel initiatorChannel = new SameProcessChannel(initiatorQueue, responderQueue);
		MessageChannel responderChannel = new SameProcessChannel(responderQueue, initiatorQueue);

		// Create Player instances for each player with their respective channels
		Player initiator = new Player("Player1", initiatorChannel, true, initialMessage);
		Player responder = new Player("Player2", responderChannel, false, null);

		// Create and start the initiator and responder threads
		// High priority thread for low latency
		Thread initiatorThread = new Thread(initiator, "Player1-Thread");
		Thread responderThread = new Thread(responder, "Player2-Thread");

		initiatorThread.start();
		responderThread.start();

		// Current thread sleeps to ensure Player2 thread sets up for shutdown
		try {
			initiatorThread.join();
			Thread.sleep(100);
			responderThread.join(1000);

			System.out.println("\n=== Completed ===");
		} catch (InterruptedException interrupExp) {
			interrupExp.printStackTrace();
		}

		System.out.println("=== Communication Finished ===");
		System.exit(0);
	}

}
