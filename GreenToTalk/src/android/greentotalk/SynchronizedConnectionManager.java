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

import android.content.SharedPreferences;
import android.util.Log;

public class SynchronizedConnectionManager {

	private static SynchronizedConnectionManager instance = null;
	private XMPPConnection mConnection;
	private String mUsernameEmail;
	private Roster mRoster;
	boolean mConnected;
	public static final String GMAIL_DOMAIN = "gmail.com";
	public static final String FACEBOOK_DOMAIN = "chat.facebook.com";
	private static final String TAG = "SynchronizedConnectionManager";
	private boolean mDisconnecting;
	private SharedPreferences mSettings;
	private static final int DEFUALT_PORT = 5222;

	private SynchronizedConnectionManager() {
		Log.i(TAG, "--------------------> CREATING NEW  *SynchronizedConnectionManager*  INSTANCE");
		mConnected = false;
		mDisconnecting = false;
	}

	private XMPPConnection getNewConnection() {
		XMPPConnection connection = new XMPPConnection(getNewConnectionConfiguration());
		mConnected = false;
		return connection;
	}
	
	private ConnectionConfiguration getNewConnectionConfiguration() {
		ConnectionConfiguration config = null;
		String serviceName = getServiceName();
		if (serviceName.equals(GreenToTalkApplication.SERVICE_NAME_GOOGLE)) {
			config = new ConnectionConfiguration("talk.google.com", DEFUALT_PORT, "gmail.com");
		}
		else if (serviceName.equals(GreenToTalkApplication.SERVICE_NAME_FACEBOOK)) {
			config = new ConnectionConfiguration("chat.facebook.com", DEFUALT_PORT);
			config.setSASLAuthenticationEnabled(true);
		}
		config.setTruststoreType("BKS");
		config.setTruststorePath("/system/etc/security/cacerts.bks");
		config.setSendPresence(false);
		return config;
	}
	
	private String getServiceName() {
		return mSettings.getString(GreenToTalkApplication.SERVICE_NAME, GreenToTalkApplication.SERVICE_NAME_GOOGLE);
	}
	
	public void setDisconnecting(boolean disconnecting) {
		mDisconnecting = disconnecting;
	}
	
	public boolean isDisconnecting() {
		return mDisconnecting;
	}

	public synchronized boolean connectAndRetry(String username, String password) {
		int retries = 3;
		boolean result = false;
		while (!result  &&  retries > 0) {
			result = connectUnsync(username, password);
			Log.i(TAG, "after connect result is "+result);
			retries--;
		}
		if (!result)
			return false;
		Presence p = new Presence(Presence.Type.available);
		p.setMode(Presence.Mode.away);
		p.setPriority(-127);
		sendPacket(p);
		return true;
	}

	public static SynchronizedConnectionManager getInstance() {
		// first call is always made from main thread, so no need to synchronize here 
		if (instance == null) {
			instance = new SynchronizedConnectionManager();
		}
		return instance;
	}

	public synchronized void addConnectionListener(ConnectionListener listener) {
		if (isConnectedUnsync()) {
			mConnection.addConnectionListener(listener);
		}
	}
	
	public String getDomain() {
		if (getServiceName().equals(GreenToTalkApplication.SERVICE_NAME_GOOGLE)) {
			return GMAIL_DOMAIN;
		}
		if (getServiceName().equals(GreenToTalkApplication.SERVICE_NAME_FACEBOOK)) {
			return FACEBOOK_DOMAIN;
		}
		return null;
	}
	
	private boolean connectUnsync(String username, String password) {
		if (isConnectedUnsync()) {
			return true;
		}
		String serviceName = getServiceName();
		if (serviceName.equals(GreenToTalkApplication.SERVICE_NAME_GOOGLE)) {
			mUsernameEmail = (username.contains("@"))? username : username+"@"+GMAIL_DOMAIN;
		}
		else if (serviceName.equals(GreenToTalkApplication.SERVICE_NAME_FACEBOOK)) {
			mUsernameEmail = (username.contains("@"))? username.substring(0, username.indexOf('@')) : username;
		}
		mConnection = getNewConnection();
		try {
			// Connect to the server
			mConnection.connect();

			// Log into the server
			mConnection.login(mUsernameEmail, password);
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
		if (isConnectedUnsync()) {
			Log.i(TAG, "REAL DISCONNECTION...");
			mConnection.disconnect();
		}
		mConnected = false;
	}

	public synchronized void addRosterListener(RosterListener rl) {
		if (isConnectedUnsync())
			mRoster.addRosterListener(rl);
	}

	public synchronized void sendPacket(Presence p) {
		if (isConnectedUnsync())
			mConnection.sendPacket(p);
	}

	public synchronized boolean isConnected() {
		return isConnectedUnsync();
	}
	
	private boolean isConnectedUnsync() {
//		Log.i(TAG, "mConnected="+mConnected+", mConnection="+mConnection);
//		if (mConnection != null)
//			Log.i(TAG, "mConnection.isConnected()="+mConnection.isConnected()+", mConnection.isAuthenticated()"+mConnection.isAuthenticated());
		return mConnected && mConnection!=null && mConnection.isConnected() && mConnection.isAuthenticated();
	}

	public synchronized void removeRosterListener(RosterListener rl) {
		if (isConnectedUnsync()) {
			mRoster.removeRosterListener(rl);
		}
	}

	public synchronized Collection<RosterEntry> getEntries() {
		if (!isConnectedUnsync())
			return new ArrayList<RosterEntry>();
		return mRoster.getEntries();
	}

	public synchronized String getUsernameEmail() {
		return mUsernameEmail;
	}

	public synchronized Presence getPresence(String email) {
		if (!isConnectedUnsync())
			return null;
		return mConnection.getRoster().getPresence(email);
	}

	public synchronized void removeOldConnection() {
		mConnected = false;
		mConnection = getNewConnection();
	}

	public synchronized void removeConnectionListener(ConnectionListener listener) {
		if (isConnectedUnsync()) {
			mConnection.removeConnectionListener(listener);
		}
	}

	public void setConnected(boolean b) {
		mConnected = false;
	}

	public void setSettings(SharedPreferences settings) {
		mSettings = settings;
	}
}
