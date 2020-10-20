package net.ccmcomputing.discovery.udp;

public interface DiscoveryListener {
   void packetReceived(DiscoveryPacket packet);
}