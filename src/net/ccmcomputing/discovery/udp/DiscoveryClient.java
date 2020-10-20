/**
 * Copyright 2011-2020 Cole Markham, all rights reserved.
 */
package net.ccmcomputing.discovery.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Enumeration;

import net.ccmcomputing.discovery.udp.DiscoveryPacket.PacketType;

/**
 * @author Cole Markham
 * 
 */
public class DiscoveryClient {
   public static void main(String[] args) throws IOException, DiscoveryException {
      final DiscoveryClient discoveryClient = new DiscoveryClient(42001, 42000, packet -> {
         System.out.printf("%s : %s%n", packet.getIpAddress(), packet.getPayload().toString());
      });
      discoveryClient.sendRequest("example.service");
   }

   DatagramSocket socket;
   private byte[] buffer;

   private int sendPort;
   private ClientThread clientThread;

   public DiscoveryClient(int listenPort, int sendPort, DiscoveryListener listener) throws SocketException {
      this.sendPort = sendPort;
      // sendAddress = new InetSocketAddress(InetAddress.getLocalHost(),
      // sendPort);
      socket = new DatagramSocket(listenPort);
      buffer = new byte[1024];
      startListenerThread(listener);
   }

   /**
    * Listens for a single packet, stores it in the discoveredPackets list and
    * returns the packet.
    * 
    * @return the packet that was recieved
    * @throws SocketTimeoutException if a timeout was specified and the socket
    *                                timed out before a packet was recieved
    * 
    * @throws IOException
    * @throws DiscoveryException
    */
   private DiscoveryPacket receivePacket() throws SocketTimeoutException, IOException, DiscoveryException {
      DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
      socket.receive(datagramPacket);
      DiscoveryPacket discoveryPacket = new DiscoveryPacket(datagramPacket);
      return discoveryPacket;
   }

   public void dispose() {
      socket.close();
      clientThread.running = false;
   }

   public void sendRequest(String serviceIdentifier) throws IOException, DiscoveryException {
      DiscoveryPacket discoveryPacket = new DiscoveryPacket(PacketType.REQUEST, serviceIdentifier, Collections.emptyList());
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
         NetworkInterface networkInterface = interfaces.nextElement();
         // Don't want to broadcast to the loopback interface
         if (networkInterface.isLoopback()) {
            continue;
         }
         for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
            InetAddress broadcast = interfaceAddress.getBroadcast();
            if (broadcast == null) {
               continue;
            }
            // Use the address
            DatagramPacket reqPacket = discoveryPacket.createDatagramPacket(broadcast, sendPort);
            socket.send(reqPacket);
         }
      }

   }

   private void startListenerThread(DiscoveryListener listener) {
      clientThread = new ClientThread(listener);
      clientThread.start();
   }

   public class ClientThread extends Thread {
      DiscoveryListener listener;
      volatile boolean running = true;

      public ClientThread(DiscoveryListener listener) {
         this.listener = listener;
      }

      @Override
      public void run() {
         try {
            while (running) {
               DiscoveryPacket packet = receivePacket();
               listener.packetReceived(packet);
            }

         } catch (IOException | DiscoveryException e) {
            System.err.println(e);
         }
         System.out.println("Discovery client stopped");
      }

   }

}
