package com.davidmis.thesis.phone.pseudonym;

import java.util.Random;

import android.util.Log;

import com.davidmis.thesis.phone.Telephony;

public class PersonIdentity {
	private String name;
	private byte[] timingSecret; 
	private byte[] idSecret;
	
	private final String LOG_TAG = "Alice-log";
	
	public PersonIdentity(String name) {
		this.name = name;
		this.timingSecret = generateSecret(Telephony.TIMING_SECRET_LENGTH);
		this.idSecret = generateSecret(Telephony.ID_SECRET_LENGTH);
	}
	
	public PersonIdentity(String name, byte[] ts, byte[] is) {
		this.name = name;
		timingSecret = ts;
		idSecret = is;
		
		if(timingSecret.length != Telephony.TIMING_SECRET_LENGTH) {
			Log.e(LOG_TAG, "ERROR: timingSecret wrong length!");
		}
		
		if(idSecret.length != Telephony.ID_SECRET_LENGTH) {
			Log.e(LOG_TAG, "ERROR: idSecret wrong length!");
		}
	}
	
	/* FOR TESTING AND DEMONSTRATION ONLY. THIS METHOD IS
	 * NOT MEANT TO BE A SECURE WAY TO GENERATE SECRETS! */
	private byte[] generateSecret(int length) {
		Random r = null;
		
		if(this.name == "Alice") {
			r = new Random(0);
		} else if(this.name == "Bob") {
			r = new Random(1);
		}
		
		byte[] secret = new byte[length];
		
		r.nextBytes(secret);
		
		return secret; 
	}
	
	public byte[] getID(long time) {
		return AliasGenerator.getID(this.timingSecret, this.idSecret, time);
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public byte[] getTimingSecret() {
		return timingSecret;
	}

	public void setTimingSecret(byte[] timingSecret) {
		this.timingSecret = timingSecret;
	}

	public byte[] getIdSecret() {
		return idSecret;
	}

	public void setIdSecret(byte[] idSecret) {
		this.idSecret = idSecret;
	}
	
	public static PersonIdentity randomId() {
		Random rand = new Random();
		
		byte[] timingSecret = new byte[Telephony.TIMING_SECRET_LENGTH];
		byte[] idSecret = new byte[Telephony.ID_SECRET_LENGTH];
		
		rand.nextBytes(timingSecret);
		rand.nextBytes(idSecret);
		
		return new PersonIdentity("Random ID", timingSecret, idSecret);
	}
	
}
