package com.davidmis.thesis.phone;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;

import android.app.Activity;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.net.rtp.AudioCodec;
import android.net.rtp.AudioGroup;
import android.net.rtp.AudioStream;
import android.net.rtp.RtpStream;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.davidmis.thesis.phone.pseudonym.AliasGenerator;
import com.example.alice.R;

public class CallActivity extends Activity {
	private TextView statusView;
	private Button button;
	
//TODO	private String peerName;
	private String peerID;
	private String peerIP;
	private String peerPort;
	
//TODO	private String myName;
	private String myID;
	private String myIP;
	private String myPort;
	
	private AudioGroup mAudioGroup;
	private AudioStream mAudioStream;
	private AudioManager mAudioManager;
	
	private final String LOG_TAG = "CallActivity-log";
	
	private Random rand = new Random();
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_call);	
		
//TODO		peerName = (String) getIntent().getExtras().get("PEER_NAME");
		peerID = (String) getIntent().getExtras().get("PEER_ID");
//TODO		myName = (String) getIntent().getExtras().get("MY_NAME");
		myID = (String) getIntent().getExtras().get("MY_ID");
		myIP = (String) getIntent().getExtras().get("MY_IP");
		
//TODO		if(myName == null) {
//			Log.e(LOG_TAG, "Trying to make/recieve a call without an identity!");
//		}
		
		/* Initialize Audio variables */
		mAudioManager = (AudioManager) getSystemService(getApplicationContext().AUDIO_SERVICE);
		mAudioGroup = new AudioGroup();
		new Thread(new Runnable () {
			public void run() {				
				try {
					mAudioStream = new AudioStream(InetAddress.getByName(myIP));
					myPort = Integer.toString(mAudioStream.getLocalPort());
				} catch (SocketException e) {
					Log.e(LOG_TAG, "Trouble setting up mAudioStream" + e.getLocalizedMessage());
					e.printStackTrace();
				} catch (UnknownHostException e) {
					Log.e(LOG_TAG, "Trouble setting up mAudioStream" + e.getLocalizedMessage());
					e.printStackTrace();
				}
			}
		}).start();
		 

		
		/* Setup UI */
		statusView = (TextView) findViewById(R.id.callStatus);
		button = (Button) findViewById(R.id.callActivityButton);
		
		if((Boolean) getIntent().getExtras().get("INCOMING_CALL")) {
//TODO			statusView.setText("Incoming Call From " + peerName);
			button.setText("Answer");
			button.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {
					new AcceptCallTask().execute(null, null, null);
				}
			});
		} else {
			initiateCall();
		}		
	}
	
	protected void onDestroy() {
		mAudioManager.setMode(AudioManager.MODE_NORMAL);
		mAudioGroup.setMode(AudioGroup.MODE_ON_HOLD);
		mAudioStream.join(null);
		
		super.onDestroy();
	}
	
	private void initiateCall() {
//TODO		statusView.setText("Calling  " + peerName);
		button.setText("Hangup");
		button.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
		
		new InitiateCallTask().execute(null, null, null);
		
				
	}
	
	public void startTalking() {
//TODO		statusView.setText("Talking to  " + peerName);
		button.setText("Hangup");
		button.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
		
		new Thread(new Runnable () {
			public void run() {

				mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
				mAudioManager.requestAudioFocus(afChangeListener,
						AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
				mAudioManager.setSpeakerphoneOn(true);
				mAudioManager.setMicrophoneMute(false);
				mAudioStream.setMode(RtpStream.MODE_NORMAL);
				mAudioStream.setCodec(AudioCodec.getCodecs()[0]);
				
				try {
					mAudioStream.associate(InetAddress.getByName(peerIP), Integer.parseInt(peerPort));
				} catch (UnknownHostException e) {
					Log.e(LOG_TAG, "Trouble associating mAudioStream" + e.getLocalizedMessage());
					e.printStackTrace();
				}
    			
    			Log.i(LOG_TAG, "My IP:" + myIP);
				Log.i(LOG_TAG, "My Port:" + myPort);
				Log.i(LOG_TAG, "Friend's IP:" + peerIP);
				Log.i(LOG_TAG, "Friend's Port:" + peerPort);
				Log.i(LOG_TAG, "Associated IP" + mAudioStream.getRemoteAddress());
				Log.i(LOG_TAG, "Associated Port" + mAudioStream.getRemotePort()); 
				
				mAudioStream.join(mAudioGroup);
				mAudioGroup.setMode(AudioGroup.MODE_NORMAL);
			}	
		}).start();
	}

	/* Not sure what this thing does */
	OnAudioFocusChangeListener afChangeListener = new OnAudioFocusChangeListener() {
		public void onAudioFocusChange(int focusChange) {
			if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
				mAudioManager.abandonAudioFocus(afChangeListener);
			}
		}
	};
	
	private class InitiateCallTask extends AsyncTask<Void, Void, Void> {
		protected Void doInBackground(Void... v) {
			
			//Log.i(LOG_TAG, "Starting ID generation: " + System.currentTimeMillis());
			
			byte[] id = AliasGenerator.getID(MainActivity.AliceIdentity.getTimingSecret(), 
							MainActivity.AliceIdentity.getIdSecret(), 
							System.currentTimeMillis());
			
			//Log.i(LOG_TAG, "Done with ID generation: " + System.currentTimeMillis());
			
			//Log.i(LOG_TAG, "ID: " + String.valueOf(id));
			
			/* Performance Test 1*/
			/* Random rand = new Random();
			long startTime;
			long endTime;
			for(int i = 0; i < 1000; i++) {
				byte[] timingSecret = new byte[Telephony.TIMING_SECRET_LENGTH];
				byte[] idSecret = new byte[Telephony.ID_SECRET_LENGTH];
				
				rand.nextBytes(timingSecret);
				rand.nextBytes(idSecret);
				
				startTime = System.currentTimeMillis();
				Alias.getID(timingSecret, idSecret, startTime);
				endTime = System.currentTimeMillis();
				Log.i("Performance-Test", Long.toString(endTime - startTime));
			//	Log.i("Performance-Test", MainActivity.bytesToHex(alias));
			}
							
			System.exit(1);
			/* End Performance Test */
			
			int r = rand.nextInt();

			Socket s = null;
			try {
				s = new Socket(Telephony.SERVER_IP, Telephony.SERVER_PORT);
				
				PrintWriter out = new PrintWriter(s.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

				out.println(Telephony.REQUEST_CALL);
				out.println(peerID);
				out.println(myID);
				out.println(r); 	// r
				out.println(); 		//TODO K_SharedKey(r)
				out.println(); 		//TODO K_Bob("Alice")
				out.println(myPort);
				
				if(Integer.parseInt(in.readLine()) == Telephony.ANSWER) {
					peerIP = in.readLine();
					peerPort = in.readLine();
				
					Log.i(LOG_TAG, "Got all info");
				} else {
					Log.e(LOG_TAG, "Expected Telephony.ANSWER");
				}
				
			} catch (IOException e) {
				Log.e(LOG_TAG, "Error calling: " + e.getLocalizedMessage());
			} finally {
				try {
					s.close();	
				} catch (IOException e) {
					Log.e(LOG_TAG, "Couldn't close s!: " + e.getLocalizedMessage());
				} catch (NullPointerException e) {
					Log.e(LOG_TAG, "Couldn't connect, maybe server is down?: " + e.getLocalizedMessage());
				}
			}	
			
			return null;
		}
		
		protected void onPostExecute(Void v){
			startTalking();
		}
	}
		
	
	
	private class AcceptCallTask extends AsyncTask<Void, Void, Void> {
		protected Void doInBackground(Void... v) {
				Socket s = null;
				try {
					s = CallServerThread.getServerSocket();
					
					PrintWriter out = new PrintWriter(s.getOutputStream(), true);
					
					out.println(Telephony.ANSWER);
					out.println("");
					out.println(myPort);
					
					peerIP = (String) getIntent().getExtras().get("PEER_IP");
					peerPort = (String) getIntent().getExtras().get("PEER_PORT");
					
					Log.i(LOG_TAG, "Peer IP: " + peerIP);
					Log.i(LOG_TAG, "Peer port: " + peerPort);

					
				} catch (IOException e) {
					Log.e(LOG_TAG, "Error calling: " + e.getLocalizedMessage());
				} finally {
					try {
						s.close();	
					} catch (IOException e) {
						Log.e(LOG_TAG, "Couldn't close s!: " + e.getLocalizedMessage());
					} catch (NullPointerException e) {
						Log.e(LOG_TAG, "Couldn't connect, maybe server is down?: " + e.getLocalizedMessage());
					}
				}
				
				return null;
		}
		
		protected void onPostExecute(Void v){
			startTalking();
		}
	}
}


