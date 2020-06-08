/**
 * Copyright 2011-2020 Cole Markham, all rights reserved.
 */
package net.ccmcomputing.discovery.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Cole Markham
 * 
 */
public class DiscoveryServer{
   public static void main(String[] args){
      DiscoveryServer discoveryServer = new DiscoveryServer(42000, "example.service", Arrays.asList("Test Server", "hostname:localhost", "port:9090"));
      discoveryServer.startThread();
   }

   final int listenPort;
   final String serviceIdentifier;

   private ServerThread serverThread;
   final List<String> responsePayload;

   public DiscoveryServer(int listenPort, String serviceIdentifier, List<String> responsePayload){
      this.listenPort = listenPort;
      this.serviceIdentifier = serviceIdentifier;
      this.responsePayload = responsePayload;
   }

   public void startThread(){
      serverThread = new ServerThread();
      serverThread.start();
   }

   public class ServerThread extends Thread{
      @Override
      public void run(){
         try{
            byte[] buffer = new byte[1024];
            try{
               InetSocketAddress listenAddr = new InetSocketAddress("0.0.0.0", listenPort);
               try(DatagramSocket socket = new DatagramSocket(listenAddr)){
            	   while(true){
            		   try{
            			   // Receive request from client
            			   DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            			   socket.receive(packet);
            			   try{
            				   DiscoveryPacket discoveryPacket = new DiscoveryPacket(packet);
            				   List<String> requestPayload = discoveryPacket.getPayload();
            				   if(requestPayload.size() != 2 || !DiscoveryPacket.REQUEST.equalsIgnoreCase(requestPayload.get(0)) || !serviceIdentifier.equalsIgnoreCase(requestPayload.get(1))){
            					   new DiscoveryException("Bad request").printStackTrace();
            				   }
            				   
            				   List<String> payload = new ArrayList<String>(responsePayload);
            				   DiscoveryPacket responsePacket = new DiscoveryPacket(payload);
            				   packet = responsePacket.createDatagramPacket(packet.getAddress(), packet.getPort());
            				   socket.send(packet);
            			   }catch(DiscoveryException e){
            				   // log and move on
            				   e.printStackTrace();
            			   }
            		   }catch(UnknownHostException ue){
            			   ue.printStackTrace();
            		   }
            	   }
               }
            }catch(java.net.BindException b){
               b.printStackTrace();
            }
         }catch(IOException e){
            System.err.println(e);
         }
      }
   }
}
