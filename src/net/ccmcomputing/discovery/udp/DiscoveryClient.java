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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

/**
 * @author Cole Markham
 * 
 */
public class DiscoveryClient{
   public static void main(String[] args) throws IOException, DiscoveryException{
      final DiscoveryClient discoveryClient = new DiscoveryClient(42001, 42000);
      Thread thread = new Thread(new Runnable(){
         public void run(){
            try{
               Collection<DiscoveryPacket> discovered = discoveryClient.startListen(5, TimeUnit.SECONDS);
               for(DiscoveryPacket packet: discovered){
                  System.out.printf("%s : %s%n", packet.getIpAddress(), packet.getPayload().toString());
               }
            }catch(IOException e){
               e.printStackTrace();
            }
         }
      });
      thread.start();
      discoveryClient.sendRequest("example.service");
   }

   DatagramSocket socket;
   Collection<DiscoveryPacket> discoveredPackets;
   private byte[] buffer;

   private int soTimeout;
   private int sendPort;

   public DiscoveryClient(int listenPort, int sendPort) throws SocketException{
      this.sendPort = sendPort;
      // sendAddress = new InetSocketAddress(InetAddress.getLocalHost(),
      // sendPort);
      socket = new DatagramSocket(listenPort);
      discoveredPackets = new ArrayList<DiscoveryPacket>();
      buffer = new byte[1024];
   }

   /**
    * Listens for a single packet, stores it in the discoveredPackets list and
    * returns the packet.
    * 
    * @return the packet that was recieved
    * @throws SocketTimeoutException
    *            if a timeout was specified and the socket timed out before a
    *            packet was recieved
    * 
    * @throws IOException
    * @throws DiscoveryException
    */
   private DiscoveryPacket doListen() throws SocketTimeoutException, IOException, DiscoveryException{
      DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
      socket.receive(datagramPacket);
      DiscoveryPacket discoveryPacket = new DiscoveryPacket(datagramPacket);
      discoveredPackets.add(discoveryPacket);
      return discoveryPacket;
   }

   public void dispose(){
      socket.close();
   }

   public void sendRequest(String serviceIdentifier) throws IOException, DiscoveryException{
      DiscoveryPacket discoveryPacket = new DiscoveryPacket(Arrays.asList(DiscoveryPacket.REQUEST, serviceIdentifier));
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while(interfaces.hasMoreElements()){
         NetworkInterface networkInterface = interfaces.nextElement();
         // Don't want to broadcast to the loopback interface
         if(networkInterface.isLoopback()){
            continue;
         }
         for(InterfaceAddress interfaceAddress: networkInterface.getInterfaceAddresses()){
            InetAddress broadcast = interfaceAddress.getBroadcast();
            if(broadcast == null){
               continue;
            }
            // Use the address
            DatagramPacket reqPacket = discoveryPacket.createDatagramPacket(broadcast, sendPort);
            socket.send(reqPacket);
         }
      }

   }

   public void setSoTimeout(int soTimeout){
      this.soTimeout = soTimeout;
   }

   /**
    * Starts listening for discovery packets, waiting until
    * <code>maxCount</code> packets have been recieved. If
    * <code>setSoTimeout</code> was called with a non-zero value, then this
    * method could return fewer packets if the timeout is encountered. Otherwise
    * this method will block until <code>maxCount</code> packets have been
    * received.
    * 
    * @param maxCount
    *           the maximum number of packets to wait for
    * @return the packets which were recieved
    * @throws SocketTimeoutException
    * @throws IOException
    */
   public Collection<DiscoveryPacket> startListen(int maxCount) throws IOException{
      int count = 0;
      while(count < maxCount){
         try{
            doListen();
         }catch(SocketTimeoutException e){
            // Timeout, just break the loop
            break;
         }catch(DiscoveryException e){
            e.printStackTrace();
            continue;
         }
         count++;
      }
      return discoveredPackets;
   }

   /**
    * Starts listening for discovery packets, waiting the minimum of the
    * specified time or the value given to <code>setSoTimeout</code>.
    * 
    * @param timeout
    * @param unit
    * @return all packets which were recieved within the timeout
    * @throws SocketTimeoutException
    * @throws IOException
    */
   public Collection<DiscoveryPacket> startListen(long timeout, TimeUnit unit) throws IOException{
      int timeoutMillis = (int)TimeUnit.MILLISECONDS.convert(timeout, unit);
      if(soTimeout > 0){
         socket.setSoTimeout(Math.min(soTimeout, timeoutMillis));
      }else{
         socket.setSoTimeout(timeoutMillis);
      }
      long now = System.currentTimeMillis();
      long then = now + timeoutMillis;
      while(System.currentTimeMillis() < then){
         try{
            doListen();
         }catch(SocketTimeoutException e){
            break;
         }catch(DiscoveryException e){
            e.printStackTrace();
            continue;
         }
      }
      return discoveredPackets;
   }
}
