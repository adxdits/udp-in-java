package fr.uge.net.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class testUdp {
    public static final int BUFFER_SIZE = 1024;

    private static void usage() {
        System.out.println("Usage : testUdp host port charset");
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            usage();
            return;
        }

        var server = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
        var cs = Charset.forName(args[2]);
        var queue = new ArrayBlockingQueue<String>(1);
        try (var dc = DatagramChannel.open()) {
            dc.bind(null);

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
                    sendBuffer.clear();
                    sendBuffer.put(cs.encode(line));
                    sendBuffer.flip();
                    dc.send(sendBuffer, server);
                    var res = queue.poll(1, TimeUnit.SECONDS);
                    if (res == null) {
                        System.out.println("Le serveur n'a pas répondu");
                    } else {
                        System.out.println("String: " + res);
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            listener.interrupt();
        }
    }
}