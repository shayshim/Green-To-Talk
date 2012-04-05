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

import java.util.Comparator;

public class PresenceWrapperComparator implements Comparator<PresenceWrapper>{
	
	@Override
	public int compare(PresenceWrapper o1, PresenceWrapper o2) {
		int p1 = PresenceWrapper.getModePriority(o1.getPresence());
		int p2 = PresenceWrapper.getModePriority(o2.getPresence());
		int diff = p2-p1;
		if (diff == 0) {
			diff = o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
			if (diff == 0  &&  o1.getPresence().getFrom().equals(o2.getPresence().getFrom())) {
				p1 = o1.getPresence().getPriority();
				p2 = o2.getPresence().getPriority();
				diff = p2-p1;
			}
		}
		return diff;
	}
}
