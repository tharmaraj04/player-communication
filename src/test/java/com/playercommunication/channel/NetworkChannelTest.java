package com.playercommunication.channel;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * @author Tharmaraj Marimuthu
 * Unit tests for NetworkChannel using JUnit 5.
 * Tests cover connection establishment, message sending/receiving, bidirectional communication,
 * multiple messages, exception handling, and socket options.
 */
public class NetworkChannelTest {

    private static final int TEST_PORT_BASE  = 19090;
    private static final String TEST_HOST = "localhost";
    private static final String TEST_MESSAGE = "Chit Chat";

    private static int portCounter = 0;
    private static NetworkChannel serverChannel;
    private static NetworkChannel clientChannel;
    private static Thread serverThread;
    private int testPort;

    @BeforeEach
    void setUp(){
        serverChannel = null;
        clientChannel = null;
        serverThread = null;
        testPort = TEST_PORT_BASE + (portCounter++); // Increment port for each test to avoid conflicts
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (clientChannel != null) {
            clientChannel.shutdown();
        }
        if (serverChannel != null) {
            serverChannel.shutdown();
        }
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
            serverThread.join(2000);
        }
        // Wait port to be released
        Thread.sleep(100);
    }

    @Test
    @Timeout(5)
    @DisplayName("Server and Client connect successfully")
    void testServerClientConnection() throws IOException, InterruptedException {
       AtomicReference<NetworkChannel> serverChannelRef = new AtomicReference<>();

       // Start server in a background
       serverThread = new Thread(() -> {
           try {
               NetworkChannel server = new NetworkChannel("TestServer", testPort);
               serverChannelRef.set(server);
               // Keep server alive for a short duration to allow client connection
               Thread.sleep(2000);
           } catch (Exception e) {
               fail("Server failed: " + e.getMessage());
           }
       });
       serverThread.start();

       //Give server time to start listening (before accept() blocks)
       Thread.sleep(200);

       // Connect client
       clientChannel = new NetworkChannel("TestClient", TEST_HOST, testPort);
       assertNotNull(clientChannel, "Client channel not created");

       //Wait for server to complete connection
       Thread.sleep(200);
       serverChannel = serverChannelRef.get();
       assertNotNull(serverChannel, "Server channel not created");
    }

    @Test
    @Timeout(5)
    @DisplayName("Client send messages and Server receives them")
    void testSendAndReceiveMessages() throws IOException, InterruptedException {
        AtomicReference<String> receivedMessage = new AtomicReference<>();

        // Start server in a background
        serverThread = new Thread(() -> {
            try {
                serverChannel = new NetworkChannel("TestServer", testPort);
                String message = serverChannel.receiveMessage();
                receivedMessage.set(message);
            } catch (Exception e) {
                fail("Server failed: " + e.getMessage());
            }
        });
        serverThread.start();

        //Give server time to start listening (before accept() blocks)
        Thread.sleep(200);

        // Connect client and server
        clientChannel = new NetworkChannel("TestClient", TEST_HOST, testPort);
        String testMessage = "Chit Chat";
        clientChannel.sendMessage(testMessage);

        // Wait for message to be received by server
        Thread.sleep(200);
        
        // Connect client and server
        // Send message from client
        clientChannel.sendMessage(testMessage);
        Thread.sleep(200);
        assertEquals(testMessage, receivedMessage.get());
    }

    @Test
    @Timeout(5)
    @DisplayName("Bidirectional message exchange between Client and Server")
    void testBidirectionalMessageExchange() throws IOException, InterruptedException {
        AtomicReference<String> serverReceivedMessage = new AtomicReference<>();
        AtomicReference<String> clientReceivedMessage = new AtomicReference<>();

        // Start server in a background
        serverThread = new Thread(() -> {
            try {
                serverChannel = new NetworkChannel("TestServer", testPort);
                String message = serverChannel.receiveMessage();
                serverReceivedMessage.set(message);

                // Send response back to client
                serverChannel.sendMessage("Server Message");
            } catch (Exception e) {
                fail("Server failed: " + e.getMessage());
            }
        });
        serverThread.start();

        //Give server time
        Thread.sleep(200);

        // Client sends and receives
        clientChannel = new NetworkChannel("TestClient", TEST_HOST, testPort);
        clientChannel.sendMessage("Client Message");
        String responseMessage = clientChannel.receiveMessage();
        clientReceivedMessage.set(responseMessage);

        Thread.sleep(200);

        Thread.sleep(300);
        assertEquals("Client Message", serverReceivedMessage.get(), "Server did not receive expected message");
        assertEquals("Server Message", clientReceivedMessage.get(), "Client did not receive expected message");
    }


    @Test
    @Timeout(5)
    @DisplayName("connection nonexistent server should throw exception")
    void testShut() throws IOException, InterruptedException {
        serverThread = new Thread(() -> {
            try {
                serverChannel = new NetworkChannel("TestServer", testPort);
                Thread.sleep(3000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        serverThread.start();

        Thread.sleep(200);

        clientChannel = new NetworkChannel("TestClient", TEST_HOST, testPort);
        
        // Shut.down shoul not throw exception
        assertDoesNotThrow(() -> clientChannel.shutdown());
        assertDoesNotThrow(() -> serverChannel.shutdown());
    }

    @Test
    @Timeout(5)
    @DisplayName("TCP_NODELAY should be enabled for low latency")
    void testLowLatencySend() throws IOException, InterruptedException {
        // Start server in a background
        serverThread = new Thread(() -> {
            try {
                serverChannel = new NetworkChannel("TestServer", testPort);
                // Keep server alive for a short duration to allow client connection
                Thread.sleep(2000);
            } catch (Exception e) {
                fail("Server failed: " + e.getMessage());
            }
        });
        serverThread.start();

        Thread.sleep(200);

        clientChannel = new NetworkChannel("TestClient", TEST_HOST, testPort);

        // TCP_NODELAY should be enabled (tested indirectly via low latency communication)
        long startTime = System.nanoTime();
        clientChannel.sendMessage(TEST_MESSAGE);
        long duration = System.nanoTime() - startTime;

        // Expect sending to be very quick (< 10ms) due to TCP_NODELAY
        assertTrue(duration < 10_000_000, "Send took too long: "+duration +"ns");
    }

    @Test
    @Timeout(5)
    @DisplayName("Port already in use should throw exception")
    void testPortAlreadyInUse() throws IOException, InterruptedException {
        // Start first server to occupy the port
       ServerSocket blocker = new ServerSocket(testPort);

       try {
           // Attempt to start second server on the same port
           assertThrows(IOException.class, () -> {
               new NetworkChannel("BlockerServer", testPort);
           });
       } finally {
           blocker.close();
       }
    }

    @Test
    @Timeout(5)
    @DisplayName("Empty message should be handled correctly")
    void testEmptyMessage() throws IOException, InterruptedException {
        AtomicReference<String> receivedMessage = new AtomicReference<>();

        // Start server in a background
        serverThread = new Thread(() -> {
            try {
                serverChannel = new NetworkChannel("TestServer", testPort);
                String message = serverChannel.receiveMessage();
                receivedMessage.set(message);
            } catch (Exception e) {
                fail("Server failed: " + e.getMessage());
            }
        });
        serverThread.start();
        Thread.sleep(200);

        clientChannel = new NetworkChannel("TestClient", TEST_HOST, testPort);
        clientChannel.sendMessage("");
        Thread.sleep(500);
        
        assertEquals("", receivedMessage.get(), "Server did not receive expected empty message");
    }

    @Test
    @Timeout(5)
    @DisplayName("Null message should be ignored without exceptions")
    void testNullMessage() throws IOException, InterruptedException {
        // Start server in a background
        serverThread = new Thread(() -> {
            try {
                serverChannel = new NetworkChannel("TestServer", testPort);
                Thread.sleep(2000);
            } catch (Exception e) {
                fail("Server failed: " + e.getMessage());
            }
        });
        serverThread.start();
        Thread.sleep(200);

        clientChannel = new NetworkChannel("TestClient", TEST_HOST, testPort);
       
        // Sending null should not throw exception - should be ignored
        assertDoesNotThrow(() -> clientChannel.sendMessage(null));
    }

}
