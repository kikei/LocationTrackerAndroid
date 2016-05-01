package net.xaxxi.locationtracker;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.app.FragmentActivity;

import net.xaxxi.locationtracker.preference.AccountPreference;

public class MainPreferencesActivity extends FragmentActivity {
    private static final String TAG = "LocationTracker.MainPreferencesActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PRNGFixes.apply();
        
        getFragmentManager()
            .beginTransaction()
            .replace(android.R.id.content,
                     new MainPreferenceFragment()).commit();
    }

    public static class MainPreferenceFragment extends PreferenceFragment {
        
        LocationTrackerClient mClient;
        Model mModel;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            mModel = new Model(getActivity());
            mClient = LocationTrackerClient.getInstance(mModel);
            
            Preference.OnPreferenceClickListener listener;

            final AccountPreference accountPref =
                (AccountPreference)findPreference(mModel.PREF_USER_NAME);

            Preference syncButton = (Preference)
                findPreference(getString(R.string.pref_sync_location));
            listener = new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference pref) {
                        android.util.Log.d(TAG, "sync button clicked");
                        if (mModel == null) return true;
                        String userName = mModel.getUserName();
                        android.util.Log.d(TAG,
                                           "sync button clicked" +
                                           ", user name=" + userName +
                                           ", client=" + mClient);
                        if (userName == null) {
                            accountPref.onClick();
                        } else {
                            if (mClient == null) return true;
                            mClient.synchronize();
                        }
                        return true;
                    }
                };
            syncButton.setOnPreferenceClickListener(listener);
        }

        /*
        private void changeAccount(final Preference pref) {
              Intent intent = new Intent(MainPreferencesActivity.this,
              AuthenticationActivity.class);
              final int REQUEST_CODE = 1601;
              startActivityForResult(intent, REQUEST_CODE);
        }
        */

    }

}
