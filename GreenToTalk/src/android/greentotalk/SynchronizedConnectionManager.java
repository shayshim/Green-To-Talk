package android.greentotalk;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;

import android.util.Log;

public class SynchronizedConnectionManager {
	
	private static SynchronizedConnectionManager instance = null;
	private XMPPConnection mConnection;
	private String mUsername;
	
	
	private SynchronizedConnectionManager() {}
	
	private XMPPConnection getNewConnection() {
		ConnectionConfiguration config = new ConnectionConfiguration("talk.google.com", 5222, "gmail.com");
		config.setTruststoreType("BKS");
		config.setTruststorePath("/system/etc/security/cacerts.bks");
		config.setSendPresence(false);
		XMPPConnection connection = new XMPPConnection(config);
		return connection;
	}
	
	public static synchronized SynchronizedConnectionManager getInstance() {
		// first call is always made from main thread, so no need to synchronize here 
		if (instance == null) {
			instance = new SynchronizedConnectionManager();
		}
		return instance;
	}
	
	public synchronized boolean connect(String username, String password) {
		mUsername = username;
		mConnection = getNewConnection();
		try {
			// Connect to the server
			mConnection.connect();
			mConnection.addConnectionListener(new ConnectionListener() {
				 
	            @Override
	            public void reconnectionSuccessful() {
	                Log.i("ThreadSafeConnectionHandler", "Successfully reconnected to the XMPP server.");
	            }
	            
	            @Override
	            public void reconnectionFailed(Exception arg0) {
	            	Log.i("ThreadSafeConnectionHandler", "Failed to reconnect to the XMPP server.");
	            }
	 
	            @Override
	            public void reconnectingIn(int seconds) {
	            	Log.i("ThreadSafeConnectionHandler", "Reconnecting in " + seconds + " seconds.");
	            }
	            
	            @Override
	            public void connectionClosedOnError(Exception arg0) {
	            	Log.i("ThreadSafeConnectionHandler", "Connection to XMPP server was lost.");
	            }
	            
	            @Override
	            public void connectionClosed() {
	            	Log.i("ThreadSafeConnectionHandler", "XMPP connection was closed.");
	            }
	        });
			// Log into the server
			mConnection.login(username, password);
		}
		catch (XMPPException e1) {
			e1.printStackTrace();
			return false;
		}
		catch (IllegalStateException e1) {
			e1.printStackTrace();
			return false;
		}
		return true;
	}
	
	public synchronized void disconnect() {
		if (mConnection != null) {
			mConnection.disconnect();
		}
	}
 
	public synchronized Roster getRoster() {
		assert(mConnection!=null);
		return mConnection.getRoster();
	}
	
	public synchronized void addRosterListener(RosterListener rl) {
		assert(mConnection!=null);
		mConnection.getRoster().addRosterListener(rl);
	}

	public synchronized void sendPacket(Presence p) {
		assert(mConnection!=null);
		mConnection.sendPacket(p);
	}

	public boolean isConnected() {
		return mConnection!=null && mConnection.isAuthenticated();
	}

	public synchronized void removeRosterListener(RosterListener rl) {
		assert(mConnection!=null);
		Roster r = mConnection.getRoster();
		if (r != null)
			r.removeRosterListener(rl);
	}	
	
	public String getUsername() {
		return mUsername;
	}
}
