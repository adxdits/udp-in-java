package fr.uge.net.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;





/**
 * Le listener (thread en arrière-plan) : il attend les réponses du serveur.
 * Quand une réponse arrive, il la dépose dans la file (queue.put(response)).
 *
 * Le sender (thread principal) : après avoir envoyé une requête, il va
 * regarder dans la file s'il y a une réponse.
 *
 * queue.poll(1, TimeUnit.SECONDS) c'est comme dire :
 * "Je regarde dans la boîte. S'il y a quelque chose dedans, je le prends.
 *  Sinon, j'attends 1 seconde max. Si après 1 seconde il n'y a toujours
 *  rien, je repars les mains vides (null)."
 */
public class ClientUpperCaseUDPTimeout {
    public static final int BUFFER_SIZE = 1024;
    private static final int TIMEOUT_SECONDS = 1;

    private static void usage() {
        System.out.println("Usage : ClientUpperCaseUDPTimeout host port charset");
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length != 3) {
            usage();
            return;
        }

        var server = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
        var cs = Charset.forName(args[2]);
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(1);

        try (var dc = DatagramChannel.open()) {
            dc.bind(null);

            // Listener thread: reads responses from the channel and enqueues them
            var listener = Thread.ofPlatform().start(() -> {
                var buf = ByteBuffer.allocate(BUFFER_SIZE);
                while (!Thread.interrupted()) {
                    buf.clear();
                    try {
                        dc.receive(buf);
                        buf.flip();
                        var response = cs.decode(buf).toString();
                        queue.put(response);
                    } catch (IOException e) {
                        // Channel closed, exit loop
                        return;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            });

            var sendBuffer = ByteBuffer.allocate(BUFFER_SIZE);
            try (var scanner = new Scanner(System.in)) {
                while (scanner.hasNextLine()) {
                    var line = scanner.nextLine();

                    // Encode and send the line to the server
                    sendBuffer.clear();
                    sendBuffer.put(cs.encode(line));
                    sendBuffer.flip();
                    dc.send(sendBuffer, server);
                    
                    // Wait up to TIMEOUT_SECONDS for a response
                    var response = queue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    if (response == null) {
                        System.out.println("Le serveur n'a pas répondu");
                    } else {
                        System.out.println("String: " + response);
                    }
                }
            }

            listener.interrupt();
        }
    }
}
