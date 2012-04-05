package android.greentotalk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.jivesoftware.smack.packet.Presence;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class PickFreindsActivity extends ListActivity {

	private static final String TAG = "PickFreindsActivity";
	public static final String NAME_FIELD = "name";
	public static final String MODE_FIELD = "mode";
	public static final String EMAIL_FIELD = "email";
	private static final int DISCONNECT_DIALOG = 0;
	private static final int SETTINGS_DIALOG = 1;
	private static final int ABOUT_DIALOG = 2;
	private static final int FOLLOWED_DIALOG = 3;
	private SimpleAdapter mAdapter;
	private AsyncContactsSupplier mAsyncContactsSupplier;
	private ArrayList<Map<String, String>> mList = new ArrayList<Map<String, String>>();
	private ContactListListenerService mMemberListUpdater;
	private ContactsManager mContactsManager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate...");
		mContactsManager = new ContactsManager();
		mAsyncContactsSupplier = new AsyncContactsSupplier(this, mContactsManager);
		try {
			mAsyncContactsSupplier.execute().get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	public void InitializeUI() {
		try {
			mList = mAsyncContactsSupplier.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		String[] from = { NAME_FIELD, MODE_FIELD, EMAIL_FIELD };
		int[] to = { R.id.label1, R.id.label2, R.id.label3 };
		mAdapter = new SimpleAdapter(this, mList, R.layout.rowlayout, from, to);
		ListView lv = getListView();
		lv.setTextFilterEnabled(true);
		setListAdapter(mAdapter);
	}
	
	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	updateUI(intent);       
        }
    };
	
	@Override
	public void onResume() {
		super.onResume();		
		registerReceiver(broadcastReceiver, new IntentFilter(ContactListListenerService.BROADCAST_ACTION));
		startService(new Intent(this, ContactListListenerService.class));
	}
	
	@Override
	public void onPause() {
		super.onPause();
		stopService(new Intent(this, ContactListListenerService.class));
		unregisterReceiver(broadcastReceiver);
	}	
	    
    private void updateUI(Intent intent) {
    	String email = intent.getStringExtra(ContactListListenerService.PRESENCE_EMAIL_KEY);
    	boolean available = intent.getBooleanExtra(ContactListListenerService.PRESENCE_TYPE_KEY, false);
    	int priority = intent.getIntExtra(ContactListListenerService.PRESENCE_PRIORITY_KEY, 0);
    	Presence.Mode mode = PresenceWrapper.getModeFromInteger(intent.getIntExtra(ContactListListenerService.PRESENCE_MODE_KEY, PresenceWrapper.AVAILABLE));
    	mList = mContactsManager.getUpdatedList(mList, email, available, mode, priority);
    	mAdapter.notifyDataSetChanged();
    	Log.i(TAG, "updateUI for email: "+email+": New list: " + mList);
    }
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		SynchronizedConnectionManager.getInstance().removeRosterListener(mMemberListUpdater);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		ListAdapter la = getListAdapter();
		@SuppressWarnings("unchecked")
		HashMap<String, String> item = (HashMap<String, String>)la.getItem(position);
		Toast.makeText(this, item.get(NAME_FIELD) + " selected", Toast.LENGTH_SHORT).show();

		Intent intent = new Intent(this, NotificationService.class);
		intent.putExtra(getPackageName()+"."+EMAIL_FIELD, item.get(EMAIL_FIELD));
		intent.putExtra(getPackageName()+"."+NAME_FIELD, item.get(NAME_FIELD));
		startService(intent);
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.layout.menu_buttons, menu); 
		return true;
	}

	@Override
	public final boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		boolean result;
		switch (item.getItemId()) {
		case R.id.menu_disconnect:
			showDialog(DISCONNECT_DIALOG);
			result = true;
			break;
		case R.id.menu_settings:
			showDialog(SETTINGS_DIALOG);
			result = true;
			break;
		case R.id.menu_about:
			showDialog(ABOUT_DIALOG);
			result = true;
			break;
		case R.id.menu_followed_freinds:
			showDialog(FOLLOWED_DIALOG);
			result = true;
			break;
		default:
			result = false;
			break;
		}
		return result;
	}

	@Override
	protected Dialog onCreateDialog(int index) {
		super.onCreateDialog(index);
		String versionname;
		try {
			PackageManager pm = getPackageManager();
			PackageInfo pi = pm.getPackageInfo("com.beem.project.beem", 0);
			versionname = pi.versionName;
		} catch (PackageManager.NameNotFoundException e) {
			versionname = "";
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		String title = "";
		final GreenToTalkApplication application= (GreenToTalkApplication)getApplication();
		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(application);
		switch(index) {
		case SETTINGS_DIALOG:
			title = getString(R.string.settings_title, versionname);
			builder.setTitle(title);
			builder.setIcon(R.drawable.available_icon);
			final String[] strings = new String[]{"Treat \"Busy\" as \"Available\""}; 
			final OnMultiChoiceClickListener onClick = new PreferencesOnMultiChoiceClickListener();
			boolean [] choices = {settings.getBoolean(GreenToTalkApplication.DND_AS_AVAILABLE_KEY, false)};
			builder.setMultiChoiceItems(strings, choices, onClick);
			builder.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					dialog.cancel();
				}
			});
			break;
		case ABOUT_DIALOG:
			title = getString(R.string.about_title, versionname);
			builder.setTitle(title).setMessage(R.string.about_msg);
			builder.setIcon(R.drawable.available_icon);
			builder.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					dialog.cancel();
				}
			});
			break;
		case FOLLOWED_DIALOG:
			builder.setTitle("Monitored freinds").setMessage("1\n2\n3");
			builder.setIcon(R.drawable.available_icon);
			builder.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					dialog.cancel();
				}
			});
			break;
		case DISCONNECT_DIALOG:
			title = "Disconnect";
			builder.setTitle(title).setMessage("Are you sure you want to disconnect?");
			builder.setIcon(R.drawable.available_icon);
//			final String[] strings2 = new String[]{"Also delete username and password and restore default preferences"};
//			final boolean [] choices2 = {false};
//			builder.setMultiChoiceItems(strings2, choices2, new OnMultiChoiceClickListener() {
//				@Override
//				public void onClick(DialogInterface dialog, int which,
//						boolean isChecked) {
//					choices2[0] = isChecked;
//				}
//			});
			builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					SynchronizedConnectionManager.getInstance().removeRosterListener(mMemberListUpdater);
					new AsyncDisconnectionTask(PickFreindsActivity.this).execute((Void[])null);
//					if (choices2[0]) {
//						settings.edit().clear();
//						settings.edit().commit();
//					}
					dialog.dismiss();
				}
			});
			builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					dialog.cancel();
				}
			});
			break;
		}
		AlertDialog dialog = builder.create();
		return dialog;
	}

	private class PreferencesOnMultiChoiceClickListener implements OnMultiChoiceClickListener {
		private final SharedPreferences.Editor mEditor;
		public PreferencesOnMultiChoiceClickListener() {
			GreenToTalkApplication application= (GreenToTalkApplication)getApplication();
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(application);
			mEditor = settings.edit();
		}
		@Override
		public void onClick(DialogInterface dialog, int which, final boolean isChecked) {
			mEditor.putBoolean(GreenToTalkApplication.DND_AS_AVAILABLE_KEY, isChecked);
			mEditor.commit();
		}
	}
}
