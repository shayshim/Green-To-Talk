package android.greentotalk;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;


/**
 * Provides utility methods for communicating with the server.
 */
public class AsyncConnectionTask extends AsyncTask<String, Void, Boolean> {
	private ProgressDialog mProgressDialog;
	private final Context mContext;
	private SynchronizedConnectionManager mConnectionManager;
	private boolean mShowProgressDialog;
	private static final String TAG = "AsyncConnectionTask";

	public AsyncConnectionTask(Context context, boolean showProgressDialog) {
		Log.i(TAG, "Constructor...");
		mContext = context;
		mShowProgressDialog = showProgressDialog;
		mConnectionManager = SynchronizedConnectionManager.getInstance();
		if (showProgressDialog) { 
			mProgressDialog = new ProgressDialog(context);
			mProgressDialog.setMessage("Singing-in...");
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setCancelable(true);
			final AsyncConnectionTask thread = this;
			mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					Log.i("ConnectionAsyncTask", "dialog cancel has been invoked");
					if (thread != null) {
						thread.cancel(false);
					}
					((Activity) mContext).finish();
				}
			});
		}
	}

	@Override
	protected void onPreExecute() {
		if (mShowProgressDialog) {
			mProgressDialog.show();
		}
	}

	@Override
	protected void onPostExecute(final Boolean success) {
		if (mShowProgressDialog) {
			if (mContext instanceof SigninActivity) {
				((SigninActivity)mContext).onAuthenticationResult(success.booleanValue());
			}
			if (mProgressDialog.isShowing()) {
				mProgressDialog.dismiss();
			}
			if (success)
				((Activity) mContext).finish();
		}
		else {
			Log.i(TAG, "onPostExecute: success is "+success);
			if (success  &&  mContext instanceof ContactListListenerService) {
				((ContactListListenerService)mContext).addListeners();
				((ContactListListenerService)mContext).setIsTryingReconnect(false);
			}
		}
	}

	@Override
	protected Boolean doInBackground(String... strings) {
		String username = strings[0];
		String password = strings[1];
		Log.i(TAG, "doInBackground");
		return mConnectionManager.connectAndRetry(username, password);
	}

	@Override
	protected void onCancelled () {
		mConnectionManager.disconnect();
	}
}