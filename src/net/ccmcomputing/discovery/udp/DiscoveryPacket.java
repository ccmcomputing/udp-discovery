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
public class DiscoveryPacket {
   public static final String UTF_8 = "UTF-8";

   public static enum PacketType {
      REQUEST("RQST"), RESPONSE("RESP"), ANNOUNCE("ANNC");

      final String value;

      PacketType(String value) {
         this.value = value;
      }

      static PacketType lookup(String id) {
         for (PacketType pt : values()) {
            if (pt.value.equals(id))
               return pt;
         }
         return null;
      }
   }

   private static final byte MAGIC = (byte)197;

   private String address;
   private PacketType packetType;
   private String serviceIdentifier;
   private List<String> payload;

   public DiscoveryPacket(DatagramPacket datagramPacket) throws DiscoveryException {
      address = datagramPacket.getAddress().getHostAddress();
      readPacketData(datagramPacket);
   }

   public DiscoveryPacket(PacketType type, String serviceIdentifier, List<String> payload) {
      this.packetType = type;
      this.serviceIdentifier = serviceIdentifier;
      this.payload = payload;
   }

   private void readPacketData(DatagramPacket packet) throws DiscoveryException {
      byte[] data = packet.getData();
      int offset = packet.getOffset();
      int length = packet.getLength();
      // length must be at least 3 (count(1 byte) + strLen(1 byte) + str (at
      // least 1 byte) = 3)
      if (length < 3 || data[offset++] != MAGIC)
         throw new DiscoveryException("Invalid packet magic");
      String packetTypeId = readPackedString(data, offset++);
      offset += packetTypeId.length();
      packetType = PacketType.lookup(packetTypeId);
      serviceIdentifier = readPackedString(data, offset++);
      offset += serviceIdentifier.length();
      int count = data[offset++];
      payload = new ArrayList<String>(count);
      for (int i = 0; i < count; i++) {
         String string = readPackedString(data, offset++);
         offset += string.length();
         payload.add(string);
      }
   }

   private String readPackedString(byte[] data, int offset) throws DiscoveryException {
      int strLen = data[offset++];
      try {
         String string = new String(data, offset, strLen, UTF_8);
         return string;
      } catch (UnsupportedEncodingException e) {
         throw new DiscoveryException(e);
      }
   }
   
   private int writePackedString(byte[] data, int offset, String str) throws DiscoveryException {
      try {
         byte[] stringData = str.getBytes(UTF_8);
         data[offset++] = (byte) stringData.length;
         System.arraycopy(stringData, 0, data, offset, stringData.length);
         offset += stringData.length;
      } catch (UnsupportedEncodingException e) {
         throw new DiscoveryException(e);
      }
      return offset;
   }

   /*
    * 
    * 
    */
   public DatagramPacket createDatagramPacket(InetAddress targetAddress, int port) throws DiscoveryException {
      // max packet length is 1024
      // FIXME handle case where packet is larger
      byte[] data = new byte[1024];
      int offset = 0;
      data[offset++] = MAGIC;
      offset = writePackedString(data, offset, packetType.value);
      offset = writePackedString(data, offset, serviceIdentifier);
      data[offset++] = (byte) payload.size();
      for (String str : payload) {
        offset = writePackedString(data, offset, str);
      }
      return new DatagramPacket(data, offset, targetAddress, port);
   }

   public String getIpAddress() {
      return address;
   }

   public List<String> getPayload() {
      return payload;
   }

   public String getServiceIdentifier() {
      return serviceIdentifier;
   }

   public PacketType getPacketType() {
      return packetType;
   }

   @Override
   public String toString() {
      return "DiscoveryPacket [address=" + address + ", packetType=" + packetType + ", serviceIdentifier="
            + serviceIdentifier + ", payload=" + payload + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((address == null) ? 0 : address.hashCode());
      result = prime * result + ((packetType == null) ? 0 : packetType.hashCode());
      result = prime * result + ((payload == null) ? 0 : payload.hashCode());
      result = prime * result + ((serviceIdentifier == null) ? 0 : serviceIdentifier.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      DiscoveryPacket other = (DiscoveryPacket) obj;
      if (address == null) {
         if (other.address != null)
            return false;
      } else if (!address.equals(other.address))
         return false;
      if (packetType != other.packetType)
         return false;
      if (payload == null) {
         if (other.payload != null)
            return false;
      } else if (!payload.equals(other.payload))
         return false;
      if (serviceIdentifier == null) {
         if (other.serviceIdentifier != null)
            return false;
      } else if (!serviceIdentifier.equals(other.serviceIdentifier))
         return false;
      return true;
   }

}
