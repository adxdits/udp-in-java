package fr.uge.net.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import java.util.logging.Logger;

public class NetcatUDP {
    private static final Logger logger = Logger.getLogger(NetcatUDP.class.getName());
    public static final int BUFFER_SIZE = 1024;
    private static final int TIMEOUT_MS = 300;

    private static void usage() {
        System.out.println("Usage : NetcatUDP host port charset");
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length != 3) {
            usage();
            return;
        }

        var server = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
        var cs = Charset.forName(args[2]);
        var queue = new ArrayBlockingQueue<String>(1);

        try (var scanner = new Scanner(System.in);
             var dc = DatagramChannel.open()) {
            dc.bind(null);

            // Thread listener : reçoit les réponses et les met dans la queue
            var listener = Thread.ofPlatform().daemon().start(() -> {
                var recBuff = ByteBuffer.allocate(BUFFER_SIZE);
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        recBuff.clear();
                        var sender = (InetSocketAddress) dc.receive(recBuff);
                        recBuff.flip();
                        var msg = cs.decode(recBuff).toString();
                        logger.info("Received from " + sender + ": " + msg);
                        queue.put(msg);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    } catch (IOException e) {
                        logger.severe("IOException in listener: " + e.getMessage());
                        return;
                    }
                }
            });

            // Thread principal : lit stdin, envoie, attend la réponse avec retry
            var sendBuff = ByteBuffer.allocate(BUFFER_SIZE);
            while (scanner.hasNextLine()) {
                var line = scanner.nextLine();
                sendBuff.clear();
                sendBuff.put(cs.encode(line));
                sendBuff.flip();
                dc.send(sendBuff, server);

                // Attendre la réponse, renvoyer si timeout
                String response;
                if ((response = queue.poll(TIMEOUT_MS, MILLISECONDS)) == null) {
                    logger.warning("No response, resending...");
                    sendBuff.rewind();
                    dc.send(sendBuff, server);
                }
                logger.info("String: " + response);
            }
        }
    }
}