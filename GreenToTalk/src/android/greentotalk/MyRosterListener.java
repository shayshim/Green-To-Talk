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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.packet.Presence;

public class MyRosterListener implements RosterListener {
	
	private ArrayList<Presence> _presences;
	
	public MyRosterListener() {
		_presences = new ArrayList<Presence>();
	}
	
	@Override
	public void entriesAdded(Collection<String> arg0) {}

	@Override
	public void entriesDeleted(Collection<String> arg0) {}

	@Override
	public void entriesUpdated(Collection<String> arg0) {}

	@Override
	public synchronized void presenceChanged(Presence presence) {
			_presences.add(presence);
	}
	
	public synchronized ArrayList<Presence> getPresences() {
		return _presences;
	}
}
