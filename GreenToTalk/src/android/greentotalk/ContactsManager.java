package android.greentotalk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class ContactsManager {

	private SharedPreferences mSavedSelectedContacts;
	private static final String TAG = "ContactManager";
	private List<Contact> mContactsList;
	private Map<String, Contact> mContactsMap;
	private Set<String> mSelectedContacts;
	private SynchronizedConnectionManager mConnectionManager;


	public ContactsManager(SharedPreferences savedSelectedContacts) {
		mContactsList = new ArrayList<Contact>();
		mContactsMap = new HashMap<String, Contact>();
		mConnectionManager = SynchronizedConnectionManager.getInstance();
		mSavedSelectedContacts = savedSelectedContacts;
		mSelectedContacts = new HashSet<String>((Set<String>) (mSavedSelectedContacts.getAll().keySet()));
	}

	public void updateContactList() {
		for (RosterEntry entry: mConnectionManager.getEntries()) {
			String email = entry.getUser();
			if (! StringUtils.parseServer(email).equals(mConnectionManager.getDomain()))
				continue;
			if (email.equals(mConnectionManager.getUsernameEmail()))
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
		if (email.equals(SynchronizedConnectionManager.getInstance().getUsernameEmail()))
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
		//		Log.i(TAG, "updateUI for: "+contact);
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
		return mSelectedContacts.contains(mContactsList.get(position).getEmail());
	}

	public void setSelected(String email, boolean selected) {
		if (selected) {
			mSelectedContacts.add(email);
		}
		else {
			mSelectedContacts.remove(email);
		}
	}

	public String getName(String email) {
		return mContactsMap.get(email).getName();
	}

	public void clearContacts() {
		mContactsMap.clear();
		mContactsList.clear();
	}

	public void sendSelectedContactsToNotificationService(Context context) {

	}

	public void setOppositeSelection(String email) {
		if (mSelectedContacts.contains(email)) {
			mSelectedContacts.remove(email);
		}
		else {
			mSelectedContacts.add(email);
		}
	}

	public void saveSelectedContacts() {
		Editor editor = mSavedSelectedContacts.edit();
		editor.clear();
		for (String email: mSelectedContacts) {
			editor.putString(email, getName(email));
		}
		editor.apply();
	}

	public void setOppositeSelection(Contact contact) {
		setOppositeSelection(contact.getEmail());
	}

	int countSavedSelectedContacts() {
		return mSelectedContacts.size();
	}

	public void deleteSavedSelectedContacts() {
		Editor edit = mSavedSelectedContacts.edit();
		edit.clear();
		edit.apply();
	}

	Set<String> getAllEmails() {
		return mSelectedContacts;
	}
}