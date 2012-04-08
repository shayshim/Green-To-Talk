/**
 *	This file is part of GTalkRadar.
 *
 *  GTalkRadar is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  GTalkRadar is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with GTalkRadar.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  @author Shay Shimony
 */

package android.greentotalk;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.pubsub.PresenceState;

import android.os.Parcel;
import android.os.Parcelable;

public class Contact implements Parcelable, Comparable<Contact>{
	
	public static final String FROM = "android.greentotalk.from";
	public static final String EMAIL = "android.greentotalk.email";
	public static final int UNAVAILABLE = 0;
	public static final int AWAY = 1;
	public static final int DND = 2;
	public static final int AVAILABLE = 3;
	
	private String mFrom;
	private String mName;
	private int mMode;
	private boolean mSelected;
	
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
	
	public String getStringMode() {
		return getStirngModeFromInt(mMode);
	}
	
	public Contact(Presence p) {
		setFrom(p.getFrom());
		setName(getEmail());
		setMode(p.getType(), p.getMode());
		setSelected(false);
	}
	
	public Contact(Parcel in) {
		mFrom = in.readString();
		mName = in.readString();
		mMode = in.readInt();
		mSelected = false;
	}
	
	public void setFrom(String from) {
		mFrom = from;
	}
	
	public void setName(String name) {
		if (name!=null)
			mName = name;
	}
	
	public void setSelected(boolean selected) {
		mSelected = selected;
	}
	
	public void setMode(Presence.Type type, Presence.Mode mode) {
		if (type == Presence.Type.unavailable) {
			mMode = UNAVAILABLE;
		}
		else {
			assert(type == Presence.Type.available);
			if (mode == null  ||  mode == Presence.Mode.available  ||  mode == Presence.Mode.chat) {
				mMode = AVAILABLE;
			}
			else if (mode == Presence.Mode.dnd) {
				mMode = DND;
			}
			else if (mode == Presence.Mode.away  ||  mode == Presence.Mode.xa) {
				mMode = AWAY;
			}
		}
	}
	
	public String getEmail() {
		return StringUtils.parseBareAddress(mFrom);
	}
	
	public String getName() {
		return mName;
	}
	
	public boolean isSelected() {
		return mSelected;
	}
	
	public int getMode() {
		return mMode;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mFrom);
		dest.writeString(mName);
		dest.writeInt(mMode);
	}
	
	public static final Parcelable.Creator<Contact> CREATOR = new Parcelable.Creator<Contact>() {
        public Contact createFromParcel(Parcel in) {
            return new Contact(in);
        }
 
        public Contact[] newArray(int size) {
            return new Contact[size];
        }
    };
    
    @Override
    public String toString() {
    	return mName+", "+mFrom+", "+getStirngModeFromInt(mMode)+", "+", selected is "+mSelected;
    }

	public void setMode(int mode) {
		mMode= mode;
	}
	
	@Override
	public int compareTo(Contact another) {
		assert(another!=null);
		int diff = -1*(mMode-another.getMode());
		if (diff == 0) {
			diff = mName.toLowerCase().compareTo(another.getName().toLowerCase());
		}
		return diff;
	}
	
	public void update(Presence presence) {
		setMode(presence.getType(), presence.getMode());
	}
}