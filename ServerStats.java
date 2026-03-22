

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ServerStats {
    private static final Logger logger = Logger.getLogger(ServerStats.class.getName());
    private final DatagramChannel datagramChannel;
    private final int port;
    private final List<Integer> numbers = new ArrayList<>();
    private final ByteBuffer buffer = ByteBuffer.allocate(1024);
    private final ByteBuffer sendbuffer = ByteBuffer.allocate(1024);

    public ServerStats(int port) throws IOException {
        this.datagramChannel = DatagramChannel.open();
        this.port = port;
    }

    private void encodeMessage(int number, int max , long average, long diff) {
    	sendbuffer.clear();
    	sendbuffer.putInt(number);
    	sendbuffer.putInt(max);
    	sendbuffer.putLong(average);
    	sendbuffer.putLong(diff);
    }
    
    public void serve() throws IOException {
        datagramChannel.bind(new InetSocketAddress(port));
        System.out.println("ServerStats started on port " + port);
        while(true) {
        	buffer.clear();
        	var data = datagramChannel.receive(buffer);
        	buffer.flip();
        	int numb = buffer.getInt();
        	numbers.add(numb);
        	int min = numbers.stream().max(Integer::compareTo).orElse(0);
        	int max = numbers.stream().max(Integer::compareTo).orElse(0);
        	long average = (long) numbers.stream().mapToInt(t->t).average().orElse(numb);

        	long diff = numbers.stream()
        	        .filter(num -> num == numb)
        	        .count();

        	encodeMessage(min, max, average, diff);
        	sendbuffer.flip();
        	datagramChannel.send(sendbuffer, data);
        }
        
        
        
    }

    public static void usage() {
        System.out.println("Usage : ServerStats port");
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

        var server = new ServerStats(port);
        try {
            server.serve();
        } catch (BindException e) {
            System.err.println("Server could not bind on " + port + "\nAnother server is probably running on this port.");
            return;
        }
    }
}