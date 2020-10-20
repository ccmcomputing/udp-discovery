/**
 * Copyright 2011-2020 Cole Markham, all rights reserved.
 */
package net.ccmcomputing.discovery.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import net.ccmcomputing.discovery.udp.DiscoveryPacket.PacketType;

/**
 * @author Cole Markham
 * 
 */
public class DiscoveryServer {
   public static void main(String[] args) {
      String hostname = "hostname:";
      try {
         String canonicalHostName = InetAddress.getLocalHost().getCanonicalHostName();
         hostname += canonicalHostName;
      } catch (UnknownHostException e) {
         e.printStackTrace();
      }
      DiscoveryServer discoveryServer = new DiscoveryServer(42000, 42001,
            "example.service", Arrays.asList("Test Server", hostname, "port:9090"));
      discoveryServer.startThread();
   }

   final int listenPort;
   final String serviceIdentifier;

   private ServerThread serverThread;
   final List<String> responsePayload;
   private int announcePort;

   public DiscoveryServer(int listenPort, int announcePort, String serviceIdentifier, List<String> responsePayload) {
      this.listenPort = listenPort;
      this.serviceIdentifier = serviceIdentifier;
      this.responsePayload = responsePayload;
      this.announcePort = announcePort;
   }

   public void startThread() {
      serverThread = new ServerThread();
      serverThread.start();
   }

   public class ServerThread extends Thread {
      @Override
      public void run() {
         try {
            byte[] buffer = new byte[1024];
            try {
               System.out.println("Starting discovery server on port " + listenPort);
               System.out.println("\tPayload: " + responsePayload);
               InetSocketAddress listenAddr = new InetSocketAddress("0.0.0.0", listenPort);
               try (DatagramSocket socket = new DatagramSocket(listenAddr)) {
                  sendAnnouncementPacket(socket);
                  while (true) {
                     try {
                        // Receive request from client
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        handlePacket(socket, packet);
                     } catch (UnknownHostException ue) {
                        ue.printStackTrace();
                     }
                  }
               }
            } catch (java.net.BindException b) {
               b.printStackTrace();
            }
         } catch (IOException e) {
            System.err.println(e);
         }
         System.out.println("Discovery server stopped");
      }

   }

   private void sendAnnouncementPacket(DatagramSocket socket) throws IOException {
      List<String> announcePayload = new ArrayList<>();
      announcePayload.addAll(responsePayload);
      DiscoveryPacket broadcastPacket = new DiscoveryPacket(PacketType.ANNOUNCE, serviceIdentifier, announcePayload);
      try {
         sendBroadcast(socket, broadcastPacket, announcePort);
      } catch (DiscoveryException e1) {
         // TODO Auto-generated catch block
         e1.printStackTrace();
      }
   }

   private void sendBroadcast(DatagramSocket socket, DiscoveryPacket responsePacket, int sendPort)
         throws IOException, DiscoveryException {
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
            DatagramPacket reqPacket = responsePacket.createDatagramPacket(broadcast, sendPort);
            socket.send(reqPacket);
         }
      }

   }

   void handlePacket(DatagramSocket socket, DatagramPacket packet) throws IOException {
      try {
         DiscoveryPacket discoveryPacket = new DiscoveryPacket(packet);
         if (discoveryPacket.getPacketType() != PacketType.REQUEST
               || !serviceIdentifier.equalsIgnoreCase(discoveryPacket.getServiceIdentifier())) {
            new DiscoveryException("Bad request").printStackTrace();
         }
         System.out.println(discoveryPacket);
         List<String> payload = new ArrayList<>(responsePayload);
         DiscoveryPacket responsePacket = new DiscoveryPacket(PacketType.RESPONSE, serviceIdentifier, payload);
         packet = responsePacket.createDatagramPacket(packet.getAddress(), packet.getPort());
         socket.send(packet);
      } catch (DiscoveryException e) {
         // log and move on
         e.printStackTrace();
      }
   }
}
