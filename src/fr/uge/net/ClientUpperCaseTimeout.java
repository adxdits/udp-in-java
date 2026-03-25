package fr.uge.net;

public class ClientUpperCaseUDPTimeout  {
 public static final int BUFFER_SIZE = 1024;

    private static void usage() {
        System.out.println("Usage : NetcatUDP host port charset");
    }

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

                        return;
                    }
                }
            });


    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            usage();
            return;
        }

        var server = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
        var cs = Charset.forName(args[2]);
        var buffer = ByteBuffer.allocate(BUFFER_SIZE);

        try (var scanner = new Scanner(System.in);
             var dc = DatagramChannel.open()) {
            dc.bind(null);
            while (scanner.hasNextLine()) {
                var line = scanner.nextLine();
                buffer.clear();
                buffer.put(cs.encode(line));
                buffer.flip();
                dc.send(buffer, server);

                buffer.clear();
                var sender = (InetSocketAddress) dc.receive(buffer);
                buffer.flip();

                // Display results
                System.out.println("Received " + buffer.remaining() + " bytes from " + sender);
                System.out.println("String: " + cs.decode(buffer));
            }
        }
    }
}
