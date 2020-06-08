/**
 * Copyright 2011-2020 Cole Markham, all rights reserved.
 */
package net.ccmcomputing.discovery.udp;

import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Cole Markham
 * 
 */
public class DiscoveryPacket{
   public static final String UTF_8 = "UTF-8";
   public static final String REQUEST = "discovery.request";
   private static final byte MAGIC = (byte)REQUEST.hashCode();

   public static List<String> readPacketData(DatagramPacket packet) throws DiscoveryException{
      byte[] data = packet.getData();
      int offset = packet.getOffset();
      int length = packet.getLength();
      // length must be at least 3 (count(1 byte) + strLen(1 byte) + str (at
      // least 1 byte) = 3)
      if(length < 3 || data[offset++] != MAGIC) throw new DiscoveryException("Invalid packet magic");
      int count = data[offset++];
      List<String> list = new ArrayList<String>(count);
      for(int i = 0; i < count; i++){
         int strLen = data[offset++];
         try{
            String string = new String(data, offset, strLen, UTF_8);
            list.add(string);
            offset += strLen;
         }catch(UnsupportedEncodingException e){
            throw new DiscoveryException(e);
         }
      }
      return list;
   }

   private String address;
   private List<String> payload;

   public DiscoveryPacket(DatagramPacket datagramPacket) throws DiscoveryException{
      address = datagramPacket.getAddress().getHostAddress();
      payload = readPacketData(datagramPacket);
   }

   public DiscoveryPacket(List<String> payload){
      this.payload = payload;
   }

   public DatagramPacket createDatagramPacket(InetAddress targetAddress, int port) throws DiscoveryException{
      try{
         // max packet length is 1024
         byte[] data = new byte[1024];
         int offset = 0;
         data[offset++] = MAGIC;
         data[offset++] = (byte)payload.size();
         for(String str: payload){
            byte[] stringData = str.getBytes(UTF_8);
            data[offset++] = (byte)stringData.length;
            System.arraycopy(stringData, 0, data, offset, stringData.length);
            offset += stringData.length;
         }
         return new DatagramPacket(data, offset, targetAddress, port);

      }catch(UnsupportedEncodingException e){
         throw new DiscoveryException(e);
      }
   }

   public String getIpAddress(){
      return address;
   }

   public List<String> getPayload(){
      return payload;
   }

}
