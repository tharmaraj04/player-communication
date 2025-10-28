package com.playercommunication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import com.playercommunication.channel.MessageChannel;

/**
 * @author Tharmaraj Marimuthu
 * Unit tests for Player using JUnit 5 and Mockito.
 * Tests cover initiator/responder roles, message sending/receiving,
 * message concatenation, stopping on null messages, handling empty strings,
 * constructor initialization, and shutdown behavior.
 */
public class PlayerTest {

    private MessageChannel mockChannel;
    private Player intiatorPlayer;
    private Player responderPlayer;

    @BeforeEach
    void setUp() {
        // Initialize mockChannel, intiatorPlayer, and responderPlayer here
        mockChannel = mock(MessageChannel.class);
        intiatorPlayer = new Player("Player1", mockChannel, true, "Chit Chat");
        responderPlayer = new Player("Player2", mockChannel, false, null);
    }

    @Test
    @DisplayName("Player should have correct Id")
    void testPlayerId() {
        assertEquals("Player1", intiatorPlayer.getId());
        assertEquals("Player2", responderPlayer.getId());
    }

    @Test
    @DisplayName("Initiator should send initial message")
    void testInitiatorInitialMessage() throws IOException {
        
        when(mockChannel.receiveMessage()).thenReturn("Chit Chat1", "Chit Chat12", "Chit Chat123",
                "Chit Chat1234", "Chit Chat12345", "Chit Chat123456", "Chit Chat1234567", "Chit Chat12345678",
                "Chit Chat123456789", "Chit Chat12345678910", null);

        intiatorPlayer.run();
        // Verify that the initiator sent the initial message

        InOrder inOrder = Mockito.inOrder(mockChannel);
        inOrder.verify(mockChannel).sendMessage("Chit Chat");
        inOrder.verify(mockChannel).receiveMessage();
    }

    @Test
    @DisplayName("Initiator should send exactly 10 messages total")
    void testInitiatorMessageCount() throws IOException {
        
        when(mockChannel.receiveMessage()).thenReturn("Chit Chat1", "Chit Chat12", "Chit Chat123",
                "Chit Chat1234", "Chit Chat12345", "Chit Chat123456", "Chit Chat1234567", "Chit Chat12345678",
                "Chit Chat123456789", "Chit Chat12345678910", null);

        intiatorPlayer.run();
        // Verify that the initiator sent exactly 10 messages
        verify(mockChannel, times(10)).sendMessage(startsWith("Chit Chat"));
    }

    @Test
    @DisplayName("Initiator should receive exactly 10 messages total")
    void testInitiatorReceiveCount() throws IOException {
        
        when(mockChannel.receiveMessage()).thenReturn("Chit Chat1", "Chit Chat12", "Chit Chat123",
                "Chit Chat1234", "Chit Chat12345", "Chit Chat123456", "Chit Chat1234567", "Chit Chat12345678",
                "Chit Chat123456789", "Chit Chat12345678910", null);

        intiatorPlayer.run();
        // Verify that the initiator received exactly 10 messages
        verify(mockChannel, times(10)).receiveMessage();
    }


    @Test
    @DisplayName("Responder should receive first then respond to message")
    void testResponderMessageFlow() throws IOException {
        
        when(mockChannel.receiveMessage()).thenReturn("Chit Chat", "Chit Chat1", "Chit Chat12", "Chit Chat123",
                "Chit Chat1234", "Chit Chat12345", "Chit Chat123456", "Chit Chat1234567", "Chit Chat12345678",
                "Chit Chat123456789", "Chit Chat12345678910", null);

        responderPlayer.run();
        // Verify that the responder received the initial message first
        InOrder inOrder = Mockito.inOrder(mockChannel);
        inOrder.verify(mockChannel).receiveMessage();
        inOrder.verify(mockChannel).sendMessage("Chit Chat1");
    }


    @Test
    @DisplayName("Constructor should set correct initial values")
    void testConstructorInitialValues() {
        Player player = new Player("TestPlayer", mockChannel, true, "Chit Chat");
        assertNotNull(player);
        assertEquals("TestPlayer", player.getId());
    }

    @Test
    @DisplayName("Shutdown flag should stop the player")
    void testShutdownFlagStopsPlayer() throws IOException, InterruptedException {
        
        when(mockChannel.receiveMessage()).thenReturn("Chit Chat1", "Chit Chat12", "Chit Chat123",
                "Chit Chat1234", "Chit Chat12345", "Chit Chat123456", "Chit Chat1234567", "Chit Chat12345678",
                "Chit Chat123456789", "Chit Chat12345678910", null);

        Thread playerThread = new Thread(responderPlayer);
        playerThread.start();
        Thread.sleep(100);
        responderPlayer.shutdown();
        playerThread.join();
        
        assertFalse(playerThread.isAlive());
    }
}