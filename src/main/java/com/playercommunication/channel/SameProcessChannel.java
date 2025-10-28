package com.playercommunication.channel;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Tharmaraj Marimuthu
 * SameProcessChannel for inter-thread communication within the same JVM process.
 * Uses BlockingQueue for thread-safe message passing with bounded buffers.
 * Implements sendMessage with timeout to avoid indefinite blocking.
 */
public class SameProcessChannel implements MessageChannel {

    private final BlockingQueue<String> incomingQueue;
    private final BlockingQueue<String> outgoingQueue;

    private static final int SEND_TIMEOUT_MS = 1000; // 1 second timeout for sending messages

    public SameProcessChannel(BlockingQueue<String> incomingQueue,
                              BlockingQueue<String> outgoingQueue) {
        this.incomingQueue = incomingQueue;
        this.outgoingQueue = outgoingQueue;
    }

    @Override
    public void sendMessage(String message) {
        if (message == null) return;

        try{
            boolean sent = outgoingQueue.offer(message, SEND_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!sent) {
                System.err.println("Failed to send message within timeout: " + message);
            }
        } catch (InterruptedException interrupExp) {
            interrupExp.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String receiveMessage() {
        try {
            return incomingQueue.take();
        } catch (InterruptedException interrupExp) {
            interrupExp.printStackTrace();
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Override
    public void shutdown() {
        // No specific resources to clean up in this implementation
    }

}
