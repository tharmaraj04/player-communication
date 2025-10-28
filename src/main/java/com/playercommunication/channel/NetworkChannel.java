package com.playercommunication.channel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * @author Tharmaraj Marimuthu
 * NetworkChannel for inter-process communication using TCP sockets.
 * Configured for low latency with TCP_NODELAY, small buffers, and TOS=LOWDELAY.
 */
public class NetworkChannel implements MessageChannel{

    private Socket socket;
    private BufferedReader messageReader;
    private BufferedWriter messageWriter;
    private ServerSocket serverSocket;

    // Constructor for server (responder)
    public NetworkChannel(String playerId, int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.serverSocket.setReuseAddress(true);
        this.serverSocket.setReceiveBufferSize(8 * 1024);

        this.socket = serverSocket.accept();
        
        configureSocket();
        initializeStreams();
    }

    // Constructor for client (initiator)
    public NetworkChannel(String playerId, String host, int port) throws IOException {
        try {
            Thread.sleep(1000); // Brief pause to ensure server is ready
            this.socket = new Socket(host, port);
        } catch (ConnectException connExp) {
            retryConncetion(host, port);
        } catch (InterruptedException interrupExp) {
            Thread.currentThread().interrupt();
        }

        configureSocket();
        initializeStreams();
    }

    // Retry if can't get connection
    private void retryConncetion(String host, int port) throws IOException {
        int maxRetries = 5;
        int retryDelayMs = 2000;
        System.out.println("Socket connection failed - Retry for connection...");
       for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Thread.sleep(retryDelayMs);
                this.socket = new Socket(host, port);
                System.out.println("Socket connection successfully created - After attempt : "+attempt);
            } catch (ConnectException connExp) {
                System.out.println("Connection attempt " + attempt + " failed, retrying in " + retryDelayMs + "ms...");
                connExp.printStackTrace();
                if (attempt == maxRetries) {
                    throw connExp;
                }
            } catch (InterruptedException interrupExp) {
                interrupExp.printStackTrace();
            }
        }
    }

    // Configure socket options for low latency
    private void configureSocket() throws SocketException {
        socket.setTcpNoDelay(true); // Disable Nagle's algorithm for low latency
        socket.setSendBufferSize(8 * 1024); // Small send buffer
        socket.setReceiveBufferSize(8 * 1024); // Small receive buffer
        socket.setTrafficClass(0x10); // Set TOS to LOWDELAY
        socket.setKeepAlive(true); // Enable TCP keep-alive
    }

    private void initializeStreams() throws IOException {
        this.messageReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.messageWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    @Override
    public void sendMessage(String message) throws IOException {
        if (message == null) return;

        try{
            messageWriter.write(message);
            messageWriter.newLine();
            messageWriter.flush();
        } catch (IOException ioExp) {
            ioExp.printStackTrace();
        }
    }

    @Override
    public String receiveMessage(){
        try{
            return messageReader.readLine();
        } catch (IOException ioExp) {
            ioExp.printStackTrace();
            return null;
        }
    }

    @Override
    public void shutdown() {
        try {
            if (messageReader != null) messageReader.close();
            if (messageWriter != null) messageWriter.close();
            if (socket != null && !socket.isClosed()) socket.close();
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException ioExp) {
            ioExp.printStackTrace();
        }
    }
}
