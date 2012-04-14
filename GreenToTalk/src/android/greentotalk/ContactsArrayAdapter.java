package android.greentotalk;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

public class ContactsArrayAdapter extends ArrayAdapter<Contact> {
	
	private LayoutInflater mInflater;
	private PickFreindsActivity mContext;
	private ContactsManager mContactsManager;
	private static final String TAG = "ContactsArrayAdapter";
	
	public ContactsArrayAdapter(PickFreindsActivity context) {
		super(context, R.layout.rowlayout, context.getContactsManager().getContactList());
		mContactsManager = context.getContactsManager();
		// Cache the LayoutInflate to avoid asking for a new one each time.
		mContext = context;
		mInflater = LayoutInflater.from(mContext);
	}
	
	/**
     * Make a view to hold each row.
     */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// A ViewHolder keeps references to children views to avoid unneccessary calls
        // to findViewById() on each row.
        ViewHolder holder;

        // When convertView is not null, we can reuse it directly, there is no need
        // to reinflate it. We only inflate a new View when the convertView supplied
        // by ListView is null.
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.rowlayout, null);

            // Creates a ViewHolder and store references to the two children views
            // we want to bind data to.
            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.mode = (ImageView) convertView.findViewById(R.id.mode);
            holder.select = (CheckBox) convertView.findViewById(R.id.select);
            holder.select.setFocusable(false);
            convertView.setTag(holder);
        } else {
            // Get the ViewHolder back to get fast access to the TextView
            // and the ImageView.
            holder = (ViewHolder) convertView.getTag();
        }

        // Bind the data efficiently with the holder.
        holder.name.setText(mContactsManager.getNameAt(position));
        if (mContactsManager.getModeAt(position) == Contact.AVAILABLE) {
        	holder.mode.setImageResource(R.drawable.available_icon);
        }
        else if (mContactsManager.getModeAt(position) == Contact.AWAY) {
        	holder.mode.setImageResource(R.drawable.orange_b_smaller5);
        }
        else if (mContactsManager.getModeAt(position) == Contact.DND) {
        	holder.mode.setImageResource(R.drawable.red_b_smaller4);
        }
        else if (mContactsManager.getModeAt(position) == Contact.UNAVAILABLE) {
        	holder.mode.setImageResource(R.drawable.grey_b_smaller1);
        }
        
        holder.select.setChecked(false);
        boolean isSelected = mContactsManager.isSelectedAt(position);
        if (isSelected)
        	holder.select.setChecked(true);
        
        return convertView;
	}
	
	static class ViewHolder {
        TextView name;
        ImageView mode;
        CheckBox select;
    }
}
