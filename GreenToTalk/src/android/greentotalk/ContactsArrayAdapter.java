package android.greentotalk;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

public class ContactsArrayAdapter extends ArrayAdapter<Contact> {
	
	private LayoutInflater mInflater;
	private PickFreindsActivity mContext;
	private ContactsManager mContactsManager;
	
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
            holder.mode = (TextView) convertView.findViewById(R.id.mode);
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
        holder.mode.setText(mContactsManager.getStringModeAt(position));
        holder.select.setChecked(false);
        if (mContactsManager.isSelectedAt(position))
        	holder.select.setChecked(true);

        return convertView;
	}
	
	static class ViewHolder {
        TextView name;
        TextView mode;
        CheckBox select;
    }
}
