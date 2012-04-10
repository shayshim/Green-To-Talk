package android.greentotalk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;

public class ContactsManager {
	
	private SharedPreferences mSavedSelectedContacts;
	public static final String SAVED_SELECTED_CONTACTS = "android.greentotalk.SAVED_SELECTED_CONTACTS";
	private static final String TAG = "ContactManager";
	private List<Contact> mContactsList;
	private Map<String, Contact> mContactsMap;
	private Map<String, Boolean> mSelectedContacts;
	private SynchronizedConnectionManager mConnectionManager;
	
	
	@SuppressWarnings("unchecked")
	public ContactsManager(SharedPreferences savedSelectedContacts) {
		mContactsList = new ArrayList<Contact>();
		mContactsMap = new HashMap<String, Contact>();
		mConnectionManager = SynchronizedConnectionManager.getInstance();
		mSavedSelectedContacts = savedSelectedContacts;
		mSelectedContacts = (Map<String, Boolean>) mSavedSelectedContacts.getAll();
	}
	
	public void updateContactList() {
		for (RosterEntry entry: mConnectionManager.getEntries()) {
			String email = entry.getUser();
			if (! StringUtils.parseServer(email).equals(SigninActivity.GMAIL_DOMAIN))
				continue;
			if (email.equals(mConnectionManager.getUsername()))
				continue;
			Presence presence = mConnectionManager.getPresence(email);
			if (presence == null)
				return; // if disconnection happened we might get null, and in such case this method execution can stop
			Contact contact;
			if (mContactsMap.containsKey(email)) {
				contact = mContactsList.get(getContactPosition(email));
				contact.update(presence);
			}
			else {
				contact = new Contact(presence);
				contact.setName(entry.getName());
				mContactsList.add(contact);
				mContactsMap.put(email, contact);
			}
		}
		Log.i(TAG, "updateContactList: "+mContactsList.toString());
		Collections.sort(mContactsList);
	}

	int getContactPosition(String email) {
		int size = mContactsList.size();
		for (int i=0; i<size; ++i) {
			if (mContactsList.get(i).getEmail().equals(email))
				return i;
		}
		return -1;
	}

	public void updateContactList(String email) {
		assert(email!=null);
		if (email.equals(SynchronizedConnectionManager.getInstance().getUsername()))
			return;
		Presence presence = mConnectionManager.getPresence(email);
		if (presence == null)
			return; // if disconnection happened we might get null, and in such case this method execution can stop
		int position = getContactPosition(email);
		Contact contact;
		if (position > -1) {
			contact = mContactsList.get(position);
			contact.update(presence);
		}
		else {
			contact = new Contact(presence);
			mContactsList.add(contact);
		}
		Collections.sort(mContactsList);
		Log.i(TAG, "updateUI for: "+contact);
	}
	
	public List<Contact> getContactList() {
		return mContactsList;
	}
	
	public String getNameAt(int position) {
		assert(position>-1 && position<mContactsList.size());
		return mContactsList.get(position).getName();
	}
	
	public String getEmailAt(int position) {
		assert(position>-1 && position<mContactsList.size());
		return mContactsList.get(position).getEmail();
	}
	
	public int getModeAt(int position) {
		assert(position>-1 && position<mContactsList.size());
		return mContactsList.get(position).getMode();
	}
	
	public String getStringModeAt(int position) {
		assert(position>-1 && position<mContactsList.size());
		return mContactsList.get(position).getStringMode();
	}

	public boolean isSelectedAt(int position) {
		return mSelectedContacts.containsKey(mContactsList.get(position).getEmail());
	}

	public void setSelected(String email, boolean selected) {
		if (selected) {
			mSelectedContacts.put(email, true);
		}
		else {
			mSelectedContacts.remove(email);
		}
	}
	
	public String getName(String email) {
		return mContactsMap.get(email).getName();
	}
	
	public void setSelectedAndSave(String email, boolean selected) {
		setSelected(email, selected);
		Editor edit = mSavedSelectedContacts.edit();
		if (selected) {
			edit.putBoolean(email, true);
		}
		else {
			edit.remove(email);
		}
		edit.commit();
	}

	public void clearContacts() {
		mContactsMap.clear();
		mContactsList.clear();
	}
	
	public void sendSelectedContactsToNotificationService(Context context) {
		Bundle bundle = new Bundle();
		if (!mSelectedContacts.isEmpty()) {
			Intent intent = new Intent(context, NotificationService.class);
			Set<String> emails = mSelectedContacts.keySet();
			for (String email: emails) {
				bundle.putString(email, mContactsMap.get(email).getName());
			}
			intent.putExtra(SAVED_SELECTED_CONTACTS, bundle);
			context.startService(intent);
		}
	}

	public void setOppositeSelection(String email) {
		if (mSelectedContacts.containsKey(email)) {
			mSelectedContacts.remove(email);
		}
		else {
			mSelectedContacts.put(email, true);
		}
	}

	public void saveSelectedContacts() {
		Editor editor = mSavedSelectedContacts.edit();
		editor.clear();
		Set<String> emails = mSelectedContacts.keySet();
		for (String email: emails) {
			editor.putBoolean(email, true);
		}
		editor.commit();
	}

	public void setOppositeSelection(Contact contact) {
		setOppositeSelection(contact.getEmail());
	}
}