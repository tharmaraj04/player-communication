package com.playercommunication.channel;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * @author Tharmaraj Marimuthu
 * Unit tests for SameProcessChannel using JUnit 5.
 * Tests cover sending and receiving messages, order of messages,
 * blocking behavior, bidirectional communication, handling of edge cases,
 * and concurrent communication scenarios.
 */
public class SameProcessChannelTest {

    private BlockingQueue<String> incomingQueue;
    private BlockingQueue<String> outgoingQueue;
    private SameProcessChannel channel;

    @BeforeEach
    void setUp() {
        incomingQueue = new ArrayBlockingQueue<>(16);
        outgoingQueue = new ArrayBlockingQueue<>(16);
        channel = new SameProcessChannel(incomingQueue, outgoingQueue);
    }

    @Test
    @DisplayName("Send message should enqueue message to outgoing queue")
    void testSendMessage() {
        channel.sendMessage("Chit Chat");

        // Assert that the message is in the outgoing queue
        assertEquals(1, outgoingQueue.size());
        assertEquals("Chit Chat", outgoingQueue.poll());
    }

    @Test
    @DisplayName("Receive message should dequeue message from incoming queue")
    void testReceiveMessage() {
        incomingQueue.offer("Chit Chat");
        String message = channel.receiveMessage();

        // Assert that the message is in the incoming queue
        assertEquals("Chit Chat", message);
        assertTrue(incomingQueue.isEmpty());
    }

    @Test
    @DisplayName("Send multiple messages in order")
    void testSendMultipleMessages() {
        channel.sendMessage("1");
        channel.sendMessage("2");
        channel.sendMessage("3");

        //Assert messages are received in order
        assertEquals(3, outgoingQueue.size());
        assertEquals("1", outgoingQueue.poll());
        assertEquals("2", outgoingQueue.poll());
        assertEquals("3", outgoingQueue.poll());
    }

    @Test
    @DisplayName("Receive multiple messages in order")
    void testReceiveMultipleMessages() {
        incomingQueue.offer("1");
        incomingQueue.offer("2");
        incomingQueue.offer("3");

        // Assert messages are received in order
        assertEquals("1", channel.receiveMessage());
        assertEquals("2", channel.receiveMessage());
        assertEquals("3", channel.receiveMessage());
        assertTrue(incomingQueue.isEmpty());
    }

    @Test
    @DisplayName("Two channels can communicate bidirectionally")
    void testBidirectionalCommunication() throws InterruptedException {
        BlockingQueue<String> player1to2Queue = new ArrayBlockingQueue<>(16);
        BlockingQueue<String> player2to1Queue = new ArrayBlockingQueue<>(16);

        SameProcessChannel player1Channel = new SameProcessChannel(player2to1Queue, player1to2Queue);
        SameProcessChannel player2Channel = new SameProcessChannel(player1to2Queue, player2to1Queue);

        // Player A sends a message to Player B
        player1Channel.sendMessage("Chit Chat");
        assertEquals("Chit Chat", player2Channel.receiveMessage());

        // Player B responds to Player A
        player2Channel.sendMessage("Chit Chat");
        assertEquals("Chit Chat", player1Channel.receiveMessage());
    }

    @Test
    @DisplayName("Channel should handle empty string messages")
    void testEmptyStringMessage() {
        incomingQueue.offer("");
        channel.sendMessage("");
        
        assertEquals("", channel.receiveMessage());
        assertEquals("", outgoingQueue.poll());
    }

    @Test
    @DisplayName("Channel should ignore null - nothing is sent")
    void testIgnoreNullMessage() {
        channel.sendMessage(null);
        assertEquals(0, outgoingQueue.size());
    }

    @Test
    @DisplayName("Channel should handle long messages")
    void testLongMessage() {
        String longMessage = "A".repeat(1000);

        incomingQueue.offer(longMessage);
        channel.sendMessage(longMessage);
        assertEquals(longMessage, channel.receiveMessage());
        assertEquals(longMessage, outgoingQueue.poll());
    }

    @Test
    @DisplayName("receiveMessage on interrupted thread should return null")
    void testReceiveMessageOnInterruptedThread() throws InterruptedException {
        Thread thread = new Thread(() -> {
            Thread.currentThread().interrupt();
            assertNull(channel.receiveMessage());
        });
        thread.start();
        assertDoesNotThrow(() -> thread.join(1000));
    }

    @Test
    @DisplayName("sendMessage on interrupted thread should throw RuntimeException")
    void testSendMessageOnInterruptedThread() throws InterruptedException {
        BlockingQueue<String> testOutputQueue = new ArrayBlockingQueue<>(1);
        SameProcessChannel channel = new SameProcessChannel(incomingQueue, testOutputQueue);
        testOutputQueue.offer("FULL");
    
        Thread thread = new Thread(() -> {
            Thread.currentThread().interrupt();
            channel.sendMessage("Test");
            
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                channel.sendMessage("Test");
            });
            assertTrue(exception.getMessage().contains("interrupted"));
            });

        thread.start();
        assertDoesNotThrow(() -> thread.join(1000));
    }

    @Test
    @DisplayName("Concurrent send and receive messages")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testConcurrentCommunication() throws InterruptedException {
        int messageCount = 100;
        CountDownLatch sendDone = new CountDownLatch(1);
        CountDownLatch receiverDone = new CountDownLatch(1);

        Thread senderThread = new Thread(() -> {
            for (int i = 0; i < messageCount; i++) {
                incomingQueue.offer(String.valueOf(i));
                try {
                    Thread.sleep(1); // Simulate processing time
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            sendDone.countDown();
        });

        Thread receiverThread = new Thread(() -> {
            for (int i = 0; i < messageCount; i++) {
                String msg = channel.receiveMessage();
                assertNotNull(msg);
            }
            receiverDone.countDown();
        });

        senderThread.start();
        receiverThread.start();

        assertTrue(sendDone.await(3, TimeUnit.SECONDS));
        assertTrue(receiverDone.await(3, TimeUnit.SECONDS));
    }
}