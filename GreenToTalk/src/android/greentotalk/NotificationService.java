package android.greentotalk;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class NotificationService extends Service implements RosterListener {

	private static final int GREEN_NOTIFICATION_ID = 1;
	public static final String UNSELECT_CONTACT = "android.greentotalk.UNSELECT_CONTACT";
	private static final String TAG = "NotificationService";
	private Map<String, String> mSelectedContacts;
	private SynchronizedConnectionManager mConnectionMgr;
	private Handler mHandler;
	private SharedPreferences mSettings;
	private static final int ONGOING_NOTIFICATION = 1;
	private static boolean running = false;
	private SharedPreferences mSavedSelectedContacts;
	
	@Override
	public void onCreate() {
		mHandler = new Handler(); // created in the main thread
		mSavedSelectedContacts = getSharedPreferences(ContactsManager.SAVED_SELECTED_CONTACTS, MODE_PRIVATE);
		GreenToTalkApplication application= (GreenToTalkApplication)getApplication();
		mSettings = PreferenceManager.getDefaultSharedPreferences(application);
		mConnectionMgr = SynchronizedConnectionManager.getInstance();
		//		Notification notification = new Notification(R.drawable.binoculas_watching, "Start watching",
		//		        System.currentTimeMillis());
		//		Intent notificationIntent = new Intent(this, GreenActivity.class);
		//		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		//		notification.setLatestEventInfo(this, "Green To Talk",
		//		        "Watching for people ... to be available", pendingIntent);
		//		startForeground(ONGOING_NOTIFICATION, notification);
		mConnectionMgr.addRosterListener(this);
		running = true;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("NotificationService", "Received start id " + startId + ": " + intent);
		if (intent != null) {
			Map<String, String> selectedContacts = new HashMap<String, String>();
			Bundle bundle = intent.getBundleExtra(ContactsManager.SAVED_SELECTED_CONTACTS);
			Set<String> emails = bundle.keySet();  
			for (String email: emails) {
				selectedContacts.put(email, bundle.getString(email));
			}
			if (selectedContacts.isEmpty()) {
				stopSelf();
			}
			mSelectedContacts = selectedContacts;
		}
		// We want this service to continue running until it is explicitly stopped, so return sticky.
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "Destroyed...");
		mConnectionMgr.removeRosterListener(this);
		running = false;
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
		if (mSelectedContacts.containsKey(email)) {
			Log.i(this.getClass().getName(),"Presence changed: " + presence.getFrom() + " " + presence);
			if (presence.getType() == Presence.Type.available  &&  
					(presence.getMode() == null  ||  presence.getMode() == Presence.Mode.available  ||  
					(presence.getMode() == Presence.Mode.dnd  &&  isDndAsAvailable()))) {
				makeAndroidNotification(mSelectedContacts.get(email));
				mSelectedContacts.remove(email);
				final Intent intent = new Intent(PickFreindsActivity.UPDATE_LIST_BROADCAST);
				intent.putExtra(UNSELECT_CONTACT, true);
				intent.putExtra(Contact.EMAIL, email);
				Editor editor = mSavedSelectedContacts.edit();
				editor.remove(email);
				editor.apply();
				sendBroadcast(intent);
				Log.i(TAG,"SENT BROADCAST FOR EMAIL "+email);
			}
		}
		if (mSelectedContacts.isEmpty()) {
			stopSelf();
		}
	}

	public static boolean isServiceRunning() {
		return running;
	}
}
