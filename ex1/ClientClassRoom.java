package fr.uge.net.udp.exam2324.ex1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;



public class ClientClassRoom {
	private static final Logger logger = Logger.getLogger(ClientClassRoom.class.getName());
	private final InetSocketAddress serverAddress;
	private final DatagramChannel datagramChannel;
	private final Charset UTF8 = StandardCharsets.UTF_8;
	private final String classRoom;
	private final ByteBuffer sendBuffer = ByteBuffer.allocate(1024);
	private final ByteBuffer recBuffer = ByteBuffer.allocate(2048);

	// NOTE: ADDED INDEX TO THE RECORD
	private record Student(String firstName, String lastName){
		private Student {
			Objects.requireNonNull(firstName);
			Objects.requireNonNull(lastName);
		}

		@Override
		public String toString() {
			return firstName + ' ' + lastName;
		}
	}
	
	private record StudentData(int index, int total, Student student) {}

	public ClientClassRoom(InetSocketAddress serverAddress, String classRoom) throws IOException {
		this.serverAddress = serverAddress;
		this.datagramChannel = DatagramChannel.open();
		this.classRoom = classRoom;
	}

	private void encodeMessage() {
		sendBuffer.clear(); 

		var encoded = UTF8.encode(classRoom);   
		sendBuffer.putInt(encoded.remaining()); 
		sendBuffer.put(encoded);             
	}

	private StudentData decodeMessage() {
		var index = recBuffer.getInt();
		var total = recBuffer.getInt();
		var nbOctet = recBuffer.getInt();
		var limit = recBuffer.limit();
		recBuffer.limit(recBuffer.position() + nbOctet);
		var prenom = UTF8.decode(recBuffer).toString();
		recBuffer.limit(limit);
		var nom = UTF8.decode(recBuffer).toString();
		var student = new Student(prenom,nom);
		var Data = new StudentData(index, total, student);
		return Data;
	}

	public List<Student> launch() throws IOException {
		this.datagramChannel.bind(null);
		List<Student> list = null;
		
		//send 
		this.encodeMessage();
		sendBuffer.flip();
		this.datagramChannel.send(sendBuffer, serverAddress);
		int received = 0;
		//receive
		while(true) {
			recBuffer.clear();
		this.datagramChannel.receive(recBuffer);
		recBuffer.flip();
		var data = this.decodeMessage();
		 if (list == null) {
	            list = new ArrayList<>(data.total());
	            for (int i = 0; i < data.total(); i++) {
	                list.add(null); // placeholder
	            }
	        }
		
		if (list.get(data.index())==null) {
			list.set(data.index(), data.student());
			received++;
		}
		if (received == list.size()) {
		 break;
		}
		}
		
		
		

		return list;
	}

	public static void usage() {
		System.out.println("Usage : ClientClassRoom host port classroom");
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			usage();
			return;
		}
		var server = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
		var classRoom = args[2];
		var students = new ClientClassRoom(server,classRoom).launch();
		var i=0;
		for(var student : students){
			System.out.println(i+". "+student.toString());
			i++;
		}
	}



}