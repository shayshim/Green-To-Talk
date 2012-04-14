package android.greentotalk;

import java.util.Set;

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
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;

public class PickFreindsActivity extends ListActivity {

	private static final String TAG = "PickFreindsActivity";
	public static final String NAME_FIELD = "android.greentotalk.name";
	public static final String MODE_FIELD = "android.greentotalk.mode";
	public static final String EMAIL_FIELD = "android.greentotalk.email";
	public static final String UPDATE_LIST_BROADCAST = "android.greentotalk.update_list_broadcast";
	public static final String START_FOR_SAVED_CONTACTS = "android.greentotalk.START_FOR_SAVED_CONTACTS";
	public static final String SAVED_SELECTED_CONTACTS = "android.greentotalk.SAVED_SELECTED_CONTACTS";
	public static final String UPDATE_LIST_CONTENT = "android.greentotalk.UPDATE_LIST_CONTENT";
	public static final String UNSELECT_CONTACT = "android.greentotalk.UNSELECT_CONTACT";
	private static final int DISCONNECT_DIALOG = 0;
	private static final int SETTINGS_DIALOG = 1;
	private ContactsArrayAdapter mAdapter;
	private ContactsManager mContactsManager;
	private ScreenStateBroadcastReceiver mScreenStateBroadcastReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate...");
		mContactsManager = new ContactsManager(getSharedPreferences(SAVED_SELECTED_CONTACTS, MODE_PRIVATE));
		mContactsManager.updateContactList();
		mAdapter = new ContactsArrayAdapter(this);
		mScreenStateBroadcastReceiver = new ScreenStateBroadcastReceiver();
		IntentFilter screenFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
		registerReceiver(mScreenStateBroadcastReceiver, screenFilter);
		ListView lv = getListView();
		lv.setTextFilterEnabled(true);
		setListAdapter(mAdapter);		
		setContentView(R.layout.contact_list);
		Button finishSelectContacts = (Button) findViewById(R.id.finish_select_contacts);
		finishSelectContacts.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mContactsManager.saveSelectedContacts();
				Bundle bundle = new Bundle();
				Intent intent = new Intent(PickFreindsActivity.this, ContactListListenerService.class);
				Set<String> emails = mContactsManager.getAllEmails();
				for (String email: emails) {
					bundle.putString(email, mContactsManager.getName(email));
				}
				intent.putExtra(SAVED_SELECTED_CONTACTS, bundle);
				startService(intent);
				finish();
			}
		});
	}

	public ContactsManager getContactsManager() {
		return mContactsManager;
	}

	private BroadcastReceiver mUpdateListBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateUI(intent);
		}
	};

	@Override
	public void onResume() {
		super.onResume();
		if (ScreenStateBroadcastReceiver.wasScreenOff()) {
			mContactsManager.clearContacts();
			mContactsManager.updateContactList();
		}
		registerReceiver(mUpdateListBroadcastReceiver, new IntentFilter(UPDATE_LIST_BROADCAST));
		Intent intent = new Intent(this, ContactListListenerService.class);
		intent.putExtra(ContactListListenerService.START_UPDATE_CONTACT_LIST, true);
		startService(intent);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mScreenStateBroadcastReceiver);
	}

	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(mUpdateListBroadcastReceiver);
	}	

	private void updateUI(Intent intent) {
		String email = intent.getStringExtra(Contact.EMAIL);
		if (intent.getBooleanExtra(UNSELECT_CONTACT, false)) {
			mContactsManager.setSelected(email, false);
		}
		else if (intent.getBooleanExtra(UPDATE_LIST_CONTENT, false)) {
			if (email == null) {
				mContactsManager.updateContactList();
			}
			else {
				mContactsManager.updateContactList(email);
			}
		}
		mAdapter.notifyDataSetChanged();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		ListAdapter la = getListAdapter();
		Contact contact = (Contact)la.getItem(position);
		mContactsManager.setOppositeSelection(contact);
		mAdapter.notifyDataSetChanged();
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
			final String[] strings = new String[]{"Treat \"Busy\" as \"Available\"", 
													"Make sound when available", 
													"Vibrate when available"}; 
			final OnMultiChoiceClickListener onClick = new PreferencesOnMultiChoiceClickListener();
			boolean [] choices = {settings.getBoolean(GreenToTalkApplication.DND_AS_AVAILABLE_KEY, false), 
					settings.getBoolean(GreenToTalkApplication.MAKE_SOUND_KEY, false), 
					settings.getBoolean(GreenToTalkApplication.VIBRATE_KEY, false)};
			builder.setMultiChoiceItems(strings, choices, onClick);
			builder.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					dialog.cancel();
				}
			});
			break;
		case DISCONNECT_DIALOG:
			title = "Are you sure you want to disconnect?";
			builder.setTitle(title);
			builder.setIcon(R.drawable.available_icon);
			final String[] strings2 = new String[]{"Also delete account settings"};
			final boolean [] choices2 = {false};
			builder.setMultiChoiceItems(strings2, choices2, new OnMultiChoiceClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which,
						boolean isChecked) {
					choices2[0] = isChecked;
				}
			});
			builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					stopService(new Intent(PickFreindsActivity.this, ContactListListenerService.class));
					mContactsManager.deleteSavedSelectedContacts();
					new AsyncDisconnectionTask(PickFreindsActivity.this).execute((Void[])null);
					if (choices2[0]) {
						application.restoreDefaultPreferences();
					}
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
			if (which == 0) {
				mEditor.putBoolean(GreenToTalkApplication.DND_AS_AVAILABLE_KEY, isChecked);
			}
			else if (which == 1) {
				mEditor.putBoolean(GreenToTalkApplication.MAKE_SOUND_KEY, isChecked);
			}
			else {
				mEditor.putBoolean(GreenToTalkApplication.VIBRATE_KEY, isChecked);
			}
			mEditor.apply();
		}
	}
}