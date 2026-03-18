package fr.uge.net.udp.exam2223.ex2;


import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.logging.Logger;

public class ServerChat {

    private static final Logger logger = Logger.getLogger(ServerChat.class.getName());
    private final DatagramChannel datagramChannel;
    private final int port;
   

    public ServerChat(int port) throws IOException {
        this.datagramChannel = DatagramChannel.open();
        this.port = port;
    }

    public String decodeMessage(ByteBuffer buffer){
        var length = buffer.getInt();
        var buff = buffer.slice().limit(length);
        buffer.position(buffer.position() + length); 
        return StandardCharsets.UTF_8.decode(buff).toString();
    }

    private ByteBuffer encodeString(String s) {
        var bytes = StandardCharsets.UTF_8.encode(s);
        var buf = ByteBuffer.allocate(4 + bytes.remaining());
        buf.putInt(bytes.remaining());
        buf.put(bytes);
        buf.flip();
        return buf;
    }

    private ByteBuffer encodeResponse(String loginSender, String message) {
        var senderBytes = StandardCharsets.UTF_8.encode(loginSender);
        var messageBytes = StandardCharsets.UTF_8.encode(message);
        var buf = ByteBuffer.allocate(4 + senderBytes.remaining() + 4 + messageBytes.remaining());
        buf.putInt(senderBytes.remaining());
        buf.put(senderBytes);
        buf.putInt(messageBytes.remaining());
        buf.put(messageBytes);
        buf.flip();
        return buf;
    }

    public void serve() throws IOException {
        datagramChannel.bind(new InetSocketAddress(port));
        System.out.println("ServerChat started on port " + port);

        // login -> address of its client
        var loginToAddress = new HashMap<String, InetSocketAddress>();
        // address -> login registered for that address
        var addressToLogin = new HashMap<InetSocketAddress, String>();

        var buffer = ByteBuffer.allocate(2048);

        while (!Thread.interrupted()) {
            buffer.clear();
            var senderAddress = (InetSocketAddress) datagramChannel.receive(buffer);
            buffer.flip();

            // Decode the three fields: login_sender, login_receiver, message
            String loginSender, loginReceiver, message;
            try {
                loginSender   = decodeMessage(buffer);
                loginReceiver = decodeMessage(buffer);
                message       = decodeMessage(buffer);
            } catch (Exception e) {
                logger.warning("Malformed packet from " + senderAddress + ", ignoring.");
                continue;
            }

            // Registration logic
            if (!addressToLogin.containsKey(senderAddress)) {
                // First packet from this address
                if (loginToAddress.containsKey(loginSender)) {
                    // Login already claimed by another address — ignore
                    continue;
                }
                addressToLogin.put(senderAddress, loginSender);
                loginToAddress.put(loginSender, senderAddress);
                logger.info("Registered '" + loginSender + "' from " + senderAddress);
            } else {
                // Already registered — reject if login_sender doesn't match
                if (!addressToLogin.get(senderAddress).equals(loginSender)) {
                    logger.warning("Login mismatch from " + senderAddress + ", ignoring.");
                    continue;
                }
            }

            // Forward to receiver if known
            var receiverAddress = loginToAddress.get(loginReceiver);
            if (receiverAddress == null) {
                logger.info("Unknown receiver '" + loginReceiver + "', ignoring.");
                continue;
            }

            // Send: login_sender (STRING) | message (STRING)
            var response = encodeResponse(loginSender, message);
            datagramChannel.send(response, receiverAddress);
            logger.info("Forwarded message from '" + loginSender + "' to '" + loginReceiver + "'");
        }
    }

    public static void usage() {
        System.out.println("Usage : ServerChat port");
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            usage();
            return;
        }
        int port = Integer.valueOf(args[0]);
        if (!(port >= 1024) & port <= 65535) {
            System.out.println("The port number must be between 1024 and 65535");
            return;
        }

        var server=new ServerChat(port);
        try {
            server.serve();
        } catch (BindException e) {
            System.err.println("Server could not bind on " + port + "\nAnother server is probably running on this port.");
            return;
        }
    }
}
