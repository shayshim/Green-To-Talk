package android.greentotalk;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class NotificationService extends Service implements RosterListener {

	private static final int GREEN_NOTIFICATION_ID = 1;
	private static final String SAVED_CONTACT_TO_FOLLOW = "saved_contact_to_follow";
	private static final String TAG = "NotificationService";
	private SharedPreferences mSettings;
	private SharedPreferences mSavedContactsToFollow;
	private Map<String, String> mFollowedMembers;
	private SynchronizedConnectionManager mConnectionMgr;
	private Handler mHandler;

	@SuppressWarnings("unchecked")
	@Override
	public void onCreate() {
		mHandler = new Handler(); // created in the main thread
		GreenToTalkApplication application= (GreenToTalkApplication)getApplication();
		mSettings = PreferenceManager.getDefaultSharedPreferences(application);
		mSavedContactsToFollow = getSharedPreferences(SAVED_CONTACT_TO_FOLLOW, MODE_PRIVATE);
		// load members in hashtable from shared preferences from previous time (maybe was killed by os)
		mFollowedMembers  = (Map<String, String>) Collections.synchronizedMap(mSavedContactsToFollow.getAll());
		Log.i(TAG, "contacts to follow: "+mFollowedMembers.size());
		Editor editor = mSavedContactsToFollow.edit();
		editor.clear();
		editor.commit();
		mConnectionMgr = SynchronizedConnectionManager.getInstance();
		if (!mConnectionMgr.isConnected()) {
			String username = mSettings.getString(GreenToTalkApplication.ACCOUNT_USERNAME_KEY, null)+"@"+SigninActivity.GMAIL_DOMAIN;
			String password = mSettings.getString(GreenToTalkApplication.ACCOUNT_PASSWORD_KEY, null);
			mConnectionMgr.connect(username, password);
		}
		mConnectionMgr.addRosterListener(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("NotificationService", "Received start id " + startId + ": " + intent);
		if (intent != null) {
			if (intent.getBooleanExtra(SigninActivity.ONLY_START_SERVICE, false)) {
				// service started only to check and extract saved contacts (in onCreate)
				if (mFollowedMembers.isEmpty()) {
					stopSelf();
				}
			}
			else {
				boolean select = intent.getBooleanExtra(PickFreindsActivity.SELECT_CONTACT, false);
				if (select) {
					mFollowedMembers.put(intent.getStringExtra(PickFreindsActivity.EMAIL_FIELD), 
						intent.getStringExtra(PickFreindsActivity.NAME_FIELD));
				}
				else {
					mFollowedMembers.remove(intent.getStringExtra(PickFreindsActivity.EMAIL_FIELD));
				}
			}
		}
		// We want this service to continue running until it is explicitly stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "Destroyed...");
		mConnectionMgr.removeRosterListener(this);
		// save members in hashtable in shared preferences for next time
		Editor editor = mSavedContactsToFollow.edit();
		for (String email: mFollowedMembers.keySet()) {
			editor.putString(email, mFollowedMembers.get(email));
		}
		editor.commit();
		Log.i(TAG, "mSavedContactsToFollow="+mSavedContactsToFollow);
	}

	private void makeAndroidNotification(String name) {
		String presenceStr = "available";
		String ns = Context.NOTIFICATION_SERVICE;
		final NotificationManager notificationManager = (NotificationManager) getSystemService(ns);
		int icon = R.drawable.logo2;
		CharSequence tickerText = name + " is " + presenceStr;
		long when = System.currentTimeMillis();

		final Notification notification = new Notification(icon, tickerText, when);

		Context context = getApplicationContext();
		CharSequence contentTitle = "Green To Talk";
		CharSequence contentText = name + " is " + presenceStr;
		Intent notificationIntent = new Intent(this, GreenActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		notification.defaults |= Notification.DEFAULT_SOUND;
		notification.defaults |= Notification.DEFAULT_VIBRATE;
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				notificationManager.notify(GREEN_NOTIFICATION_ID, notification);
			}
		});
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	boolean isDndAsAvailable() {
		return mSettings.getBoolean(GreenToTalkApplication.DND_AS_AVAILABLE_KEY, false);
	}

	@Override
	public void entriesAdded(Collection<String> arg0) {}

	@Override
	public void entriesDeleted(Collection<String> arg0) {}

	@Override
	public void entriesUpdated(Collection<String> arg0) {}

	@Override
	public void presenceChanged(Presence presence) {
		String email = StringUtils.parseBareAddress(presence.getFrom()); 
		if (mFollowedMembers.containsKey(email)) {
			Log.i(this.getClass().getName(),"Presence changed: " + presence.getFrom() + " " + presence);
			if (presence.getType() == Presence.Type.available  &&  
					(presence.getMode() == null  ||  presence.getMode() == Presence.Mode.available  ||  
					(isDndAsAvailable() && presence.getMode() == Presence.Mode.dnd))) {
				makeAndroidNotification(mFollowedMembers.get(email));
				mFollowedMembers.remove(email);
				final Intent intent = new Intent(PickFreindsActivity.UPDATE_LIST_BROADCAST);
				intent.putExtra(PickFreindsActivity.SELECT_CONTACT, true);
				intent.putExtra(Contact.EMAIL, email);
				sendBroadcast(intent);
			}
		}
		if (mFollowedMembers.isEmpty()) {
			stopSelf();
		}
	}
}
