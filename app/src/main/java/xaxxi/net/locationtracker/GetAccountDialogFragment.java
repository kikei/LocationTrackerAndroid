package net.xaxxi.locationtracker;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;

public class GetAccountDialogFragment extends DialogFragment {

    public interface GetAccountListener {
        public void onGetAccountName(String accountName);
    }

    // Now supported only google account
    private static final String ACCOUNT_TYPE = "com.google";
    private static int REQUEST_CODE = 0;

    GetAccountListener mGetAccountListener = null;

    public static GetAccountDialogFragment newInstance() {
        GetAccountDialogFragment f = new GetAccountDialogFragment();
        return f;
    }

    public void setGetAccountListener(GetAccountListener listener) {
        mGetAccountListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();

        Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.fragment_dialog_get_account);

        AccountManager accountManager = AccountManager.get(getActivity());
        Intent intent =
            accountManager.newChooseAccountIntent(null, null,
                                                  new String[] { ACCOUNT_TYPE },
                                                  false, null,
                                                  null, null, null);
        startActivityForResult(intent, REQUEST_CODE);

        return dialog;
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode,
                                 final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            String accountName = 
                data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            android.util.Log.d("LocationTracker.GetAccountDialogFragment",
                               "onActivityResult accountName=" + accountName);
            if (mGetAccountListener != null)
                mGetAccountListener.onGetAccountName(accountName);
            dismiss();
        }
    }
}
