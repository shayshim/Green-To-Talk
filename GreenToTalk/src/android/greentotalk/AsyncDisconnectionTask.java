package android.greentotalk;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;

public class AsyncDisconnectionTask extends AsyncTask<Void, Void, Void> {
	
	private ProgressDialog mProgressDialog;
	private final PickFreindsActivity mContext;

	public AsyncDisconnectionTask(PickFreindsActivity context) {
		mContext = context;
		mProgressDialog = new ProgressDialog(context);
		mProgressDialog.setMessage("Disconnecting...");
		mProgressDialog.setIndeterminate(true);
		mProgressDialog.setCancelable(false);
	}

	@Override
	protected void onPreExecute() {
		mContext.stopService(new Intent(mContext, ContactListListenerService.class));
		mProgressDialog.show();
	}

	@Override
	protected void onPostExecute(Void result) {
		mContext.startActivity(new Intent(mContext, 
				SigninActivity.class).putExtra(SigninActivity.USER_DISCONNECTED, true));
		if (mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
		}
		mContext.finish();
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		if (ContactListListenerService.isConnectedToInternet(mContext)) {
			SynchronizedConnectionManager.getInstance().disconnect();	
		}
		else {
			SynchronizedConnectionManager.getInstance().removeOldConnection();
		}
		
		return null;
	}
}