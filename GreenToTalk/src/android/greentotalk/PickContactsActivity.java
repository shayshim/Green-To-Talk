package android.greentotalk;

import java.util.Set;

import android.app.Activity;
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
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class PickContactsActivity extends ListActivity {

	private static final String TAG = "PickFreindsActivity";
	public static final String NAME_FIELD = "android.greentotalk.name";
	public static final String MODE_FIELD = "android.greentotalk.mode";
	public static final String EMAIL_FIELD = "android.greentotalk.email";
	public static final String UPDATE_LIST_BROADCAST = "android.greentotalk.update_list_broadcast";
	public static final String START_FOR_SAVED_CONTACTS = "android.greentotalk.START_FOR_SAVED_CONTACTS";
	public static final String SAVED_SELECTED_CONTACTS = "android.greentotalk.SAVED_SELECTED_CONTACTS";
	public static final String UPDATE_LIST_CONTENT = "android.greentotalk.UPDATE_LIST_CONTENT";
	public static final String UNSELECT_CONTACT = "android.greentotalk.UNSELECT_CONTACT";
	public static final String NOTIFICATION_SOUND_URI = "android.greentotalk.NOTIFICATION_SOUND_URI";
	private static final int DISCONNECT_DIALOG = 0;
	private static final int SETTINGS_DIALOG = 1;
	public static final String CLEAR_LIST = "android.greentotalk.CLEAR_LIST";
	private ContactsArrayAdapter mAdapter;
	private ContactsManager mContactsManager;
	private ScreenStateBroadcastReceiver mScreenStateBroadcastReceiver;
	private Button mChangeSoundButton;
	private SharedPreferences mSettings;
	private GreenToTalkApplication mApplication;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate...");
		mApplication= (GreenToTalkApplication)getApplication();
		mSettings = PreferenceManager.getDefaultSharedPreferences(mApplication);
		
		mContactsManager = new ContactsManager(getSharedPreferences(SAVED_SELECTED_CONTACTS, MODE_PRIVATE));
		if (getIntent().getBooleanExtra(CLEAR_LIST, false)) {
			mContactsManager.clearContacts();
		}
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
				if (SynchronizedConnectionManager.getInstance().isConnected()) {
					mContactsManager.saveSelectedContacts();
					Bundle bundle = new Bundle();
					Intent intent = new Intent(PickContactsActivity.this, ContactListListenerService.class);
					Set<String> emails = mContactsManager.getAllEmails();
					for (String email: emails) {
						bundle.putString(email, mContactsManager.getName(email));
					}
					intent.putExtra(SAVED_SELECTED_CONTACTS, bundle);
					startService(intent);
				}
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
		Intent intent = new Intent(this, ContactListListenerService.class);
		intent.putExtra(ContactListListenerService.STOP_UPDATE_CONTACT_LIST, true);
		if (!SynchronizedConnectionManager.getInstance().isDisconnecting()) {
			startService(intent);
		}
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
	 protected void onActivityResult(final int requestCode, final int resultCode, final Intent intent)
	 {
	     if (resultCode == Activity.RESULT_OK && requestCode == 5)
	     {
	          Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
	          
	          if (uri != null)
	          {
	              mSettings.edit().putString(NOTIFICATION_SOUND_URI, uri.toString()).apply();
	          }
	          else
	          {
	        	  mSettings.edit().putString(NOTIFICATION_SOUND_URI, null).apply();
	          }
	     }            
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
		switch(index) {
		case SETTINGS_DIALOG:
			LayoutInflater inflater = (LayoutInflater) PickContactsActivity.this.getSystemService(LAYOUT_INFLATER_SERVICE);
	    	View layout = inflater.inflate(R.layout.settings_dialog_layout,
	    	                               (ViewGroup) findViewById(R.id.layout_root));
	    	mChangeSoundButton = (Button) layout.findViewById(R.id.buttonChangeSound);
	    	mChangeSoundButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
					intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
					intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone");
					intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
					intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
					String chosenRingtone = mSettings.getString(NOTIFICATION_SOUND_URI, null); 
					if (chosenRingtone == null) {
						intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
					}
					else {
						intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(chosenRingtone));
					}
					PickContactsActivity.this.startActivityForResult(intent, 5);
				}
			});
	    	CheckBox c = (CheckBox) layout.findViewById(R.id.checkbox_busy_as_available);
	    	c.setChecked(mSettings.getBoolean(GreenToTalkApplication.DND_AS_AVAILABLE_KEY, false));
	    	c = (CheckBox) layout.findViewById(R.id.checkbox_vibrate);
	    	c.setChecked(mSettings.getBoolean(GreenToTalkApplication.VIBRATE_KEY, false));
	    	c = (CheckBox) layout.findViewById(R.id.checkbox_make_sound);
	    	c.setChecked(mSettings.getBoolean(GreenToTalkApplication.MAKE_SOUND_KEY, false));
	    	mChangeSoundButton.setEnabled(c.isChecked());
	    	
	    	title = getString(R.string.settings_title, versionname);
			builder.setTitle(title);
			builder.setIcon(R.drawable.available_icon);
			builder.setView(layout);
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
			final String[] disconnectDescriptions = new String[]{"Also delete account settings"};
			final boolean [] disconnectChoises = {false};
			builder.setMultiChoiceItems(disconnectDescriptions, disconnectChoises, new OnMultiChoiceClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which,
						boolean isChecked) {
					disconnectChoises[0] = isChecked;
				}
			});
			builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// need to set disconnecting because there is a chance that new connection state events will be received 
					// in the ContactListListenerService broadcast receiver before stopService will complete
					SynchronizedConnectionManager.getInstance().setDisconnecting(true);
					stopService(new Intent(PickContactsActivity.this, ContactListListenerService.class));
					mContactsManager.deleteSavedSelectedContacts();
					new AsyncDisconnectionTask(PickContactsActivity.this).execute((Void[])null);
					if (disconnectChoises[0]) {
						mApplication.restoreDefaultPreferences();
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
	
	public void onCheckboxClicked(View v) {
		Editor editor = mSettings.edit();
		if (((CheckBox) v).getId() == R.id.checkbox_busy_as_available) {
			editor.putBoolean(GreenToTalkApplication.DND_AS_AVAILABLE_KEY, ((CheckBox) v).isChecked());
		}
		if (((CheckBox) v).getId() == R.id.checkbox_make_sound) {
        	mChangeSoundButton.setEnabled(((CheckBox) v).isChecked());
        	editor.putBoolean(GreenToTalkApplication.MAKE_SOUND_KEY, ((CheckBox) v).isChecked());
        }
		if (((CheckBox) v).getId() == R.id.checkbox_vibrate) {
			editor.putBoolean(GreenToTalkApplication.VIBRATE_KEY, ((CheckBox) v).isChecked());
        }
		editor.apply();
    }
}