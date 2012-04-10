package android.greentotalk;

import java.util.ArrayList;
import java.util.Map;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;

public class AsyncContactsSupplier extends AsyncTask<Void, Void, Void> {
	private ProgressDialog mProgressDialog;
	private final PickFreindsActivity mContext;
	private ContactsManager mContactsManager;

	public AsyncContactsSupplier(PickFreindsActivity context, ContactsManager cm) {
		mContext = context;
		mContactsManager = cm;
		mProgressDialog = new ProgressDialog(context);
		mProgressDialog.setMessage("Loading list...");
		mProgressDialog.setIndeterminate(true);
		mProgressDialog.setCancelable(true);
		mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				Log.i("ConnectionAsyncTask", "dialog cancel has been invoked");
				if (AsyncContactsSupplier.this != null) {
					AsyncContactsSupplier.this.cancel(false);
				}
				mContext.finish();
			}
		});
	}

	@Override
	protected void onPreExecute() {
		mProgressDialog.show();
	}

	@Override
	protected void onPostExecute(Void v) {
		if (mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
		}
//		mContext.initializeUI();
//		mContext.setContactsListsnerService();
	}

	@Override
	protected Void doInBackground(Void... v) {
		Log.i("AsyncContactsSupplier", "get contacts info...");
		mContactsManager.updateContactList();
		return null;
	}
}
