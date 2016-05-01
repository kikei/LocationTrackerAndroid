package net.xaxxi.locationtracker.preference;

import android.content.Context;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.AttributeSet;
import android.view.View;

import net.xaxxi.locationtracker.GetAccountDialogFragment;

public class AccountPreference extends Preference {

    Context mContext = null;

    public AccountPreference(Context context) {
        super(context);
        mContext = context;
    }

    public AccountPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public AccountPreference(Context context, AttributeSet attrs,
                             int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
    }

    @Override
    protected void onAttachedToActivity() {
        super.onAttachedToActivity();
        android.util.Log.d("LocationTracker.AccountPreference",
                           "onAttachedToActivity getSummary=" + getSummary());
    }

    @Override
    protected View onCreateView(android.view.ViewGroup parent) {
        String accountName = getPersistedString("no account");
        setSummary(accountName);
        return super.onCreateView(parent);
    }

    @Override
    public void onClick() {
        android.util.Log.d("LocationTracker" +
                           ".MainPreferencesActivity",
                           "account button clicked");
        if (mContext == null) return;
        final FragmentActivity activity = (FragmentActivity)mContext;
        FragmentManager manager = activity.getSupportFragmentManager();
        
        String tag =
            GetAccountDialogFragment.class.getSimpleName();
        GetAccountDialogFragment dialog =
            GetAccountDialogFragment.newInstance();
        GetAccountDialogFragment.GetAccountListener listener = 
            new GetAccountDialogFragment.GetAccountListener() {
                @Override
                public void onGetAccountName(String accountName) {
                    setSummary(accountName);
                    boolean saved = false;
                    if (shouldPersist()) {
                        saved = persistString(accountName);
                        notifyChanged();
                    }
                    android.util.Log.d("LocationTracker.AccountPreference",
                                       "onGetAccountName" +
                                       " shouldPersist=" + shouldPersist() +
                                       ", isPersistent=" + isPersistent() +
                                       ", saved=" + saved +
                                       ", value=" + getPersistedString("_"));
                }
            };
        dialog.setGetAccountListener(listener);
        dialog.show(manager, tag);
    }

}
