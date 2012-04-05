package android.greentotalk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.util.StringUtils;

public class ContactsManager {
	private static final String TAG = "ContactManager";
	private Map<String, PresenceWrapper> mEmailToPresenceWrappers = new HashMap<String, PresenceWrapper>();

	public void buildMapEmailToPresenceWrappers() {
		Roster roster = SynchronizedConnectionManager.getInstance().getRoster();
		for (RosterEntry entry: roster.getEntries()) {
			String email = entry.getUser();
			if (! StringUtils.parseServer(email).equals(SigninActivity.GMAIL_DOMAIN))
				continue;
			String name = (entry.getName() == null)? email : entry.getName();
			Presence newp = roster.getPresence(email);
			mEmailToPresenceWrappers.put(email, new PresenceWrapper(newp, name));
		}
	}
	
	public ArrayList<Map<String, String>> buildUIContacts() {
		return buildUIContacts(new ArrayList<Map<String, String>>());
	}

	public ArrayList<Map<String, String>> buildUIContacts(ArrayList<Map<String, String>> list) {
		ArrayList<PresenceWrapper> presenceWrappers = new ArrayList<PresenceWrapper>(mEmailToPresenceWrappers.values()); 
		Collections.sort(presenceWrappers, new PresenceWrapperComparator());

		for (PresenceWrapper p : presenceWrappers) {
			String email = StringUtils.parseBareAddress(p.getPresence().getFrom());
			if (email.equals(SynchronizedConnectionManager.getInstance().getUsername()))
				continue;
			list.add(putData(p.getName(), email, PresenceWrapper.getIntegerFromMode(p.getPresence())));
		}
		return list;
	}

	private HashMap<String, String> putData(String name, String email, int intmode) {
		HashMap<String, String> item = new HashMap<String, String>();
		if (name == null) {
			name = StringUtils.parseName(email);
		}
		item.put(PickFreindsActivity.NAME_FIELD, name);
		item.put(PickFreindsActivity.EMAIL_FIELD, email);
		item.put(PickFreindsActivity.MODE_FIELD, PresenceWrapper.getStirngModeFromInt(intmode));
		return item;
	}

	public ArrayList<Map<String, String>> getUpdatedList(ArrayList<Map<String, String>> list, String from, boolean available, Mode mode, int priority) {
//		PresenceWrapper prevpw = mEmailToPresenceWrappers.get(email);
//		if (prevpw == null) {
//			prevpw = new PresenceWrapper(new Presence((available)? Type.available : Type.unavailable), SynchronizedConnectionManager.getInstance().getRoster().getEntry(email).getName());
//			prevpw.getPresence().setFrom(email);
//			mEmailToPresenceWrappers.put(email, prevpw);
//		}
//		else {
//			int prevPriority = prevpw.getPresence().getPriority(); 
//			Log.i(TAG, "for "+email+", available="+available+", mode="+mode+"priority="+priority+";;; prev: "+prevpw.getPresence().getFrom()+", type:mode"+prevpw.getPresence()+", priority"+prevpw.getPresence().getPriority());
//			if ( prevPriority > priority) {
//				return list;
//			}
//			else if (prevPriority == priority) {
//				Presence newPresence = new Presence((available)? Type.available : Type.unavailable);
//				newPresence.setMode(mode);
//				if (PresenceWrapper.getModePriority(prevpw.getPresence()) >= PresenceWrapper.getModePriority(newPresence)) {
//					return list;
//				}
//			}
//		}
		
		
//		Roster r = SynchronizedConnectionManager.getInstance().getRoster();
//		PresenceWrapper pw = new PresenceWrapper(r.getPresence(from), r.getEntry(StringUtils.parseBareAddress(from)).getName());
//		String email = StringUtils.parseBareAddress(from);
//		if (mode == Mode.dnd) {
//			// read Roster.getPresence(String) for explanation. we need to correct order for dnd to be stronger than away
//			pw.getPresence().setFrom(from);
//			pw.getPresence().setMode(mode);
//			pw.getPresence().setPriority(priority);
//			pw.getPresence().setType(Type.available);
//		}
//		else {
//			PresenceWrapper prevpw = mEmailToPresenceWrappers.get(email);
//			if (prevpw.getPresence().getMode() == Mode.dnd  &&  (mode == Mode.away  ||  mode == Mode.xa)  &&  
//					!prevpw.getPresence().getFrom().equals(pw.getPresence().getFrom())) { 
//				// note we compare equals including resource. if different resource then old dnd remain stronger than new away
//				// we need same resource with new away to change from dnd to away
//				return list;
//			}
//		}
//		mEmailToPresenceWrappers.put(email, pw);
		
		
		buildMapEmailToPresenceWrappers();
		list.clear();
//		prevpw.getPresence().setMode(mode);
//		if (available)
//			prevpw.getPresence().setType(Presence.Type.available);
//		else prevpw.getPresence().setType(Presence.Type.unavailable);
//		prevpw.getPresence().setPriority(priority);
		return buildUIContacts(list);
	}
}
