/**
 * Copyright 2011-2020 Cole Markham, all rights reserved.
 */
package net.ccmcomputing.discovery.udp;

/**
 * @author Cole Markham
 * 
 */
public class DiscoveryException extends Exception{

   private static final long serialVersionUID = 1L;

   public DiscoveryException(String message){
      super(message);
   }

   public DiscoveryException(String message, Throwable cause){
      super(message, cause);
   }

   public DiscoveryException(Throwable cause){
      super(cause);
   }

}
