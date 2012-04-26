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
	private static final String TAG = "AsyncConnectionTask";

	public AsyncConnectionTask(Context context) {
		Log.i(TAG, "Constructor...");
		mContext = context;
		mConnectionManager = SynchronizedConnectionManager.getInstance();
		mProgressDialog = new ProgressDialog(context);
		mProgressDialog.setMessage("Signing in...");
		mProgressDialog.setIndeterminate(true);
		mProgressDialog.setCancelable(true);
		mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				Log.i("ConnectionAsyncTask", "dialog cancel has been invoked");
				AsyncConnectionTask.this.cancel(false);
				((Activity) mContext).finish();
			}
		});
	}

	@Override
	protected void onPreExecute() {
		mProgressDialog.show();
	}

	@Override
	protected void onPostExecute(final Boolean success) {
		if (mContext instanceof SigninActivity) {
			((SigninActivity)mContext).onAuthenticationResult(success.booleanValue());
		}
		if (mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
		}
		if (success)
			((Activity) mContext).finish();
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