package android.greentotalk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class ContactListListenerService extends Service {

	private static final int ONGOING_NOTIFICATION = 1;
	private static final int GREEN_NOTIFICATION_ID = 2;
	private static final String TAG = "ContactListListenerService";
	public static final String START_UPDATE_CONTACT_LIST = "android.greentotalk.START_UPDATE_CONTACT_LIST";
	public static final String STOP_UPDATE_CONTACT_LIST = "android.greentotalk.STOP_UPDATE_CONTACT_LIST";
	public static final String CONNECTION_TYPE_MOBILE="MOBILE";
	public static final String CONNECTION_TYPE_WIFI="WIFI";

	private boolean mForegroundStateEnabled = false;
	private static boolean isWatching = false;
	private static Object watchingLock = new Object();
	private Map<String, String> mSelectedContacts;
	private SynchronizedConnectionManager mConnectionManager;
	private SharedPreferences mSettings;
	private SharedPreferences mSavedSelectedContacts;
	private Object mTryingReconnectLock;
	private boolean mIsTryingReconnect;
	private SelectedContactsListener mSelectedContactsListener;
	private UpdateContactListListener mUpdateContactListListener;
	private Object mAddRemoveListenersLock;
	private String mPrevConnectionType;
	private ClearNotificationTask mClearNotificationTask;
	private Handler mClearNotificationHandler;

	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (mConnectionManager.isDisconnecting()) {
				return;
			}
			NetworkInfo info = getNetworkInfo(ContactListListenerService.this);
			boolean isConnected = isConnectedToInternet(info);
			Log.i(TAG, "onReceive: Connection status is "+isConnected+", mPrevConnectionType="+mPrevConnectionType);
			if (isConnected) {
				synchronized (mTryingReconnectLock) {
					if (mIsTryingReconnect) {
						return;
					}
					if (connectionTypeSwitch(info, mPrevConnectionType)) {
						Log.i(TAG, "onReceive: *** CONNECTION TYPE SWITCH: from "+mPrevConnectionType+" to "+info.getTypeName());
						mPrevConnectionType = info.getTypeName();
						removeListeners();
						mConnectionManager.removeOldConnection();
					}
				}
				tryReconnect();
			}
			else {
				removeListeners();
				mConnectionManager.removeOldConnection();
				Intent i = new Intent(PickContactsActivity.UPDATE_LIST_BROADCAST);
				i.putExtra(PickContactsActivity.UPDATE_LIST_CONTENT, true);
				sendBroadcast(i);
				synchronized (watchingLock) {
					if (!isWatching) {
						ContactListListenerService.this.stopSelf();
					}
				}
			}
		}
	};

	@Override
	public void onCreate() {
		Log.i(TAG, "onCreate...");
		mAddRemoveListenersLock = new Object();
		mTryingReconnectLock = new Object();
		mIsTryingReconnect = false;
		isWatching = false;
		mSavedSelectedContacts = getSharedPreferences(PickContactsActivity.SAVED_SELECTED_CONTACTS, MODE_PRIVATE);
		GreenToTalkApplication application= (GreenToTalkApplication)getApplication();
		mSettings = PreferenceManager.getDefaultSharedPreferences(application);
		mConnectionManager = SynchronizedConnectionManager.getInstance();
		mSelectedContacts = new HashMap<String, String>(); // need even empty map before adding roster listener
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		NetworkInfo info = getNetworkInfo(this);
		mPrevConnectionType = (info == null)? mPrevConnectionType : info.getTypeName();
		registerReceiver(mBroadcastReceiver, intentFilter);
		addListeners();
		mClearNotificationTask = new ClearNotificationTask();
		mClearNotificationHandler = new Handler();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "Received intent: " + intent);
		mForegroundStateEnabled = mSettings.getBoolean(GreenToTalkApplication.ONGOING_NOTIFICATION, false);
		if (intent == null) {
			Log.i(TAG, "onStartCommand: not a foreground");
			// we get here after android stopped this service and now restarting it
			if (!mConnectionManager.isConnected()) {
				Log.i(TAG, "onStartCommand: not a foreground and trying to reconnect...");
				tryReconnect();
			}
		}
		else {
			if (intent.getBooleanExtra(START_UPDATE_CONTACT_LIST, false)) {
				mConnectionManager.addRosterListener(mUpdateContactListListener);
				Log.i(TAG, "Added mUpdateContactListListener");
				return START_STICKY;
			}
			else if (intent.getBooleanExtra(STOP_UPDATE_CONTACT_LIST, false)) {
				mConnectionManager.removeRosterListener(mUpdateContactListListener);
				Log.i(TAG, "Removed mUpdateContactListListener");
				return START_STICKY;
			}
		}
		@SuppressWarnings("unchecked")
		Map<String, String> selectedContacts = (Map<String, String>)(mSavedSelectedContacts.getAll());
		synchronized (this) {
			mSelectedContacts = selectedContacts;
		}
		if (selectedContacts.isEmpty()) {
			stopWatching();
			return START_STICKY;
		}
		Notification notification = getForegroundNotification(getNamesString(null), true);
		startWatching(notification);
		return START_STICKY;
	}

	private Notification getForegroundNotification(String names, boolean isStarted) {
		String operation = (isStarted)? "Start" : "Continue";
		Notification notification = new Notification(R.drawable.binoculas_watching, operation+" watching "+names,
				System.currentTimeMillis());
		Intent notificationIntent = new Intent(this, PickContactsActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(this, "Green To Talk",
				"Watching "+names, pendingIntent);
		return notification;
	}

	private String getNamesString(String excludeName) {
		String names = "";
		List<String> list = new ArrayList<String>(mSelectedContacts.values());
		Collections.sort(list, new Comparator<String>() {
			@Override
			public int compare(String lhs, String rhs) {
				return lhs.toLowerCase().compareTo(rhs.toLowerCase());
			}
		});
		for (String name: list) {
			if (!name.equals(excludeName)) {
				int index = name.indexOf(' ');
				if (index > -1) {
					name = name.substring(0, index);
				}
				else if ((index = name.indexOf('@')) > -1) {
					name = name.substring(0, index);
				}
				names += name+", ";
			}
		}
		return names.substring(0, names.length()-2);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "onDestroy...");
		removeListeners();
		unregisterReceiver(mBroadcastReceiver);
		isWatching = false;
		if (!mForegroundStateEnabled && !mConnectionManager.isDisconnecting()) {
			if (isConnectedToInternet(this)) {
				Log.i(TAG, "onDestroy: real disconnection");
				mConnectionManager.disconnect();
			}
			else {
				Log.i(TAG, "onDestroy: work around disconnection");
				mConnectionManager.removeOldConnection();
			}
		}
		Log.i(TAG, "onDestroy... Done");
	}

	private void makeAndroidNotification(String email, Mode mode) {
		String name = mSelectedContacts.get(email);
		String presenceStr = "available";
		final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		int icon = R.drawable.logo2;
		if (mode == Presence.Mode.dnd) {
			icon = R.drawable.red_b;
		}
		CharSequence tickerText = name + " is " + presenceStr;
		long when = System.currentTimeMillis();

		final Notification notification = new Notification(icon, tickerText, when);

		Context context = getApplicationContext();
		CharSequence contentTitle = "Green To Talk";
		CharSequence contentText = name + " is " + presenceStr;
		Intent notificationIntent = new Intent(this, GreenActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		if (makeSound()) {
			String chosenSound = mSettings.getString(PickContactsActivity.NOTIFICATION_SOUND_URI, null);
			if (chosenSound==null) {
				notification.defaults |= Notification.DEFAULT_SOUND;
			}
			else {
				notification.sound = Uri.parse(chosenSound);
			}
		}
		if (vibrate()) {
			notification.defaults |= Notification.DEFAULT_VIBRATE;
		}
		notification.defaults |= Notification.DEFAULT_LIGHTS;
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		mClearNotificationHandler.removeCallbacks(mClearNotificationTask);
		notificationManager.notify(GREEN_NOTIFICATION_ID, notification);
		if (autoClearNotification()) {
			mClearNotificationHandler.postDelayed(mClearNotificationTask, 30000);
		}

		if (mForegroundStateEnabled && mSelectedContacts.size() > 1)
			notificationManager.notify(ONGOING_NOTIFICATION, getForegroundNotification(getNamesString(name), false));				
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	boolean isDndAsAvailable() {
		return mSettings.getBoolean(GreenToTalkApplication.DND_AS_AVAILABLE_KEY, false);
	}

	boolean makeSound() {
		return mSettings.getBoolean(GreenToTalkApplication.MAKE_SOUND_KEY, false);
	}

	boolean vibrate() {
		return mSettings.getBoolean(GreenToTalkApplication.VIBRATE_KEY, false);
	}

	boolean autoClearNotification() {
		return mSettings.getBoolean(GreenToTalkApplication.AUTO_CLEAR_NOTIFICATION, false);
	}

	static boolean isConnectedToInternet(Context c) {
		return isConnectedToInternet(getNetworkInfo(c));
	}


	static boolean isConnectedToInternet(NetworkInfo info) {
		if (info!=null)
			Log.i(TAG, "getExtraInfo="+info.getExtraInfo()+", getReason="+info.getReason()+", getSubtypeName="+info.getSubtypeName()+
					", getTypeName="+info.getTypeName()+", isAvailable="+info.isAvailable()+", isConnected="+info.isConnected()+", isConnectedOrConnecting="+info.isConnectedOrConnecting()
					+", isFailover="+info.isFailover()+", isRoaming="+info.isRoaming());
		return info != null && info.isConnected();
	}

	static boolean connectionTypeSwitch(NetworkInfo info, String prevType) {
		return info == null || (CONNECTION_TYPE_MOBILE.toLowerCase().equals(info.getTypeName().toLowerCase()) && 
				CONNECTION_TYPE_WIFI.toLowerCase().equals(prevType.toLowerCase())  ||
				CONNECTION_TYPE_WIFI.toLowerCase().equals(info.getTypeName().toLowerCase()) && 
				CONNECTION_TYPE_MOBILE.toLowerCase().equals(prevType.toLowerCase()));
	}

	static NetworkInfo getNetworkInfo(Context c) {
		ConnectivityManager connectivityManager = (ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);
		return connectivityManager.getActiveNetworkInfo();
	}

	private void addListeners() {
		synchronized (mAddRemoveListenersLock) {
			mSelectedContactsListener = new SelectedContactsListener(this);
			mUpdateContactListListener = new UpdateContactListListener(this);
			mConnectionManager.addRosterListener(mSelectedContactsListener);
		}
	}

	private void removeListeners() {
		synchronized (mAddRemoveListenersLock) {
			mConnectionManager.removeRosterListener(mSelectedContactsListener);
			mConnectionManager.removeRosterListener(mUpdateContactListListener);
		}
	}

	private void tryReconnect() {
		if (mConnectionManager.isDisconnecting()) {
			return;
		}
		synchronized (mTryingReconnectLock) {
			Log.i(TAG, "tryReconnect: mConnectionManager.isConnected="+mConnectionManager.isConnected()+", mIsTryingReconnect="+mIsTryingReconnect);
			if (!mConnectionManager.isConnected()  &&  !mIsTryingReconnect) {
				mIsTryingReconnect = true;
				boolean isConnected = ContactListListenerService.isConnectedToInternet(ContactListListenerService.this);
				if (isConnected) {
					String username = mSettings.getString(GreenToTalkApplication.ACCOUNT_USERNAME_KEY, "");
					String password = mSettings.getString(GreenToTalkApplication.ACCOUNT_PASSWORD_KEY, "");
					if (mConnectionManager.connectAndRetry(username, password)) {
						addListeners();
					}
				}
				mIsTryingReconnect = false;
			}
		}
	}

	void handleSelectedContact(Presence presence, String email) {
		//		Log.i(this.getClass().getName(),"Presence changed for SELECTED: " + email + " " + presence);
		if (presence.getType() == Presence.Type.available  &&  
				(presence.getMode() == null  ||  presence.getMode() == Presence.Mode.available  ||  
				(presence.getMode() == Presence.Mode.dnd  &&  isDndAsAvailable()))) {
			makeAndroidNotification(email, presence.getMode());
			mSelectedContacts.remove(email);
			final Intent intent = new Intent(PickContactsActivity.UPDATE_LIST_BROADCAST);
			intent.putExtra(PickContactsActivity.UNSELECT_CONTACT, true);
			intent.putExtra(Contact.EMAIL, email);
			Editor editor = mSavedSelectedContacts.edit();
			editor.remove(email);
			editor.apply();
			sendBroadcast(intent);
			Log.i(TAG,"SENT BROADCAST FOR EMAIL "+email);
		}
		if (mSelectedContacts.isEmpty()) {
			stopWatching();
		}
	}

	public boolean isSelectedContact(String email) {
		return mSelectedContacts.containsKey(email);
	}

	private void startWatching(Notification notification) {
		synchronized (watchingLock) {
			if (mForegroundStateEnabled) {
				startForeground(ONGOING_NOTIFICATION, notification);
			}
			else {
				stopForeground(true);
			}
			isWatching = true;
		}
	}

	private void stopWatching() {
		synchronized (watchingLock) {
			if (mForegroundStateEnabled) {
				stopForeground(true);
			}
			isWatching = false;
		}
	}

	static boolean isWatching() {
		boolean result;
		synchronized (watchingLock) {
			result = isWatching;
		}
		return result;
	}

	private class ClearNotificationTask implements Runnable {
		@Override
		public void run() {
			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.cancel(GREEN_NOTIFICATION_ID);
		}
	}
}