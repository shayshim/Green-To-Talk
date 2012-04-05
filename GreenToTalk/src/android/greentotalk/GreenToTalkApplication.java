package android.greentotalk;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * This class contains informations that needs to be global in the application.
 * Theses informations must be necessary for the activities and the service.
 */
public class GreenToTalkApplication extends Application {

	/* Constants for PREFERENCE_KEY
	 */
	/** Preference key for account username. */
	public static final String ACCOUNT_USERNAME_KEY = "account_username";
	/** Preference key for account password. */
	public static final String ACCOUNT_PASSWORD_KEY = "account_password";
	public static final String DND_AS_AVAILABLE_KEY = "dnd_as_available";
	private boolean mIsAccountConfigured = false;
	private static SharedPreferences mSettings;
	private final PreferenceListener mPreferenceListener = new PreferenceListener();

	@Override
	public void onCreate() {
		super.onCreate();
		mSettings = PreferenceManager.getDefaultSharedPreferences(this);
		String login = mSettings.getString(GreenToTalkApplication.ACCOUNT_USERNAME_KEY, "");
		String password = mSettings.getString(GreenToTalkApplication.ACCOUNT_PASSWORD_KEY, "");
		mIsAccountConfigured = !("".equals(login) || "".equals(password));
		mSettings.registerOnSharedPreferenceChangeListener(mPreferenceListener);
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		mSettings.unregisterOnSharedPreferenceChangeListener(mPreferenceListener);
	}

	/**
	 * Tell if a XMPP account is configured.
	 * @return false if there is no account configured.
	 */
	public boolean isAccountConfigured() {
		return mIsAccountConfigured;
	}

	/**
	 * A listener for all the change in the preference file. It is used to maintain the global state of the application.
	 */
	private class PreferenceListener implements SharedPreferences.OnSharedPreferenceChangeListener {

		/**
		 * Constructor.
		 */
		public PreferenceListener() {
		}

		@Override
		public void onSharedPreferenceChanged(SharedPreferences  sharedPreferences, String key) {
			if (GreenToTalkApplication.ACCOUNT_USERNAME_KEY.equals(key) || GreenToTalkApplication.ACCOUNT_PASSWORD_KEY.equals(key)) {
				String login = mSettings.getString(GreenToTalkApplication.ACCOUNT_USERNAME_KEY, "");
				String password = mSettings.getString(GreenToTalkApplication.ACCOUNT_PASSWORD_KEY, "");
				mIsAccountConfigured = !("".equals(login) || "".equals(password));
			}
		}
	}
	
	public static SharedPreferences getSharedPreferences() {
		return mSettings;
	}
}
