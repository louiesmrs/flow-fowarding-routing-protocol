
    /**
 *
 */
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.HexFormat;
import java.util.Scanner;
import java.io.IOException;

// Author: Louie Somers

/**
 *
 * Endpoint class
 *
 * An instance accepts user input
 *
 */
public class endpoint extends Node {
 
	static byte[] endpoint_id;
	InetSocketAddress dstAddress;
    String networkAddress;
	boolean hasRemovedInfo = false;
    static String number;
	byte[] removeInfoID = new byte[]{(byte)0xFF, (byte)0xFF,(byte) 0xFF, (byte) 0xFF}; 
	/**
	 * Constructor
	 *
	 * Attempts to create socket at given port and create an InetSocketAddress for the destinations
	 */
	endpoint(int srcPort) {
		try {
			socket= new DatagramSocket(srcPort);
            networkAddress = endpointAddresses.get(number.trim());
			listener.go();
		}
		catch(java.lang.Exception e) {e.printStackTrace();}
	}

	public static void main(String[] args) {
        
		try {
            Scanner scanner = new Scanner(System.in);
			
			while(true) {
				System.out.println("Which endpoint are you?");
				number = scanner.nextLine();
				
				if(Integer.parseInt(number) > 0 && Integer.parseInt(number) <= NUMBEROFENDPOINTS) {
					break;
				}
				System.out.println("Invalid endpoint. Endpoints must be between 0 and " + NUMBEROFENDPOINTS);
			}
            while(true) {
				System.out.println("What is your ID?");
				String id = scanner.nextLine();
                if(id.matches("[a-zA-Z0-9]*")) {
                        byte[] test = HexFormat.of().parseHex(id);
                        if(test.length == IDSIZE) {
                            endpoint_id = test;
                            break;
                        }
                    
                }
				System.out.println("Invalid ID. ID must be 8 digit hexidecimal number.");
			}
			(new endpoint(ROUTER_PORT)).start();
			System.out.println("Program completed");
		} catch(java.lang.Exception e) {e.printStackTrace();}
	}


	/**
	 * Sender Method
	 *
	 */
	public synchronized void start() throws Exception {
		sendBroadcast();
		this.wait();
        while (true) {
			Scanner scanner = new Scanner(System.in);
			System.out.println("Instructions:\n" +
				">SEND\nTo send a message to another Endpoint\n" +
				">WAIT\nTo wait for messages\n" +
				">REMOVE\nTo remove your info from routers");
			String startUI = scanner.nextLine();
			if(hasRemovedInfo) {
				sendBroadcast();
				hasRemovedInfo = false;
				this.wait();
			}
			if (startUI.toUpperCase().contains("SEND")) {
				createMessage(scanner);
				this.wait();
			} else if (startUI.toUpperCase().contains("WAIT")) {
				this.wait(); // then wait for the message
		   
		    } else if (startUI.toUpperCase().contains("REMOVE")) {
				removeInfo();
				hasRemovedInfo = true;
		    }  else {
				System.out.println("You just entered invalid input!");
			}
		}
	}

	public synchronized void onReceipt(DatagramPacket packet) {
        byte[] data = packet.getData();
        if(getType(packet.getData()) != BROADCAST) {
			if(!getSource_id(data).equals(getStringFromByte(endpoint_id)) && getDest_id(data).equals(getStringFromByte(endpoint_id))) {
				this.notify();
				try{
					if (getType(data) == MESSAGE) {
						System.out.println("Message received: " + getMessage(data));
						responseMessage(getDest_id(data) + " has received your message at its assigned destination", getByteSource(data));
					} else if(getType(data) == REPLY) {
						System.out.println("REPLY received: " + getMessage(data));
					}
					
				}catch(Exception e) {e.printStackTrace();}
            }
		} else if(getType(packet.getData()) == BROADCAST && !getCurrentRouter(packet.getData()).equals("0")) {
                this.notify();
                System.out.println("Reply to Broadcast Received received: " + packet.getSocketAddress());
                dstAddress = (InetSocketAddress) packet.getSocketAddress();
		}	
	}


	public void createMessage(Scanner scan) {
        byte[] endpoint_dst;
        while(true) {
				System.out.println("What ID do you want to send to?");
				String endpoint = scan.nextLine();
                if(endpoint.matches("[a-zA-Z0-9]*")) {
						byte[] test = HexFormat.of().parseHex(endpoint);
						if(test.length == IDSIZE) {
							endpoint_dst = test;
							break;
						}
                }
				System.out.println("Invalid ID. ID must be 8 digit hexidecimal number.");
		}
        System.out.println("Enter text you want to send");
        String message = scan.nextLine();
        // Find the server using UDP broadcast
        try {
            byte[] sendData = createHeader(MESSAGE, endpoint_dst, endpoint_id, (byte) 0, message);
            try {
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, dstAddress);
                socket.send(sendPacket);
                System.out.println("Endpoint " + number + "  Request packet sent to: " + dstAddress);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
		public void responseMessage(String message, byte[] endpoint_dst) {
			
		// Find the server using UDP broadcast
			try {
				byte[] sendData = createHeader(REPLY, endpoint_dst, endpoint_id, (byte) 0, message);
				try {
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, dstAddress);
					socket.send(sendPacket);
					System.out.println("Endpoint" + number + "  Request packet sent to: " + dstAddress);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			System.out.println("Endpoint " + number + " sent reply");
			} catch (Exception e) {
				e.printStackTrace();
			
			}
		}

		public void removeInfo() {
		
		// Find the server using UDP broadcast
			try {
				byte[] sendData = createHeader(REMOVE, removeInfoID, endpoint_id, (byte) 0, 
				"Endpoint" + number + " Request packet to remove their info from fowarding table");
				try {
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, dstAddress);
					socket.send(sendPacket);
					System.out.println("Endpoint" + number + "  Request packet sent to: " + dstAddress + " to remove their info from fowarding table");
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			System.out.println("Endpoint " + number + " sent reply");
			} catch (Exception e) {
				e.printStackTrace();
			
			}
		}

		public void sendBroadcast() {
			
		// Find the server using UDP broadcast
			try {
				socket.setBroadcast(true);
				byte[] sendData = createBroadcastHeader(BROADCAST, endpoint_id, (byte) 0);
				try {
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(networkAddress), ROUTER_PORT);
					socket.send(sendPacket);
					System.out.println("Endpoint1 Request packet sent to: 172.20.20.255 (DEFAULT)");
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			System.out.println("Endpoint1 Done looping over all network interfaces. Now waiting for a reply!");
			} catch (IOException e) {
				e.printStackTrace();
			
			}
		}

		

			
		


	
}

    

