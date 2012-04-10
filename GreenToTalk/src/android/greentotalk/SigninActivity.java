/*
 * Copyright (C) 2010 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package android.greentotalk;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Activity which displays login screen to the user.
 */
public class SigninActivity extends Activity {
	public static final String PARAM_PASSWORD = "password";
	public static final String PARAM_USERNAME = "username";
	public static final String GMAIL_DOMAIN = "gmail.com";
	public static final String USER_DISCONNECTED = "user_disconnected";
	private static final String TAG = "AuthenticatorActivity";

	/** for posting authentication attempts back to UI thread */
	private TextView mMessage;
	private EditText mPasswordEdit;

	private String mUsername;
	private String mPassword;
	private EditText mUsernameEdit;
	private GreenToTalkApplication mApplication;
	private SharedPreferences mSettings;
	private SynchronizedConnectionManager mConnectionManager;
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		mConnectionManager = SynchronizedConnectionManager.getInstance();
		initializeUI();
	}

	//used in onCreate() and onConfigurationChanged() to set up the UI elements
	public void initializeUI() {
		mApplication = (GreenToTalkApplication)getApplication();
		if (mConnectionManager.isConnected()) {
			// go to the pick friend activity
			pickFreindsActivity();
			finish();
			return;
		}
		mSettings = PreferenceManager.getDefaultSharedPreferences(mApplication);
		mUsername = mSettings.getString(GreenToTalkApplication.ACCOUNT_USERNAME_KEY, "");
		mPassword = mSettings.getString(GreenToTalkApplication.ACCOUNT_PASSWORD_KEY, "");

		setContentView(R.layout.login_activity);

		mMessage = (TextView) findViewById(R.id.message);
		mUsernameEdit = (EditText) findViewById(R.id.username_edit);
		mPasswordEdit = (EditText) findViewById(R.id.password_edit);

		mMessage.setText(getMessage());
		mUsernameEdit.setText(mUsername);
		mPasswordEdit.setText(mPassword);

		Button okLogin = (Button)findViewById(R.id.ok_button);
		okLogin.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				handleLogin();
			}
		});
	}

	/**
	 * Handles onClick event on the Submit button. Sends username/password to
	 * the server for authentication.
	 */
	private void handleLogin() {
		mUsername = mUsernameEdit.getText().toString();
		mPassword = mPasswordEdit.getText().toString();
		if (TextUtils.isEmpty(mUsername) || TextUtils.isEmpty(mPassword)) {
			mMessage.setText(getMessage());
		} else {
			String email = (mUsername.endsWith("@"+GMAIL_DOMAIN))? mUsername : mUsername+"@"+GMAIL_DOMAIN; 
			// Start authenticating...
			new AsyncConnectionTask(this).execute(email, mPassword);
		}
	}

	/**
	 * Called when response is received from the server for authentication
	 * request. See onAuthenticationResult(). 
	 * 
	 */

	protected void pickFreindsActivity() {
		startActivity(new Intent(this,PickFreindsActivity.class));
	}

	/**
	 * Called when the authentication process completes (see attemptLogin()).
	 */
	public void onAuthenticationResult(boolean result) {
		Log.i(TAG, "onAuthenticationResult(" + result + ")");
		if (result) {
			if (!mApplication.isAccountConfigured() || isNewUser()) {
				// save username and password
				SharedPreferences.Editor edit = mSettings.edit();
				edit.putString(GreenToTalkApplication.ACCOUNT_USERNAME_KEY, mUsername);
				edit.putString(GreenToTalkApplication.ACCOUNT_PASSWORD_KEY, mPassword);
				edit.commit();
			}
			pickFreindsActivity();
		} else {
			Log.e(TAG, "onAuthenticationResult: failed to authenticate");
			if (!mApplication.isAccountConfigured() || isNewUser()) {
				// Please enter a valid username/password.
				mMessage.setText(getText(R.string.login_activity_loginfail_text_both));				
			}
			else {
				// there are saved username and not valid password. need to ask for the new password
				// NEED TO CONSIDER ALSO NO CONNECTION OPTION (WE GET HERE ALSO WHEN NO WIFI/INTERNET CONNECTION)
				mMessage.setText(getText(R.string.login_activity_loginfail_text_pwonly));
			}
			mPasswordEdit.setText("");
			mPasswordEdit.requestFocus();
		}
	}
	
	private boolean isNewUser() {
		return !mUsername.equals(mSettings.getString(GreenToTalkApplication.ACCOUNT_USERNAME_KEY, null));
	}

	/**
	 * Returns the message to be displayed at the top of the login dialog box.
	 */
	private CharSequence getMessage() {
		getString(R.string.label);
		if (TextUtils.isEmpty(mUsername)) {
			// If no username, then we ask the user to log in using an
			// appropriate service.
			final CharSequence msg =
					getText(R.string.login_activity_newaccount_text);
			return msg;
		}
		if (TextUtils.isEmpty(mPassword)) {
			// We have an account but no password
			return getText(R.string.login_activity_loginfail_text_pwmissing);
		}
		return null;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ECLAIR
				&& keyCode == KeyEvent.KEYCODE_BACK
				&& event.getRepeatCount() == 0) {
			// Take care of calling this method on earlier versions of
			// the platform where it doesn't exist.
			onBackPressed();
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onBackPressed() {
		Log.i(TAG, "back pressed and user disconnected is " + getIntent().getBooleanExtra(USER_DISCONNECTED, false));
		if (getIntent().getBooleanExtra(USER_DISCONNECTED, false)) {
			finish();
			return;
		}
		super.onBackPressed();
	}
}
