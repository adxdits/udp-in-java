package fr.uge.net.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.util.Scanner;

public class NetcatUDP {
    public static final int BUFFER_SIZE = 1024;

    private static void usage() {
        System.out.println("Usage : NetcatUDP host port charset");
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            usage();
            return;
        }

        var server = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
        var cs = Charset.forName(args[2]);
        var buffer = ByteBuffer.allocate(BUFFER_SIZE);
        
        try (var scanner = new Scanner(System.in); DatagramChannel dc = DatagramChannel.open()) {
        	dc.bind(null);
            while (scanner.hasNextLine()) {
                var line = scanner.nextLine();
                buffer.clear();
                buffer.put(cs.encode(line));
                buffer.flip();
                dc.send(buffer, server);
                
                
                var address = dc.receive(buffer);
                buffer.flip();
                System.out.print("address: " + address + "buffer: " + buffer.remaining());
                
            }
        }
    }
}