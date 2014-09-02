package com.davidmis.thesis.phone.pseudonym;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

import android.util.Log;

public class AliasGenerator {
	
	/* All time constants in milliseconds */ 
	public static long MIN_UPDATE = 60000;  			// 1 minute 
	public static long MAX_UPDATE = 600000;				// 10 minutes
//	public static long MIN_UPDATE = 6000;  				// 6 seconds
//	public static long MAX_UPDATE = 60000;				// 1 minutes
	public static long PERIOD_LENGTH = 86400000;		// 1 day
	
	public static long ALIAS_WINDOW_LENGTH = MAX_UPDATE - MIN_UPDATE;
	
	private static final String LOG_TAG = "Alice-log";
	
	/* The following are determined by above constants and 
	 * the desired update granularity. 16 bpa gives an update
	 * granularity of about 1 second. */
	//TODO Determine these programatically
	private static int BITS_PER_AUS = 16;
	private static int BYTES_PER_AUS = 2;
	private static int ROUNDS_PER_PERIOD = 90;
	private static int AUS_PER_ROUND = 16;
	private static int AUS_PER_PERIOD = (256 / BITS_PER_AUS) * ROUNDS_PER_PERIOD;
	private static int MAX_AUS = 65535;
	
	/* Returns the ID of a phone with given secrets and given time */ 
	public static byte[] getID(byte[] timingSecret, byte[] idSecret, long time) {		
		MessageDigest sha256 = null;		
		try {
			sha256 = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		long lastAU = getLastUpdateTime(timingSecret, time);
		
		ByteArrayOutputStream idStream = new ByteArrayOutputStream( );
		try {
			idStream.write(ByteBuffer.allocate(8).putLong(lastAU).array());
			idStream.write(idSecret);
		} catch (IOException e) { e.printStackTrace(); }
		
		return sha256.digest(idStream.toByteArray());
	}
	
	public static long getLastUpdateTime(byte[] timingSecret, long time) {
		long periodStart = time - (time % PERIOD_LENGTH);
		
		/* int array for convenience, should probably change to make more general */
		//int[] ausArray = new int[AUS_PER_PERIOD];
		ArrayList<Double> ausArray = buildAUSArray(timingSecret, periodStart);
		
		long lastAU = periodStart + MIN_UPDATE + (long)((ALIAS_WINDOW_LENGTH) * ausArray.get(0));
		long nextAU = 0;
		int count = 0;
		
//		Log.i(LOG_TAG, "periodStart: " + periodStart);
//		Log.i(LOG_TAG, "time: " + time);
//		Log.i(LOG_TAG, "lastAU: " + lastAU);
//		Log.i(LOG_TAG, "ausArray[0]" + ausArray.get(0));
		
		while(true) {
			count = count + 1;
			nextAU = lastAU + MIN_UPDATE + (long)((ALIAS_WINDOW_LENGTH) * ausArray.get(count));
			
			if(nextAU > time)
				break;
			
			lastAU = nextAU;
		}
		
		return lastAU;
	}
	
	/* Returns the next AU after time */
	public static long getNextUpdateTime(byte[] timingSecret, long time) {
		
		long periodStart = time - (time % PERIOD_LENGTH);
		
		/* int array for convenience, should probably change to make more general */
		//int[] ausArray = new int[AUS_PER_PERIOD];
		ArrayList<Double> ausArray = buildAUSArray(timingSecret, periodStart);
		
		
		
		long lastAU = periodStart + MIN_UPDATE + (long)((ALIAS_WINDOW_LENGTH) * ausArray.get(0));
		long nextAU = 0;
		int count = 0;
		
//		Log.i(LOG_TAG, "periodStart: " + periodStart);
//		Log.i(LOG_TAG, "time: " + time);
//		Log.i(LOG_TAG, "lastAU: " + lastAU);
//		Log.i(LOG_TAG, "ausArray[0]" + ausArray.get(0));
		
		while(true) {
			count = count + 1;
			nextAU = lastAU + MIN_UPDATE + (long)((ALIAS_WINDOW_LENGTH) * ausArray.get(count));
			
			if(nextAU > time)
				break;
			
			lastAU = nextAU;
		}
		
		return nextAU;
	}
	
	private static ArrayList<Double> buildAUSArray(byte[] timingSecret, long time) {
		byte[] timeBytes = ByteBuffer.allocate(8).putLong(time).array();
		
		ArrayList<Double> ausArray = new ArrayList<Double>();
		
		MessageDigest sha256 = null;		
		try {
			sha256 = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		/* Build ausArray */
		for(int i = 0; i < ROUNDS_PER_PERIOD; i++) {

			ByteArrayOutputStream timingStream = new ByteArrayOutputStream( );
			try {
				timingStream.write(timingSecret);
				timingStream.write(timeBytes);
				timingStream.write(i);
			} catch (IOException e) { e.printStackTrace(); }
			
			byte[] digest = sha256.digest(timingStream.toByteArray());
			
			/* Split and copy digest to aus array */
			for(int j = 0; j < AUS_PER_ROUND; j++) {
				/* TODO Not at all general */
				int aus = ByteBuffer.wrap(Arrays.copyOfRange(digest, j, j+2)).getShort();
				ausArray.add((double)aus / MAX_AUS + 0.5); //TODO handling of signed scalar
			}
		}
		
		return ausArray;
	}
	
}
