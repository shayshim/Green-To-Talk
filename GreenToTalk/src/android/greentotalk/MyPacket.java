package android.greentotalk;
import org.jivesoftware.smack.packet.Packet;


public class MyPacket extends Packet {

	private int p;
	private String username="shayshim";
	void setPacket(int i) {
		p = i;
	}
	
	@Override
	public String toXML() {
		switch (p) {
		case 1: 
			return "<iq type='get' to='gmail.com'><query xmlns='http://jabber.org/protocol/disco#info'/></iq>";
		case 2:
			return "<iq type='get' to='"+username+"@gmail.com' id='ss-1' ><query xmlns='google:shared-status' version='2'/></iq>";
		case 3:
			return "<iq type='set' to='"+username+"@gmail.com' id='ss-2'><query xmlns='google:shared-status' version='2'><invisible value='true'/></query></iq>";
		}
		return null;
	}

}
