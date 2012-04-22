package android.greentotalk;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

public class AsyncDisconnectionTask extends AsyncTask<Void, Void, Void> {
	
	private ProgressDialog mProgressDialog;
	private final PickContactsActivity mContext;
	private static final String TAG = "AsyncDisconnectionTask";

	public AsyncDisconnectionTask(PickContactsActivity context) {
		mContext = context;
		mProgressDialog = new ProgressDialog(context);
		mProgressDialog.setMessage("Disconnecting...");
		mProgressDialog.setIndeterminate(true);
		mProgressDialog.setCancelable(false);
	}

	@Override
	protected void onPreExecute() {
		mProgressDialog.show();
	}

	@Override
	protected void onPostExecute(Void result) {
		if (mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
		}
		mContext.finish();
		mContext.startActivity(new Intent(mContext, 
				SigninActivity.class).putExtra(SigninActivity.USER_DISCONNECTED, true));
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		if (ContactListListenerService.isConnectedToInternet(mContext)) {
			Log.i(TAG, "doInBackground, real disconnection");
			SynchronizedConnectionManager.getInstance().disconnect();
		}
		else {
			Log.i(TAG, "doInBackground, work around disconnection");
			SynchronizedConnectionManager.getInstance().removeOldConnection();
		}
		
		return null;
	}
}