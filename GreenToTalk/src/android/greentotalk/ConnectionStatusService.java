package android.greentotalk;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.util.Log;

public class ConnectionStatusService extends Service {

	private static final String TAG = "ConnectionStatusService"; 
	private SynchronizedConnectionManager mConnectionManager;
//	private SharedPreferences mSettings;
	static final String ADD_CONTACTS_LISTENER = "android.greentotalk.ADD_CONTACTS_LISTENER";
	static final String REMOVE_CONTACTS_LISTENER = "android.greentotalk.REMOVE_CONTACTS_LISTENER";
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	boolean isConnected = isConnectedToInternet(context);
        	Log.i(TAG, "Connection status is "+isConnected);
//        	if (isConnected  &&  !mConnectionManager.isConnected()) {
//        		String username = mSettings.getString(GreenToTalkApplication.ACCOUNT_USERNAME_KEY, "");
//        		String password = mSettings.getString(GreenToTalkApplication.ACCOUNT_PASSWORD_KEY, "");
//        		new AsyncConnectionTask(ConnectionStatusService.this, false).execute(username, password);
//        	}
        	if (!isConnected){
        		sendBroadcast(REMOVE_CONTACTS_LISTENER);
        		stopService(new Intent(ConnectionStatusService.this, NotificationService.class));
        		mConnectionManager.removeOldConnection();
        		stopSelf();
        	}
        }
    };
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "onCreate...");
		mConnectionManager = SynchronizedConnectionManager.getInstance();
//		mSettings = PreferenceManager.getDefaultSharedPreferences((GreenToTalkApplication)getApplication());
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(mBroadcastReceiver, intentFilter);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mBroadcastReceiver);
		Log.i(TAG, "onDestroy...");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}
	    
    static boolean isConnectedToInternet(Context c) {
        ConnectivityManager connectivityManager = (ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        return info != null && info.isConnected();
   }
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	void sendBroadcast(String action) {
		final Intent intent = new Intent(PickFreindsActivity.UPDATE_LIST_BROADCAST);
		intent.putExtra(action, true);
		sendBroadcast(intent);
//		if (context instanceof RosterListener) {
//			if (action.equals(ADD_CONTACTS_LISTENER))
//				SynchronizedConnectionManager.getInstance().addRosterListener((RosterListener) context);
//			else if (action.equals(REMOVE_CONTACTS_LISTENER))
//				SynchronizedConnectionManager.getInstance().removeRosterListener((RosterListener) context);
//		}
		Log.i(TAG, "sent broadcast to PickFreindsActivity for action "+action);
	}

}
