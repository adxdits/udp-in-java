package fr.uge.net.udp.exam2223.ex1;



import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;


import static java.nio.file.StandardOpenOption.*;

public class ClientPokemon {

    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private static final Logger logger = Logger.getLogger(ClientPokemon.class.getName());
    private static final int MAX_SEND_SIZE = 1024;
    private static final int MAX_RECV_SIZE = 2048;

    private record Pokemon(String name, Map<String,Integer> characteristics){
        public Pokemon {
            Objects.requireNonNull(name);
            characteristics= Map.copyOf(characteristics);
        }

        @Override
        public String toString() {
            var stringBuilder = new StringBuilder();
            stringBuilder.append(name);
            for( var entry : characteristics.entrySet()){
                stringBuilder.append(';')
                        .append(entry.getKey())
                        .append(':')
                        .append(entry.getValue());
            }
            return stringBuilder.toString();
        }
    }

    private final String inFilename;
    private final String outFilename;
    private final InetSocketAddress server;
    private final DatagramChannel datagramChannel;
    private final ByteBuffer sendBuffer = ByteBuffer.allocate(MAX_SEND_SIZE);
    private final ByteBuffer recvBuffer = ByteBuffer.allocate(MAX_RECV_SIZE);

    public static void usage() {
        System.out.println("Usage : ClientPokemon in-filename out-filename host port ");
    }

    public ClientPokemon(String inFilename, String outFilename,
                         InetSocketAddress server) throws IOException {
        this.inFilename = Objects.requireNonNull(inFilename);
        this.outFilename = Objects.requireNonNull(outFilename);
        this.server = server;
        this.datagramChannel = DatagramChannel.open();
    }

    /**
     * Encode a Pokemon name into the send buffer according to the protocol:
     * INT (BigEndian) = number of UTF-8 bytes, followed by the UTF-8 bytes.
     * Returns true if the packet fits within MAX_SEND_SIZE, false otherwise.
     */
    private boolean encodeRequest(String pokemonName) {
        var encoded = UTF8.encode(pokemonName);
        var nameLength = encoded.remaining();
        // 4 bytes for the int + nameLength bytes for the name
        if (Integer.BYTES + nameLength > MAX_SEND_SIZE) {
            return false;
        }
        sendBuffer.clear();
        sendBuffer.putInt(nameLength);
        sendBuffer.put(encoded);
        return true;
    }

    /**
     * Decode the response buffer into a Pokemon record.
     * Format: name(UTF8) 0x00 [charName(UTF8) 0x00 INT(BigEndian)]*
     */
    private Pokemon decodeResponse(ByteBuffer buffer) {
        var name = readStringUntilZero(buffer);
        var characteristics = new LinkedHashMap<String, Integer>();
        while (buffer.hasRemaining()) {
            characteristics.put(readStringUntilZero(buffer), buffer.getInt());
        }
        return new Pokemon(name, characteristics);
    }

    private String readStringUntilZero(ByteBuffer buffer) {
        var bb = ByteBuffer.allocate(buffer.remaining());
        byte b;
        while ((b = buffer.get()) != 0) {
            bb.put(b);
        }
        bb.flip();
        return UTF8.decode(bb).toString();
    }

    public void launch() throws IOException, InterruptedException {
        try {
            datagramChannel.bind(null);
            // Read all lines of inFilename opened in UTF-8
            var pokemonNames = Files.readAllLines(Path.of(inFilename), UTF8);
            // List of Pokemon to write to the output file
            var pokemons = new ArrayList<Pokemon>();

            for (var pokemonName : pokemonNames) {
                // Encode the request; skip if it exceeds max packet size
                if (!encodeRequest(pokemonName)) {
                    logger.warning("Pokemon name too long, skipping: " + pokemonName);
                    continue;
                }

                // Send request
                sendBuffer.flip();
                datagramChannel.send(sendBuffer, server);

                // Receive response
                recvBuffer.clear();
                datagramChannel.receive(recvBuffer);
                recvBuffer.flip();

                // Decode response and add to the list
                var pokemon = decodeResponse(recvBuffer);
                pokemons.add(pokemon);
            }

            // Convert the pokemons to strings and write then in the output file
            var lines = pokemons.stream().map(Pokemon::toString).toList();
            Files.write(Paths.get(outFilename), lines , UTF8, CREATE, WRITE, TRUNCATE_EXISTING);
        } finally {
            datagramChannel.close();
        }
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length != 4) {
            usage();
            return;
        }

        var inFilename = args[0];
        var outFilename = args[1];
        var server = new InetSocketAddress(args[2], Integer.parseInt(args[3]));

        // Create client with the parameters and launch it
        new ClientPokemon(inFilename, outFilename, server).launch();
    }
}