package com.davidmis.thesis.phone;

public abstract class Telephony {
	public static final int ACK = 100;
	public static final int UPDATE_ALIAS = 300;
	public static final int REQUEST_CALL = 400;
	public static final int INCOMING_CALL = 500;
	public static final int ANSWER = 600;
	
	public static final int START_TICKET_BATCH = 700;
	public static final int END_TICKET_BATCH = 701;
	public static final int TICKET_REQUEST = 702;
	
	
	public static final int PHONE_CTRL_PORT = 22999;
	public static final int SERVER_PORT = 2296;
	
	public static final String SERVER_IP = "192.168.1.137";
	
	/* Length of secrets in bytes */
	public static final int TIMING_SECRET_LENGTH = 32;
	public static final int ID_SECRET_LENGTH = 32;
	public static final int ID_LENGTH = 32;
}
