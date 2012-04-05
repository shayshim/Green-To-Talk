package android.greentotalk;

import org.jivesoftware.smack.packet.Presence;

public class PresenceWrapper {

	public static final int UNAVAILABLE = 0;
	public static final int AWAY = 1;
	public static final int DND = 2;
	public static final int AVAILABLE = 3;
	
	private Presence mPresence;
	private String mName;
	
	public static int getModePriority(Presence p) {
		Presence.Type type = p.getType();
		Presence.Mode mode = p.getMode();
		if (type == Presence.Type.unavailable)
			return UNAVAILABLE;
		else if (mode == null  ||  mode == Presence.Mode.available)
			return AVAILABLE;
		else if (mode == Presence.Mode.away)
			return AWAY;
		else if (mode == Presence.Mode.chat)
			return AVAILABLE;
		else if (mode == Presence.Mode.dnd)
			return DND;
		else if (mode == Presence.Mode.xa)
			return AWAY;
		return AVAILABLE;
	}
	
	public static int getIntegerFromMode(Presence p) {
		Presence.Type type = p.getType();
		Presence.Mode mode = p.getMode();
		if (mode == null) {
			if (type == Presence.Type.available)
				return AVAILABLE;
			else return UNAVAILABLE;
		}
		else if (mode == Presence.Mode.available  ||  mode == Presence.Mode.chat)
			return AVAILABLE;
		else if (mode == Presence.Mode.away  ||  mode == Presence.Mode.xa)
			return AWAY;
		else if (mode == Presence.Mode.dnd)
			return DND;
		return UNAVAILABLE;
	}
	
	public static Presence.Mode getModeFromInteger(int m) {
		if (m == AVAILABLE) 
			return Presence.Mode.available;
		else if (m == AWAY) 
			return Presence.Mode.away;
		else if (m == DND) 
			return Presence.Mode.dnd;
		return null;
	}
	
	public static String getStirngModeFromInt(int intmode) {
		if (intmode == AVAILABLE)
			return "Available";
		else if (intmode == DND)
			return "Busy";
		else if (intmode == AWAY)
			return "Away";
		else if (intmode == UNAVAILABLE)
			return "Unavailable";
		return null;
	}
	
	public PresenceWrapper(Presence p, String name) {
		mPresence = p;
		mName = name;
	}
	
	public Presence getPresence() {
		return mPresence;
	}
	
	public String getName() {
		return mName;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (! (obj instanceof PresenceWrapper)) {
			return false;
		}
		Presence p = ((PresenceWrapper)obj).getPresence();
		return mPresence.equals(p);
	}
	
	@Override
	public int hashCode() {
		return mPresence.hashCode();
	}
}
