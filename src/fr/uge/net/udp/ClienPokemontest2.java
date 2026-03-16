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
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;


import static java.nio.file.StandardOpenOption.*;

public class ClientPokemontest2 {

    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private static final Logger logger = Logger.getLogger(ClientPokemon.class.getName());

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
    private final ByteBuffer bsent = ByteBuffer.allocate(1024);

    public static void usage() {
        System.out.println("Usage : ClientPokemon in-filename out-filename host port ");
    }


    private String retrieveUntilZero(ByteBuffer buffer){
        byte b;
        ByteBuffer temp = ByteBuffer.allocate(1024);
        while(buffer.hasRemaining()){
            b = buffer.get();
            if(b != 0){
                temp.put(b);
            }
        }
        temp.flip();
        return UTF8.decode(temp).toString();
    }

    public ClientPokemon(String inFilename, String outFilename,
                         InetSocketAddress server) throws IOException {
        this.inFilename = Objects.requireNonNull(inFilename);
        this.outFilename = Objects.requireNonNull(outFilename);
        this.server = server;
        this.datagramChannel = DatagramChannel.open();
    }


    public void launch() throws IOException, InterruptedException {
        try {
            datagramChannel.bind(null);
            var pokemonNames = Files.readAllLines(Path.of(inFilename), UTF8);
            var pokemons = new ArrayList<Pokemon>();
            
            for(var pokemon : pokemonNames){
                bsent.clear();
                var cs = UTF8.encode(pokemon);
                var length = cs.remaining();
                bsent.putInt(length);
                bsent.put(cs);
                bsent.flip();
                datagramChannel.send(bsent, server);


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
