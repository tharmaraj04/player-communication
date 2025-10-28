# Player Communication System

**Date of submission**: October 11, 2025 | **Status**:  Functional | **Tests**: 28/28 passing

Java implementation of message exchange between two players supporting same-process (BlockingQueue) and separate-process (TCP/IP) communication.

---


### Prerequisites: 
**Java**: Version - 21 
**Maven**: Version - 3.6+

## Overview

**Message Flow**: Player1 sends "1"  Player2 receives "1", sends "11"  Player1 receives "11", sends "112"  continues until 10 exchanges

**Logic**: `newMessage = receivedMessage + sentCount`  
**Stop**: Initiator stops after 10 sends/receives

## FEATURES

- **Player Communication**: Players communicated between them in the same and different process 
- **Message**: First Message - User can pass their custom message through the shell param while executing or else takes default message as `Chit_Chat`
- **Message Count**: We can decide how many times players can communicate. Values added in the properties file.
- **Responsibilities**: Every class has its own responsibilities
- **NetworkChannel**: Documented every class responsibilities using `JavaDoc`
- **SameProcessChannel**: Thread-safe `ArrayBlockingQueue` (capacity 16), timeout backpressure
- **NetworkChannel**: TCP/IP network connection using socket and socket server
- **Test cases**: 26 Test cases added
---

## Class Responsibilities

- **Player**: Core business logic - message loop, concatenation (`message + sentCount`), stop at 10
- **MessageChannel**: Interface - `sendMessage()`, `receiveMessage()`, `shutdown()`
- **SameProcessChannel**: Thread-safe `ArrayBlockingQueue` (capacity 16), timeout backpressure
- **NetworkChannel**: TCP/IP with optimizations (TCP_NODELAY, 8KB buffers, Keep-Alive, LOWDELAY QoS, newLine framing)
- **SingleProcessCommunication**: Same-JVM entry - creates queues, spawns threads
- **MultiProcessCommunication**: Separate-JVM entry - displays PID, creates server/client socket
- **ConfigLoader**: Loads `network.port`, `network.host`, `queue.capacity` from application.properties

---

## Quick Start

### Same-Process Mode
```bash

# Maven:
mvn compile exec:java -Dexec.mainClass="com.playercommunication.SingleProcessCommunication" -Dexec.args=$message""
```

### Separate-Process Mode
```bash
# Maven - Terminal 1 (Server):
mvn compile exec:java -Dexec.mainClass="com.playercommunication.MultiProcessCommunication" -Dexec.args="Player2 false $message"
# Maven - Terminal 2 (Client):
mvn compile exec:java -Dexec.mainClass="com.playercommunication.MultiProcessCommunication" -Dexec.args="Player1 true $message"
```

### Custom Initial Message
```bash
# Default message is "1" if not provided
./start_same_process_communication.sh            # Uses default "Chit Chat"
./start_same_process_communication.sh "5"        # Uses "5"
./start_different_process_communication.sh       # Uses default "Chit Chat"
./start_different_process_communication.sh "ABC" # Uses "ABC"
```

---

## Architecture

```
src/main/java/com/playercommunication/
 Player.java         # Core logic (concatenation, stop condition)
 SingleProcessCommunication.java  # Same-process entry (Requirement 5)
 MultiProcessCommunication.java   # Separate-process entry (Requirement 7)
 channel/
    MessageChannel.java          # Communication interface
    SameProcessChannel.java      # BlockingQueue implementation
    NetworkChannel.java          # TCP/IP socket with optimizations
 config/
     ConfigLoader.java            # Loads application.properties
```

## Configuration

**application.properties**:
```properties
network.port=9090
network.host=localhost
queue.capacity=16
message.count.max=10
```

---

## Testing

```bash
mvn test                                        # Run all 28 tests
mvn test -Dtest=PlayerTest                      # 8 tests
mvn test -Dtest=SameProcessChannelTest          # 11 tests
mvn test -Dtest=NetworkChannelTest              # 9 tests
```

**Coverage**: Message concatenation, stop condition, order verification, null handling, thread safety, network reliability

---

## Technical Details

**Concurrency**: `BlockingQueue`, `volatile` no shared mutable state  
**Network**: TCP_NODELAY, 8KB buffers, LOWDELAY QoS, socket reuse 

---

## Troubleshooting

**Port in use**: Change port in `application.properties` or wait 60-120 seconds  
**Connection refused**: Start server (Player2) before client (Player1)

---

**Version**: 1.0.0 | **Java**: 21 | **Maven**: 3.6+ | **Author**: Tharmaraj Marimuthu
