package fr.uge.net.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Scanner;

public class StoreWithByteOrder {

    private static void usage() {
        System.out.println("Usage: StoreWithByteOrder {LE|BE} filename");
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            usage();
            return;
        }

        var order = switch (args[0]) {
            case "LE" -> ByteOrder.LITTLE_ENDIAN;
            case "BE" -> ByteOrder.BIG_ENDIAN;
            default -> {
                usage();
                yield null;
            }
        };
        if (order == null) {
            return;
        }

        var path = Path.of(args[1]);
        var buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.order(order);

        try (var fc = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
             var scanner = new Scanner(System.in)) {
            while (scanner.hasNextLong()) {
                var value = scanner.nextLong();
                buffer.putLong(value);
                buffer.flip();
                while (buffer.hasRemaining()) {
                    fc.write(buffer);
                }
                buffer.clear();
            }
        }
    }
}
