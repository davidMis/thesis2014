package com.davidmis.thesis.phone;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.rtp.AudioGroup;
import android.net.rtp.AudioStream;
import android.util.Log;

public class CallServerThread extends Thread  {
	
	final int SERVER_PORT = Telephony.PHONE_CTRL_PORT; 
	ServerSocket listener = null;
	
	final String SERVER_TAG = "SERVER_TAG";
	private Context context;
	
	private String myName = null;
	private String myIP;
	
	private static Socket serverSocket;
	
	public CallServerThread(Context c, String ip) {
		this.context = c;
		this.myIP = ip;
	}
	
	public void setMyName(String n) {
		this.myName = n;
	}
	
	public void run() {
		try {
			listener = new ServerSocket(SERVER_PORT);
			while (true) {
				new IncomingCallHandler(listener.accept()).start(); 
			}
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			try {
				listener.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
        }
	}
	
	public static Socket getServerSocket() {
		return serverSocket;
	}
	
	private class IncomingCallHandler extends Thread {
	    private BufferedReader in;
//	    private PrintWriter out;
	       
		public IncomingCallHandler(Socket s) {
			serverSocket = s;
		}
		
		public void run() {
			try {
	//			out = new PrintWriter(socket.getOutputStream(), true);
				in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));

				   
				switch(Integer.parseInt(in.readLine())) { //TODO handle bad input
				case Telephony.INCOMING_CALL:
			//		Log.i(SERVER_TAG, "Incoming Call!");
					
					String encryptedName = in.readLine();
					String alias = in.readLine();
					String challenge = in.readLine();
					String IP = in.readLine();
					String port = in.readLine();
					
					Log.i(SERVER_TAG, "Incoming call from " + encryptedName);
					
					String name = "";// TODO decrypt name
					
					
					Intent callIntent = new Intent(context, CallActivity.class);
					callIntent.putExtra("PEER_NAME", name);
					callIntent.putExtra("PEER_IP", IP);
					callIntent.putExtra("PEER_PORT", port);
					callIntent.putExtra("MY_NAME", myName);
					callIntent.putExtra("MY_IP", myIP);
					callIntent.putExtra("CHALLENGE", challenge);
					callIntent.putExtra("INCOMING_CALL", true);
					callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(callIntent);
					break;
				default:
					break;
				}   
			} catch (IOException e) {
				System.out.println(e);
			} finally {
		/*		try {
					serverSocket.close();
				} catch (IOException e) {
					System.out.println(e);
				} */
			}
		}
	}
}
