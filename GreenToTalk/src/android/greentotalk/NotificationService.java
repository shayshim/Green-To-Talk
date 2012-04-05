package android.greentotalk;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;

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
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class NotificationService extends Service implements RosterListener {

	private static final int GREEN_NOTIFICATION_ID = 1;
	private static final String COUNTER_CONTACTS_TO_FOLLOW = "counter_contacts_to_follow";
	private static final String SAVED_CONTACT_TO_FOLLOW = "saved_contact_to_follow";
	private static final String TAG = "NotificationService";
	private SharedPreferences mSettings;
	private SharedPreferences mSavedContactsToFollow;
	private Hashtable<String, String> mFollowedMembers;
	private SynchronizedConnectionManager mConnectionMgr;
	 
	@Override
	public void onCreate() {
		mFollowedMembers = new Hashtable<String, String>();
		GreenToTalkApplication application= (GreenToTalkApplication)getApplication();
		mSettings = PreferenceManager.getDefaultSharedPreferences(application);
		mSavedContactsToFollow = getSharedPreferences(SAVED_CONTACT_TO_FOLLOW, MODE_PRIVATE);
		// load members in hashtable from shared preferences from previous time (maybe was killed by os)
		int counter = mSavedContactsToFollow.getInt(COUNTER_CONTACTS_TO_FOLLOW, 0);
		Log.i(TAG, "contacts to follow: "+counter);
		for (int i=0; i<counter; i++) {
			String[] str = mSavedContactsToFollow.getString(SAVED_CONTACT_TO_FOLLOW+i, null).split(";");
			mFollowedMembers.put(str[0], str[1]);
			Log.i(TAG, "email, name: "+str);
		}
		mSavedContactsToFollow.edit().clear();
		mSavedContactsToFollow.edit().commit();
		mConnectionMgr = SynchronizedConnectionManager.getInstance();
		if (!mConnectionMgr.isConnected()) {
			String username = mSettings.getString(GreenToTalkApplication.ACCOUNT_USERNAME_KEY, null)+"@"+SigninActivity.GMAIL_DOMAIN;
			String password = mSettings.getString(GreenToTalkApplication.ACCOUNT_PASSWORD_KEY, null);
			Log.i(TAG, "username="+username+", password="+password);
			mConnectionMgr.connect(username, password);
		}
		mConnectionMgr.addRosterListener(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("NotificationService", "Received start id " + startId + ": " + intent);
		if (intent != null) {
			mFollowedMembers.put(intent.getStringExtra(getPackageName()+"."+PickFreindsActivity.EMAIL_FIELD), 
					intent.getStringExtra(getPackageName()+"."+PickFreindsActivity.NAME_FIELD));
		}
		// We want this service to continue running until it is explicitly stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "Destroyed...");
		mConnectionMgr.removeRosterListener(this);
		if (!mFollowedMembers.isEmpty()) {
			// save members in hashtable in shared preferences for next time
			Editor editor = mSavedContactsToFollow.edit(); 
			editor.putInt(COUNTER_CONTACTS_TO_FOLLOW, mFollowedMembers.size());
			Enumeration<String> enumerator = mFollowedMembers.keys();
			int i=0;
			while (enumerator.hasMoreElements()) {
				String email = enumerator.nextElement();
				editor.putString(SAVED_CONTACT_TO_FOLLOW+i++, email+";"+mFollowedMembers.get(email));
			}
			editor.commit();
			Log.i(TAG, "mSavedContactsToFollow="+mSavedContactsToFollow);
		}
	}

	private void makeAndroidNotification(String name) {
		String presenceStr = "available";
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		int icon = R.drawable.logo2;
		CharSequence tickerText = name + " is " + presenceStr;
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);

		Context context = getApplicationContext();
		CharSequence contentTitle = "Green To Talk";
		CharSequence contentText = name + " is " + presenceStr;
		Intent notificationIntent = new Intent(this, GreenActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		notification.defaults |= Notification.DEFAULT_SOUND;
		notification.defaults |= Notification.DEFAULT_VIBRATE;
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		mNotificationManager.notify(GREEN_NOTIFICATION_ID, notification);
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
			}
		}
		if (mFollowedMembers.isEmpty()) {
			stopSelf();
		}
	}
}
