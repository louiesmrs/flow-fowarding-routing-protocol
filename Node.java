
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;


// Author: Louie Somers

public abstract class Node {
	static final int PACKETSIZE = 65000;
	static final byte MESSAGE = 1;
	static final byte REPLY = 2;
	static final byte BROADCAST = 3;
	static final byte REMOVE = 4;
	static final int ROUTER_PORT = 51000;
	static final String DEFAULT_DST_NODE = "localhost";
	static final int NUMBEROFENDPOINTS = 4;
	static final int IDSIZE = 4;
	public static Map<String, String> routerAddresses;
	public static Map<String, String> endpointAddresses;
	static {
		routerAddresses = new HashMap<>();
		endpointAddresses = new HashMap<>();
		routerAddresses.put("1", "172.20.17.255,172.20.20.255");
		routerAddresses.put("2", "172.20.17.255,172.20.19.255");
		routerAddresses.put("3", "172.20.19.255,172.20.18.255,172.20.1.255");
		endpointAddresses.put("1", "172.20.20.255");
		endpointAddresses.put("2", "172.20.18.255");
		endpointAddresses.put("3", "172.20.20.255");
		endpointAddresses.put("4", "172.20.1.255");


	}


	DatagramSocket socket;
	Listener listener;
	CountDownLatch latch;

	Node() {
		latch= new CountDownLatch(1);
		listener= new Listener();
		listener.setDaemon(true);
		listener.start();
	}


	protected byte[] createHeader(byte packet_type, byte[] dest_endpoint_id, byte[] source_endpoint_id, byte currentRouter, String msg) {
		byte[] data = new byte[PACKETSIZE];
		data[0] =  packet_type;
		for (int i = 0; i < 4; i++) {
			data[i + 1] = dest_endpoint_id[i];
		}
		for (int i = 0; i < 4; i++) {
			data[i + 5] = source_endpoint_id[i];
		}
		data[9] = currentRouter;
		byte[] msg_array = msg.getBytes();
		for (int i = 0; i < msg_array.length && i < PACKETSIZE; i++) {
			data[i + 10] = msg_array[i];
		}
		return data;
	}

		protected byte[] createBroadcastHeader(byte packet_type, byte[] dest_endpoint_id, byte currentRouter) {
		byte[] data = new byte[PACKETSIZE];
		data[0] =  packet_type;
		for (int i = 0; i < 4; i++) {
			data[i + 1] = dest_endpoint_id[i];
		}
		data[9] = currentRouter;
		return data;
	}

	protected void setCurrentRouter(byte[] data, byte n) {
		data[9] = n;
	}

	protected String getCurrentRouter(byte[] data) {
		return String.format("%X",data[9]);
	}
	
	protected int getType(byte[] data) {
		return data[0];
	}
	protected void setType(byte[] data, byte type) {
		data[0] = type;
	}


	protected int hex2decimal(String s) {
		String digits = "0123456789ABCDEF";
		s = s.toUpperCase();
		int val = 0;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			int d = digits.indexOf(c);
			val = 16*val + d;
		}
		return val;
	}

	protected String getDest_id(byte[] data) {
		StringBuffer id =new StringBuffer();
		for (int i = 0; i < 4; i++) {
			id.append(String.format("%02X", data[i + 1]));
		}
		return id.toString();
	}

	protected String getSource_id(byte[] data) {
		StringBuffer id =new StringBuffer();
		for (int i = 0; i < 4; i++) {
			id.append(String.format("%02X", data[i + 5]));
		}
		return id.toString();
	}

	protected byte[] getByteSource(byte[] data) {
		byte[] id = new byte[4];
		for (int i = 0; i < 4; i++) {
			id[i] = data[i + 5];
		}
		return id;
	}

	protected String getStringFromByte(byte[] data) {
		StringBuffer id =new StringBuffer();
		for (int i = 0; i < 4; i++) {
			id.append(String.format("%02X", data[i]));
		}
		return id.toString();
	}

	protected String getMessage(byte[] data) {
		byte[] messageArray = new byte[data.length - 9];
		for (int i = 0; i < messageArray.length && data[i + 10] != 0; i++) {
			messageArray[i] = data[i + 10];
		}
		String message = new String(messageArray).trim();
		return message;
	}

	

	public abstract void onReceipt(DatagramPacket packet);

	/**
	 *
	 * Listener thread
	 *
	 * Listens for incoming packets on a datagram socket and informs registered receivers about incoming packets.
	 */
	class Listener extends Thread {

		/*
		 *  Telling the listener that the socket has been initialized
		 */
		public void go() {
			latch.countDown();
		}

		/*
		 * Listen for incoming packets and inform receivers
		 */
		public void run() {
			try {
				latch.await();
				// Endless loop: attempt to receive packet, notify receivers, etc
				while(true) {
					DatagramPacket packet = new DatagramPacket(new byte[PACKETSIZE], PACKETSIZE);
					socket.receive(packet);

					onReceipt(packet);
				}
			} catch (Exception e) {if (!(e instanceof SocketException)) e.printStackTrace();}
		}
	}
}
