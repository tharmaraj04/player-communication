package com.playercommunication;

import com.playercommunication.channel.MessageChannel;
import com.playercommunication.config.ConfigLoader;

/**
 * @author Tharmaraj Marimuthu
 * Represents a player in the communication system.
 * Each player can send and receive messages via a MessageChannel.
 * Initiator starts the communication by sending the first message.
 * Responder waits to receive messages and responds accordingly.
 * Message format : Actual message + Message count
 */

public class Player implements Runnable {

    private final String playerId;
    private final MessageChannel channel;
    private final boolean isInitiator;
    private final String initialMessage;

    private static final int MAX_MESSAGES = ConfigLoader.getMaxMessageCount();
    private int sentCount = 0;
    private int receivedCount = 0;


    /**
     * @param playerId Unique identifier for the player
     * @param channel Communication channel for sending/receiving messages
     * @param isInitiator True if this player initiates the communication
     * @param initialMessage The initial message to start the communication with
     */
    public Player(String playerId, MessageChannel channel, boolean isInitiator, String initialMessage) {
        this.playerId = playerId;
        this.channel = channel;
        this.isInitiator = isInitiator;
        this.initialMessage = initialMessage;
    }

    /**
     * Initiator logic : Sends the initial message and continues sending messages until send and receive counts reach MAX_MESSAGES.
     * Responder logic : Waits to receive messages and responds until send and receive counts reach MAX_MESSAGES.
     * Both players check the shutdownFlag to terminate gracefully.
     * @see Runnable#run()
     */
    @Override
    public void run() {
        communicate();
    }

    /**
     * Get Player's Id
     * @return the Player Id
     */
    public String getId(){
        return playerId;
    }

    public void communicate(){

        try {
            System.out.println(String.format("[%s] Started. (Initiator=%s)", playerId, isInitiator));

            // Initiator sends the first message
            if (isInitiator) {
                sentCount++;
                channel.sendMessage(initialMessage);
                System.out.println(String.format("[%s] Sent: %s (Sent Count: %d)", playerId, initialMessage, sentCount));
            }

            //Message exchange loop
            while ((sentCount <= MAX_MESSAGES) && ( receivedCount <= MAX_MESSAGES ) ) {
                // Receive message
                String receivedMessage = channel.receiveMessage();
                receivedCount++;
                System.out.println(String.format("[%s] Received: %s (Received Count: %d)", playerId, receivedMessage, receivedCount));

                if (receivedMessage == null || (isInitiator && receivedCount >= MAX_MESSAGES)) {
                    break;
                }
                sentCount++;
                channel.sendMessage(receivedMessage + sentCount);
                System.out.println(String.format("[%s] Sent: %s (Sent Count: %d)", playerId, receivedMessage + sentCount, sentCount));
                Thread.sleep(100);
            }

            System.out.println(String.format("[%s] Completed (Sent: %d, Received: %d)", playerId, sentCount, receivedCount));
        } catch (Exception e) {
            System.out.println(String.format("[%s] Stopped with exception: %s (sent=%d, received=%d)", playerId, e.getMessage(), sentCount, receivedCount));
        }
    }

}
