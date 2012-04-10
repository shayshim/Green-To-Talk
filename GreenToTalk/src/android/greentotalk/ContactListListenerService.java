package android.greentotalk;

import java.util.Collection;

import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ContactListListenerService extends Service implements RosterListener {
	
	public static final String PRESENCE_TYPE_KEY = "presence_type";
	public static final String PRESENCE_MODE_KEY = "presence_mode";
	public static final String PRESENCE_EMAIL_KEY = "presence_email";
	public static final String PRESENCE_PRIORITY_KEY = "presence_priority";
	private static final String TAG = "BroadcastService";

	@Override
	public void onCreate() {
		super.onCreate();
		SynchronizedConnectionManager.getInstance().addRosterListener(this);
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}

	@Override
	public void entriesAdded(Collection<String> arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void entriesDeleted(Collection<String> arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void entriesUpdated(Collection<String> arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		SynchronizedConnectionManager.getInstance().removeRosterListener(this);
	}

	@Override
	public void presenceChanged(final Presence p) {
		final Intent intent = new Intent(PickFreindsActivity.UPDATE_LIST_BROADCAST);
		intent.putExtra(Contact.EMAIL, StringUtils.parseBareAddress(p.getFrom()));
		Log.d(TAG, "sendUpdatesToUI: from="+p.getFrom()+", type="+p.getType()+", mode="+p.getMode()+", priority="+p.getPriority());
		sendBroadcast(intent);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
