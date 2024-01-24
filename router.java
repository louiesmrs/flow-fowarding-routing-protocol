import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer; 
// Author: Louie Somers
import java.util.TimerTask;

public class router<K> extends Node {
	
	/*
	 *
	 */
	/** Map to foward to next endpoint if dst address is known, */
	private Map<String, InetSocketAddress> fowardingTable;
	Timer timer = new Timer();
	long timeout = 1000_000; // milliseconds

	
	private static String number;
	ArrayList<String> addresses;
	byte[] Broker_id = new byte[]{(byte)0x00, (byte)0x00,(byte) 0x00}; 
	router(int port, String number) {
		router.number = number;
		addresses = new ArrayList<>(Arrays.asList(routerAddresses.get(number).split(",")));
		fowardingTable = new HashMap<String, InetSocketAddress>();
		try {
			socket= new DatagramSocket(port);
			listener.go();
		}
		catch(java.lang.Exception e) {e.printStackTrace();}
	}

	/**
	 * Assume that incoming packets contain a String and print the string.
	 */
	public synchronized void onReceipt(DatagramPacket packet) {
		System.out.println("Received packet from " + packet.getAddress());
		printForwardingTable();
			try {
				byte[] data = packet.getData();
				System.out.println("Current Router " + getCurrentRouter(data) + " router number " + number);
				System.out.println("Request recieved from " + getSource_id(data) + " with dest " + getDest_id(data));
				if(!getCurrentRouter(data).equals(number)) {
					System.out.println("Request recieved from to send TEXT: " + getMessage(data) + " packet type: " + getType(data));
					switch(getType(data)) {
							case MESSAGE:
								System.out.println("Request recieved from " + getSource_id(data) + " to send TEXT: " + getMessage(data));
								System.out.println("Do I know " + getSource_id(data) + "?");
								if(!fowardingTable.containsKey(getSource_id(data))) {
									addEntry(getSource_id(data), (InetSocketAddress)packet.getSocketAddress());
									printForwardingTable();
								}
								System.out.println("Do I know " + getDest_id(data) + "?");
								if(fowardingTable.containsKey(getDest_id(data))) {
									fowardMessage(data, getDest_id(data));
								} else {
									createMessage(data, packet.getAddress());
								}
								break;
							case REPLY:
								System.out.println("Request recieved from " + getSource_id(data) + " to send Reply to " + getDest_id(data));
								if(!fowardingTable.containsKey(getSource_id(data))) {
									addEntry(getSource_id(data), (InetSocketAddress)packet.getSocketAddress());
									printForwardingTable();
								}
								fowardMessage(data, getDest_id(data));
								break;
							case BROADCAST:
								System.out.println("Request recieved from to send Address to " + getDest_id(data));
								if(!fowardingTable.containsKey(getDest_id(data))) {
									addEntry(getDest_id(data), (InetSocketAddress)packet.getSocketAddress());
								}
								fowardMessage(data, getDest_id(data));
								break;
							case REMOVE:
								System.out.println("Request recieved from to " + getSource_id(data) + " to delete info from forwarding table");
								if(fowardingTable.containsKey(getSource_id(data))) {
									fowardingTable.remove(getSource_id(data));
								}
								printForwardingTable();
								createMessage(data, packet.getAddress());
								break;


						}
				}
				
			}
			catch(Exception e) {e.printStackTrace();}
		
	}


	public synchronized void start() throws Exception {
		System.out.println("Waiting for contact");
		while (true) {
			this.wait();
		}
	}

	/*
	 *
	 */
	public static void main(String[] args) {
		try {
			number = args[0].trim();
			(new router(ROUTER_PORT, number)).start();
			System.out.println("Program completed");
		} catch(java.lang.Exception e) {e.printStackTrace();}
	}

	public synchronized void fowardMessage(byte[] sendData, String dst) {
			setCurrentRouter(sendData, (byte)Integer.parseInt(number));
			// Foward to next hop
			try {
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, fowardingTable.get(dst));
				socket.send(sendPacket);
				System.out.println("Router Request packet fowarded to next hop: " + fowardingTable.get(dst));
			} catch (Exception e) {
				e.printStackTrace();
			}
	}
	public synchronized void createMessage(byte[] sendData, InetAddress receiverAddress) {
			System.out.println(receiverAddress);
			setCurrentRouter(sendData, (byte)Integer.parseInt(number));
			// Find the server using UDP broadcast
			try {
				socket.setBroadcast(true);
				for(int i = 0;i<addresses.size();i++) {
					if(!split(addresses.get(i)).equals(split(receiverAddress.toString().replace("/", "")))) {
						try {
							DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(addresses.get(i)), ROUTER_PORT);
							socket.send(sendPacket);
							System.out.println("Router Request packet sent to: " + addresses.get(i));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				}catch (IOException e) {
			 	e.printStackTrace();
				}
			}


	public void printForwardingTable() {
			System.out.println("Printing current fowarding table");
			for (String name : fowardingTable.keySet()) {
				String value = fowardingTable.get(name).toString();
				System.out.println(name + " " + value);
			}
	}



	
			
		
	private String split(String  ipAddressString) {
		String[] splittedArray = ipAddressString.split("\\.");
		return splittedArray [0] + "." + splittedArray [1] + "." + splittedArray [2] + ".";
	}

	synchronized void addEntry(String key, InetSocketAddress value) {
		//set timeout for the key-value pair to say 10 seconds
		fowardingTable.put(key, value);

		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				actionAfterTimeout(key);
			}
		}, timeout);
	}

	void actionAfterTimeout(String key) {
		System.out.println(key + " has been removed from fowarding table");
		if(fowardingTable.containsKey(key)) {
			fowardingTable.remove(key);
		}
	}
	
}
