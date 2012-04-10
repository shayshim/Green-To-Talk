package android.greentotalk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import android.util.Log;

public class ContactsManager {
	private static final String TAG = "ContactManager";
	private static ContactsManager instance; 
	private List<Contact> mContacts;
	private Set<String> mEmails;
	private SynchronizedConnectionManager mConnectionManager;
	
	public synchronized static ContactsManager getInstance() {
		if (instance == null) {
			instance = new ContactsManager();
		}
		return instance;
	}
	
	private ContactsManager() {
		mContacts = new ArrayList<Contact>();
		mEmails = new HashSet<String>();
		mConnectionManager = SynchronizedConnectionManager.getInstance();
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
			if (mEmails.contains(email)) {
				contact = mContacts.get(getContactPosition(email));
				contact.update(presence);
			}
			else {
				contact = new Contact(presence);
				contact.setName(entry.getName());
				mContacts.add(contact);
				mEmails.add(email);
			}
		}
		Log.i(TAG, "updateContactList: "+mContacts.toString());
		Collections.sort(mContacts);
	}

	int getContactPosition(String email) {
		int size = mContacts.size();
		for (int i=0; i<size; ++i) {
			if (mContacts.get(i).getEmail().equals(email))
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
			contact = mContacts.get(position);
			contact.update(presence);
		}
		else {
			contact = new Contact(presence);
			mContacts.add(contact);
		}
		Collections.sort(mContacts);
		Log.i(TAG, "updateUI for: "+contact);
	}
	
	public List<Contact> getContactList() {
		return mContacts;
	}
	
	public String getNameAt(int position) {
		assert(position>-1 && position<mContacts.size());
		return mContacts.get(position).getName();
	}
	
	public String getEmailAt(int position) {
		assert(position>-1 && position<mContacts.size());
		return mContacts.get(position).getEmail();
	}
	
	public int getModeAt(int position) {
		assert(position>-1 && position<mContacts.size());
		return mContacts.get(position).getMode();
	}
	
	public String getStringModeAt(int position) {
		assert(position>-1 && position<mContacts.size());
		return mContacts.get(position).getStringMode();
	}

	public boolean isSelectedAt(int position) {
		return mContacts.get(position).isSelected();
	}

	public void setSelected(String email, boolean selected) {
		mContacts.get(getContactPosition(email)).setSelected(selected);
	}

	public void clearContacts() {
		mEmails.clear();
		mContacts.clear();
	}
}