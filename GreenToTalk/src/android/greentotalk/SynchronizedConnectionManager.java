package android.greentotalk;
import java.util.ArrayList;
import java.util.Collection;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;

import android.util.Log;

public class SynchronizedConnectionManager {

	private static SynchronizedConnectionManager instance = null;
	private XMPPConnection mConnection;
	private String mUsername;
	private Roster mRoster;
	boolean mConnected;
	
	private SynchronizedConnectionManager() {
		mConnected = false;
	}

	private XMPPConnection getNewConnection() {
		ConnectionConfiguration config = new ConnectionConfiguration("talk.google.com", 5222, "gmail.com");
		config.setTruststoreType("BKS");
		config.setTruststorePath("/system/etc/security/cacerts.bks");
		config.setSendPresence(false);
		XMPPConnection connection = new XMPPConnection(config);
		mConnected = false;
		return connection;
	}
	
	boolean connectAndRetry(String username, String password) {
		int retries = 2;
		boolean result = false;
		while (!result  &&  retries > 0) {
			result = SynchronizedConnectionManager.getInstance().connect(username, password);
			retries--;
		}
		if (!result)
			return false;
		Presence p = new Presence(Presence.Type.available);
		p.setMode(Presence.Mode.away);
		p.setPriority(-127);
		SynchronizedConnectionManager.getInstance().sendPacket(p);
		return true;
	}

	public static SynchronizedConnectionManager getInstance() {
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
		mRoster = mConnection.getRoster();
		mConnected = true;
		return true;
	}

	public synchronized void disconnect() {
		mConnected = false;
		mConnection.disconnect();
	}

	public synchronized void addRosterListener(RosterListener rl) {
		if (mConnected)
			mRoster.addRosterListener(rl);
	}

	public synchronized void sendPacket(Presence p) {
		if (mConnected)
			mConnection.sendPacket(p);
	}

	public synchronized boolean isConnected() {
		return mConnected;
	}

	public synchronized void removeRosterListener(RosterListener rl) {
		if (mConnected) {
			mRoster.removeRosterListener(rl);
		}
	}

	public synchronized Collection<RosterEntry> getEntries() {
		if (!mConnected)
			return new ArrayList<RosterEntry>();
		return mRoster.getEntries();
	}

	public synchronized String getUsername() {
		return mUsername;
	}
	
	public synchronized Presence getPresence(String email) {
		if (!mConnected)
			return null;
		return mConnection.getRoster().getPresence(email);
	}

	public void removeOldConnection() {
		mConnected = false;
		mConnection = getNewConnection();
	}
}
