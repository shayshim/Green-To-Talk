package android.greentotalk;

import org.jivesoftware.smack.packet.Presence;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;


/**
 * Provides utility methods for communicating with the server.
 */
public class AsyncConnectionTask extends AsyncTask<String, Void, Boolean> {
	public static final String PARAM_USERNAME = "username";
	public static final String PARAM_PASSWORD = "password";
	public static final String PARAM_UPDATED = "timestamp";
	public static final String USER_AGENT = "AuthenticationService/1.0";
	private ProgressDialog mProgressDialog;
	private final SigninActivity mContext;

	public AsyncConnectionTask(SigninActivity context) {
		mContext = context;
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
				mContext.finish();
			}
		});
	}

	@Override
	protected void onPreExecute() {
		mProgressDialog.show();
	}

	@Override
	protected void onPostExecute(final Boolean success) {
		mContext.onAuthenticationResult(success.booleanValue());
		if (mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
		}
		if (success)
			mContext.finish();
	}

	@Override
	protected Boolean doInBackground(String... strings) {
		String username = strings[0];
		String password = strings[1];
		int retries = 2;
		boolean result = false;
		while (!result  &&  retries > 0) {
			result = SynchronizedConnectionManager.getInstance().connect(username, password);
			retries--;
		}
		if (!result)
			return false;
		Presence p = new Presence(Presence.Type.available);
		p.setMode(Presence.Mode.away);
		p.setPriority(-127);
		SynchronizedConnectionManager.getInstance().sendPacket(p);
		return true;
	}

	@Override
	protected void onCancelled () {
		SynchronizedConnectionManager.getInstance().disconnect();
	}
}