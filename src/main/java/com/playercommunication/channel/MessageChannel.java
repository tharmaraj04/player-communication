package com.playercommunication.channel;

import java.io.IOException;

/**
 * @author Tharmaraj Marimuthu
 * MessageChannel interface for sending and receiving messages between players.
 * Implementations can vary based on communication method ( Same process, Separate process).
 */
public interface MessageChannel {

    void sendMessage(String string) throws IOException;

    String receiveMessage();

    void shutdown();

}
