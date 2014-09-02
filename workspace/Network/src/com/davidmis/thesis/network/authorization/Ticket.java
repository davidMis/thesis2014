package com.davidmis.thesis.network.authorization;

import java.math.BigInteger;
import java.nio.ByteBuffer;

public class Ticket {

	public static final long MILLIS_PER_MIN = 60000;
	
	public static byte[] buildTicket(byte[] alias, long time) {
		ByteBuffer ticket = ByteBuffer.allocate(alias.length + 12);
		
		ticket.put((byte)0x11);
		ticket.put((byte)0x11);
		ticket.put((byte)0x11);
		ticket.put((byte)0x11);
		ticket.put(alias, 0, alias.length);
		ticket.putLong(alias.length + 4, time); 
		
		return ticket.array();
	}
	
	public static byte[] getAliasFromTicket(byte[] ticket) {
		ByteBuffer alias = ByteBuffer.allocate(ticket.length - 12);
		alias.put(ticket, 4, ticket.length - 12);
		return alias.array();
	}
	
	public static long getTimeFromTicket(byte[] ticket) {
		ByteBuffer ticketBB = ByteBuffer.allocate(ticket.length);
		ticketBB.put(ticket, 0, ticket.length);
		return ticketBB.getLong(ticket.length - 8);
	}
	
	public static boolean ticketIsValid(String declared_alias, String time, String ticket, BigInteger e, BigInteger N) {
		
		long declared_time = Long.valueOf(time);
		BigInteger blindedTicket = new BigInteger(ticket);
		BigInteger unblindedTicket = blindedTicket.modPow(e, N);
		unblindedTicket = unblindedTicket.mod(N);
		
		long ticket_time = getTimeFromTicket(unblindedTicket.toByteArray());
		String ticket_alias = bytesToHex(getAliasFromTicket(unblindedTicket.toByteArray()));

		
		System.out.println("ticket_alias:\t" + ticket_alias);
		System.out.println("declared_alias:\t" + declared_alias);
		System.out.println("ticket_time:\t" + ticket_time);
		System.out.println("declared_time:\t" + declared_time);
		System.out.println("unblinded_ticket:\t" + unblindedTicket.toString(16));
		System.out.println("unblinded_ticket:\t" + unblindedTicket.toString(10));
		System.out.println("blinded_ticket:\t" + blindedTicket.toString(16));
		
		//Be sure declared_time is within 10 minutes of currentTime
		long currentTime = System.currentTimeMillis();
		if(Math.abs(currentTime - declared_time) > MILLIS_PER_MIN * 10) {
			System.out.println("declared_time expired: " + declared_time + " " + currentTime);
			return false;
		}
		
		//Check ticket_time equals declared_time
		if(ticket_time != declared_time) {
			System.out.println("ticket_time does not equal declared_time");
			System.out.println("ticket_time: " + ticket_time);
			System.out.println("declared_time: " + declared_time);
			return false;
		}
		
		//Check ticket_alias equals declared_alias
		if(!ticket_alias.equals(declared_alias)) {
			System.out.println("ticket_alias does not equal declared_alias");
			System.out.println("ticket_alias: " + ticket_alias);
			System.out.println("declared_alias: " + declared_alias);
			return false;
		}
		
		
		
		return true;
	}
	
	/* http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java */
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
}
