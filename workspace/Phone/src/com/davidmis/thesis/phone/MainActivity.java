package com.davidmis.thesis.phone;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.conn.util.InetAddressUtils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;

import com.davidmis.thesis.phone.authorization.AuthorizationUtils;
import com.davidmis.thesis.phone.pseudonym.AliasGenerator;
import com.davidmis.thesis.phone.pseudonym.PersonIdentity;
import com.example.alice.R;


public class MainActivity extends Activity {
	
	private final String LOG_TAG = "Alice-log";
	
	public static PersonIdentity AliceIdentity = new PersonIdentity("Alice");
	private PersonIdentity BobIdentity = new PersonIdentity("Bob");
	
	private PersonIdentity myIdentity;
	private ConcurrentHashMap<Long, byte[]> ticketList;
	
	private Button callButton;
	private RadioGroup myIdGroup;
	private RadioGroup contactsGroup;
	
	private CallServerThread callServerThread;
	
	private boolean registered = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);		

		/* Set up UI */
		callButton = (Button) findViewById(R.id.callButton);
		callButton.setEnabled(false);
		callButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				
				String calleeID = null;
				String calleeName = null;
				switch (contactsGroup.getCheckedRadioButtonId()) {
				case R.id.contactsAlice:
					calleeID = bytesToHex(AliceIdentity.getID(System.currentTimeMillis()));
					calleeName = AliceIdentity.getName();
					break;
				case R.id.contactsBob:
					calleeID = bytesToHex(BobIdentity.getID(System.currentTimeMillis()));
					calleeName = AliceIdentity.getName();
					break;
				default:
					break;
				}
				
				Intent callIntent = new Intent(getApplicationContext(), CallActivity.class);
				callIntent.putExtra("PEER_ID", calleeID);
				callIntent.putExtra("PEER_NAME", calleeName);
				callIntent.putExtra("MY_NAME", myIdentity.getName());
				callIntent.putExtra("MY_ID", bytesToHex(myIdentity.getID(System.currentTimeMillis())));
				callIntent.putExtra("MY_IP", getIPAddress(true));
				callIntent.putExtra("INCOMING_CALL", false);
				callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //Do I need this?
				startActivity(callIntent);
			}
		});
		
		myIdGroup = (RadioGroup) findViewById(R.id.myIdGroup);
		myIdGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			public void onCheckedChanged(RadioGroup group, int checkedId){
				if(checkedId == R.id.idAlice) {
					myIdentity = AliceIdentity;
				} else if(checkedId == R.id.idBob) {
					myIdentity = BobIdentity;
				}
				
				callButton.setEnabled(true);
				
		//		ticketList = AuthenticationUser.buildTicketList(myIdentity); 
				
		//		register(bytesToHex(myIdentity.getID(System.currentTimeMillis())), getIPAddress(true));
				
		//		Log.i(LOG_TAG, "Here");

				
				if(!registered)
					startRegisterThread();
			}			
		}); 
		
		contactsGroup = (RadioGroup) findViewById(R.id.contactsGroup);
		
		/* Start listening for calls */
		callServerThread = new CallServerThread(getApplicationContext(), getIPAddress(true));
		callServerThread.start();
	}
	
	protected void onStart() {
		super.onStart();		
	}
	
	protected void onResume() {
		super.onResume();	
	}

	
	protected void onPause() {
		super.onPause();
	}
	
	protected void onDestroy() {
		this.unregister();		
		super.onDestroy();
	}

	/* Assumes MS is already registered.
	 * Periodically updates this phones's ID with the network. */
	private void startRegisterThread() {
		
		Log.i(LOG_TAG, "Starting Register Thread");
		registered = true;	
		
		new Thread(new Runnable () {
			public void run() {
				
				
				ticketList = AuthorizationUtils.buildTicketList(myIdentity, 1);
				
				/* Performance Test 2 */
				
				//ArrayList<Long> total_times = new ArrayList<Long>();
				//ArrayList<Long> crypto_times = new ArrayList<Long>();

				
				/*for(int num_days = 1; num_days < 31; num_days++) {
					PersonIdentity randomID = PersonIdentity.randomId();
					
					long start_time = System.currentTimeMillis();
					ticketList = AuthenticationUser.buildTicketList(randomID, num_days);
					long end_time = System.currentTimeMillis();
					
					Log.i("GT-total", "#Days: " + num_days + ", time: " +
							Long.toString(end_time - start_time));
					
				//	total_times.add(end_time - start_time);
					
				//	Log.i("GT-total", "#Days done: " + num_days);
				}
				
				System.exit(0);
				/* End Test */
				
				
				Log.i(LOG_TAG, "GrantTicket Done, initial register");
				
				long updateTime = AliasGenerator.getLastUpdateTime(myIdentity.getTimingSecret(), 
						System.currentTimeMillis());
				String ticket = (new BigInteger(ticketList.get(updateTime))).toString();
				register(bytesToHex(myIdentity.getID(updateTime)), Long.toString(updateTime), ticket, getIPAddress(true));
				
				while(true) {
					long aliasUpdateTime = AliasGenerator.getNextUpdateTime(myIdentity.getTimingSecret(),
							System.currentTimeMillis());
					
					if(!ticketList.containsKey(aliasUpdateTime)) {
						Log.e(LOG_TAG, "No ticket!");
					}
					
					ticket = (new BigInteger(ticketList.get(aliasUpdateTime))).toString();
					
					while (aliasUpdateTime > System.currentTimeMillis()) {
						try {
							Thread.sleep(aliasUpdateTime - System.currentTimeMillis());
						} catch (InterruptedException e) { }
					}
					
					register(bytesToHex(myIdentity.getID(aliasUpdateTime)), Long.toString(aliasUpdateTime), ticket, getIPAddress(true));
				}
			}				
		}).start();
	}
	
	/* Registers this phone with the server */
	private void register(String id, String time, String ticket, String ip) {
		
		final String myID = id;
		final String myIP = ip;
		final String myTime = time;
		final String myTicket = ticket;
		
		new Thread(new Runnable () {
			public void run() {
				Socket s = null;
				try {
					s = new Socket(Telephony.SERVER_IP, Telephony.SERVER_PORT);
					
					Log.i(LOG_TAG, "Registering Phone");
					
					PrintWriter out = new PrintWriter(s.getOutputStream(), true);
					out.println(Telephony.UPDATE_ALIAS);
					out.println(myID);
					out.println(myTime);	 	// current_time
					out.println(myTicket);		// ticket
					out.println(myIP);				
				} catch (IOException e) {
					Log.e(LOG_TAG, "Error registering: " + e.getLocalizedMessage());
				} finally {
					try {
						s.close();	
					} catch (IOException e) {
						Log.e(LOG_TAG, "Couldn't close s!: " + e.getLocalizedMessage());
					} catch (NullPointerException e) {
						Log.e(LOG_TAG, "Couldn't connect, maybe server is down?: " + e.getLocalizedMessage());
					}
					
	//				if(!registered) {
	//					startRegisterThread();
	//				}
					
	//				registered = true;
				}	
			}				
		}).start();
		
		callServerThread.setMyName(myIdentity.getName());
	}
	
	/* TODO Unregisters this phone with the server */
	private void unregister() {
		
	}
	
	/* !!!!!!!!!!!!!!!!!!! */
	/* !!! NOT MY CODE !!! */
	/* !!!!!!!!!!!!!!!!!!! */
	/* http://stackoverflow.com/questions/6064510/how-to-get-ip-address-of-the-device */
	public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr); 
                        if (useIPv4) {
                            if (isIPv4) 
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 port suffix
                                return delim<0 ? sAddr : sAddr.substring(0, delim);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
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
