package com.davidmis.thesis.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import com.davidmis.thesis.network.authorization.Ticket;

public class MSC {

   private HashMap<String, Phone> registeredPhones = new HashMap<String, Phone>();
   private BigInteger e;
   private BigInteger d;
   private BigInteger N;

   public MSC() throws IOException {
	   // Get exponents and modulus for tickets
	   e = new BigInteger("65537");
	   d = new BigInteger("17e5e9997b1fc170b0ec786847ecc1" +
	   		"cbace0c6915a246e46876dcf45588d" +
	   		"744259f2e334f7de85d55a27be7cc7" +
	   		"1a581160924055e2ad1d567a29a33c" +
	   		"c8b99d5d089d01d418ac46888331ba" +
	   		"e6c27ce4a0eaab1030693b1b8ef87e" +
	   		"eda968e6849cc2f7402d3736054e23" +
	   		"fa94d88aecb908c68fc8c2f31332ca" +
	   		"32709aeb9fa17461", 16);
	   N = new BigInteger("00b0c5e8a2ddf0e03ac7dd22769716" +
	   		"5b16095fe9689dcfeebfca09355f1d" +
	   		"172379e168838558fa8066a17da636" +
	   		"7b68cd99d980a970146d90eb8757a5" +
	   		"9302d7d70dbd3821b8d6469b0ad718" +
	   		"7c1901718a04e157605a43b013f7c0" +
	   		"c551fa2d2c178eeb8fb3ffd7850622" +
	   		"25178b798d2ece8aee191749975035" +
	   		"4b9a2173c73dc61bd7", 16);
	   
	   //System.out.println(BigInteger.TEN.modPow(e, N).modPow(d, N).toString());
	   
	   // Start server
	   ServerSocket listener = new ServerSocket(2296); 
       try {
    	   while (true) {
    		   RequestHandler req = new RequestHandler(listener.accept());
    		   req.start();
    	   }
       }
       finally {
           listener.close();
       }
   }
   
   private synchronized void addPhone(String name, String IP) {
	   Phone phone = new Phone(name, IP);
	   
	   registeredPhones.put(phone.getName(), phone);
	   
	   System.out.println("Registering Phone:");
	   System.out.println(phone.getName());
	   System.out.println(phone.getIP());
   }
   
   private synchronized Phone getPhone(String name) {
	   if(!registeredPhones.containsKey(name)) {
		   System.out.println("Couldn't find phone with name " + name);
		   return null;
	   }

	   return registeredPhones.get(name);
   }
   
/*   private class CallHandler extends Thread {
	   private Phone caller;
	   private Phone callee;

	   private Socket socket;
       private BufferedReader in;
       private PrintWriter out;
	   
	   public CallHandler(Phone c1, Phone c2) {
		   caller = c1;
		   callee = c2;
	   }
	   
	   public void run() {
		   try {
			   socket = new Socket(callee.getIP(), Telephony.PHONE_CTRL_PORT);
			   out = new PrintWriter(socket.getOutputStream(), true);
			   in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			   
			   out.println(Telephony.INCOMING_CALL);
			   out.println(caller.getName());
			   out.println(caller.getIP());
			   out.println(caller.getPort());
		   } catch (IOException e) {
               System.out.println(e);
           } finally {
        	   try {
        		   socket.close();
        	   } catch (IOException e) {
               System.out.println(e);
        	   }
           }
	   }
   } */
	   
   private class RequestHandler extends Thread {
	   private Socket socket;
       private BufferedReader in;
       private PrintWriter out;

	   public RequestHandler(Socket s) {
		   this.socket = s;
	   }
	   
	   public void run() {
		   try {
			   out = new PrintWriter(socket.getOutputStream(), true);
			   in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			   
			   switch(Integer.parseInt(in.readLine())) { //TODO handle bad input
			   
			   case Telephony.TICKET_REQUEST:
//				   System.out.println("Starting GrantTicket Batch");
	//			   System.out.print("Sending modulus and exponent...");
				   out.println(N.toString());
				   out.println(e.toString());
//				   System.out.println("Done!");
				   
				   ArrayList<BigInteger> msgList = new ArrayList<BigInteger>();
				   
			//	   if(Integer.parseInt(in.readLine()) != Telephony.START_TICKET_BATCH) {
			//		   System.out.println("ERROR: Expected Telephony.START_TICKET_BATCH");
			//	   }
//				   System.out.println(in.readLine());
				   in.readLine();
//				   System.out.print("Getting batch...");
				   String msg = in.readLine();
				   do {
			//		   System.out.println("msg: " + msg);
					   msgList.add(new BigInteger(msg));
					   
					   msg = in.readLine();
				   } while(!msg.equals(String.valueOf(Telephony.END_TICKET_BATCH)));
//				   System.out.println("Done!");
			   
			//	   System.out.println("# of tickets to send: " + msgList.size());
		//		   System.out.print("Sending tickets...");
				   long cryptoTime = 0;
				   
				   for(BigInteger m_prime : msgList) {
					   long startTime = System.currentTimeMillis();
					   BigInteger s_prime = m_prime.modPow(d, N);
					   long endTime = System.currentTimeMillis();
					   
					   cryptoTime += endTime - startTime;
					   
					   out.println(s_prime.toString());
				   }
				   
				   System.out.println(Long.toString(cryptoTime));
	//			   System.out.print("Done!");
				   
				   break;
			   case Telephony.UPDATE_ALIAS:
				   System.out.println("Registering Phone");
				   String pname = in.readLine();
				   String time = in.readLine();
				   String ticket = in.readLine();
				   String pIP = in.readLine();
				   if(Ticket.ticketIsValid(pname, time, ticket, e, N)) {
					   addPhone(pname, pIP);
				   } else {
					   System.out.println("Invalid Ticket");
					   System.exit(1);
				   }
				   break;
			   case Telephony.REQUEST_CALL:
				   String calleeName = in.readLine();
				   String callerName = in.readLine();
				   String r = in.readLine();
				   String challenge = in.readLine();
				   String encryptedName = in.readLine();
				   String callerPort = in.readLine();
	
				   Phone caller = getPhone(callerName);
				   Phone callee = getPhone(calleeName);
				   
				   Socket rSocket = new Socket(callee.getIP(), Telephony.PHONE_CTRL_PORT);
				   PrintWriter rOut = new PrintWriter(rSocket.getOutputStream(), true);
				   BufferedReader rIn = new BufferedReader(new InputStreamReader(rSocket.getInputStream()));
				   
				   rOut.println(Telephony.INCOMING_CALL);
				   rOut.println(encryptedName);
				   rOut.println(callerName);
				   rOut.println(challenge);
				   rOut.println(caller.getIP());
				   rOut.println(callerPort);
				   
				   System.out.println("Waiting for Answer");
				   
				   if(Integer.parseInt(rIn.readLine()) == Telephony.ANSWER) {	
					   String response = rIn.readLine();
					   String calleePort = rIn.readLine();
				   
			//		   if(response.equalsIgnoreCase(r)) {
					   if(true) {
						   out.println(Telephony.ANSWER);
						   out.println(callee.getIP());
						   out.println(calleePort);
					   } else {
						   System.out.println("Challenge failed"); //TODO
					   }
				   } else {
					   System.out.println("ERROR: Expected Answer");
				   }
				   break;
			   default:
				   break;
			   }

           } catch (IOException e) {
               System.out.println(e);
           } finally {
        	   try {
        		   socket.close();
        	   } catch (IOException e) {
               System.out.println(e);
        	   }
           }
	   }
   }  
   
   public static void main(String[] args) throws IOException {
	   new MSC();
   }
}
