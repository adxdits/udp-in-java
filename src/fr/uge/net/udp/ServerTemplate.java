package fr.uge.net.udp.examXXXX.exY;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class ServerTemplate {

    private static final Logger logger = Logger.getLogger(ServerTemplate.class.getName());
    private static final int BUFFER_SIZE = 2048; // adjust to max packet size in the protocol

    private final DatagramChannel datagramChannel;
    private final int port;

    // --- State (per-client or global, depends on the protocol) ---
    // e.g. HashMap<InetSocketAddress, ClientStats> perClientState = new HashMap<>();
    // e.g. Set<Integer> globalSeen = new HashSet<>();

    public ServerTemplate(int port) throws IOException {
        this.datagramChannel = DatagramChannel.open();
        this.port = port;
    }

    // --- Decode helpers ---
    // Decode a UTF-8 STRING (INT length + UTF-8 bytes) from buffer, advances position
    private String decodeString(ByteBuffer buffer) {
        var length = buffer.getInt();
        var slice = buffer.slice().limit(length);
        buffer.position(buffer.position() + length);
        return StandardCharsets.UTF_8.decode((ByteBuffer) slice).toString();
    }

    // --- Encode helpers ---
    // Encode a UTF-8 STRING (INT length + UTF-8 bytes) into a new buffer, ready to read
    private ByteBuffer encodeString(String s) {
        var bytes = StandardCharsets.UTF_8.encode(s);
        var buf = ByteBuffer.allocate(4 + bytes.remaining());
        buf.putInt(bytes.remaining());
        buf.put(bytes);
        buf.flip();
        return buf;
    }

    // --- Core logic ---
    // Called once per valid received packet.
    // senderAddress: who sent it
    // request:       the received buffer (flipped, ready to read)
    // Returns:       the response buffer (ready to read), or null to send no response
    private ByteBuffer process(InetSocketAddress senderAddress, ByteBuffer request) {
        // 1. DECODE the request fields
        // e.g. int value = request.getInt();
        // e.g. String login = decodeString(request);

        // 2. UPDATE state
        // e.g. update per-client min/max/sum/count
        // e.g. update global distinct set

        // 3. ENCODE the response
        // e.g. ByteBuffer response = ByteBuffer.allocate(...);
        // e.g. response.putInt(...).putLong(...).flip();
        // return response;

        return null; // replace with actual response
    }

    public void serve() throws IOException {
        datagramChannel.bind(new InetSocketAddress(port));
        logger.info("Server started on port " + port);

        var buffer = ByteBuffer.allocate(BUFFER_SIZE);

        while (!Thread.interrupted()) {
            buffer.clear();
            var senderAddress = (InetSocketAddress) datagramChannel.receive(buffer);
            buffer.flip();

            // Optional: validate packet size / content before processing
            var response = process(senderAddress, buffer);

            if (response != null) {
                datagramChannel.send(response, senderAddress);
            }
        }
    }

    public static void usage() {
        System.out.println("Usage: ServerTemplate port");
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            usage();
            return;
        }
        int port = Integer.parseInt(args[0]);
        if (port < 1024 || port > 65535) {
            System.out.println("Port must be between 1024 and 65535");
            return;
        }
        var server = new ServerTemplate(port);
        try {
            server.serve();
        } catch (BindException e) {
            System.err.println("Could not bind on port " + port + ". Another server is probably running.");
        }
    }
}
