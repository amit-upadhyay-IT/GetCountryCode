package com.amitupadhyay.getcountrycode;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amitupadhyay.getcountrycode.CountryCodesAdapter.CountryCode;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberValidation extends ToolbarActivity {
    static final String TAG = NumberValidation.class.getSimpleName();

    public static final String PARAM_FROM_INTERNAL = "org.kontalk.internal";


    private EditText mNameText;
    private Spinner mCountryCode;
    private EditText mPhone;
    private Button mValidateButton;
    private MaterialDialog mProgress;
    Handler mHandler;

    private String mPhoneNumber;
    private String mName;

    private boolean mForce;

    boolean mFromInternal;
    /** Runnable for delaying initial manual sync starter. */
    Runnable mSyncStart;



    /**
     * Compatibility method for {@link PhoneNumberUtil#getSupportedRegions()}.
     * This was introduced because crappy Honeycomb has an old version of
     * libphonenumber, therefore Dalvik will insist on we using it.
     * In case getSupportedRegions doesn't exist, getSupportedCountries will be
     * used.
     */
    @SuppressWarnings("unchecked")
    private Set<String> getSupportedRegions(PhoneNumberUtil util) {
        try {
            return (Set<String>) util.getClass()
                    .getMethod("getSupportedRegions")
                    .invoke(util);
        }
        catch (NoSuchMethodException e) {
            try {
                return (Set<String>) util.getClass()
                        .getMethod("getSupportedCountries")
                        .invoke(util);
            }
            catch (Exception helpme) {
                // ignored
            }
        }
        catch (Exception e) {
            // ignored
        }

        return new HashSet<>();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.number_validation);
        setupToolbar(false);

        mHandler = new Handler();

        final Intent intent = getIntent();
        mFromInternal = intent.getBooleanExtra(PARAM_FROM_INTERNAL, false);

        mNameText = (EditText) findViewById(R.id.name);
        mCountryCode = (Spinner) findViewById(R.id.phone_cc);
        mPhone = (EditText) findViewById(R.id.phone_number);
        mValidateButton = (Button) findViewById(R.id.button_validate);

        // populate country codes
        final CountryCodesAdapter ccList = new CountryCodesAdapter(this,
                android.R.layout.simple_list_item_1,
                android.R.layout.simple_spinner_dropdown_item);
        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        Set<String> ccSet = getSupportedRegions(util);
        for (String cc : ccSet)
            ccList.add(cc);

        ccList.sort(new Comparator<CountryCodesAdapter.CountryCode>() {
            public int compare(CountryCodesAdapter.CountryCode lhs, CountryCodesAdapter.CountryCode rhs) {
                return lhs.regionName.compareTo(rhs.regionName);
            }
        });
        mCountryCode.setAdapter(ccList);
        mCountryCode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ccList.setSelected(position);
            }

            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });

        // FIXME this doesn't consider creation because of configuration change
        Phonenumber.PhoneNumber myNum = getMyNumber(this);
        if (myNum != null) {
            CountryCode cc = new CountryCode();
            cc.regionCode = util.getRegionCodeForNumber(myNum);
            if (cc.regionCode == null)
                cc.regionCode = util.getRegionCodeForCountryCode(myNum.getCountryCode());
            mCountryCode.setSelection(ccList.getPositionForId(cc));
            mPhone.setText(String.valueOf(myNum.getNationalNumber()));
        }
        else {
            final TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            final String regionCode = tm.getSimCountryIso().toUpperCase(Locale.US);
            CountryCode cc = new CountryCode();
            cc.regionCode = regionCode;
            cc.countryCode = util.getCountryCodeForRegion(regionCode);
            mCountryCode.setSelection(ccList.getPositionForId(cc));
        }

        // listener for autoselecting country code from typed phone number
        mPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // unused
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // unused
            }

            @Override
            public void afterTextChanged(Editable s) {
                syncCountryCodeSelector();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        keepScreenOn(false);

        if (mProgress != null) {
            if (isFinishing())
                mProgress.cancel();
            else
                mProgress.dismiss();
        }
    }

    void keepScreenOn(boolean active) {
        if (active)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /** Sync country code with text entered by the user, if possible. */
    void syncCountryCodeSelector() {
        try {
            PhoneNumberUtil util = PhoneNumberUtil.getInstance();
            CountryCode cc = (CountryCode) mCountryCode.getSelectedItem();
            Phonenumber.PhoneNumber phone = util.parse(mPhone.getText().toString(), cc != null ? cc.regionCode : null);
            // autoselect correct country if user entered country code too
            if (phone.hasCountryCode()) {
                CountryCode ccLookup = new CountryCode();
                ccLookup.regionCode = util.getRegionCodeForNumber(phone);
                ccLookup.countryCode = phone.getCountryCode();
                int position = ((CountryCodesAdapter) mCountryCode.getAdapter()).getPositionForId(ccLookup);
                if (position >= 0) {
                    mCountryCode.setSelection(position);
                }
            }
        }
        catch (NumberParseException e) {
            // ignored
        }
    }

    private void enableControls(boolean enabled) {
        mValidateButton.setEnabled(enabled);
        mCountryCode.setEnabled(enabled);
        mPhone.setEnabled(enabled);
    }

    private void error(int message) {
        new MaterialDialog.Builder(this)
                .content(message)
                .positiveText(android.R.string.ok)
                .show();
    }

    private boolean checkInput(boolean importing) {
        String phoneStr;

        // check name first
        if (!importing) {
            mName = mNameText.getText().toString().trim();
            if (mName.length() == 0) {
                error(R.string.msg_no_name);
                return false;
            }
        }

        String phoneInput = mPhone.getText().toString();
        // if the user entered a phone number use it even when importing for backward compatibility
        if (!importing || !phoneInput.isEmpty()){
            PhoneNumberUtil util = PhoneNumberUtil.getInstance();
            CountryCode cc = (CountryCode) mCountryCode.getSelectedItem();
            if (!BuildConfig.DEBUG) {
                Phonenumber.PhoneNumber phone;
                try {
                    phone = util.parse(phoneInput, cc.regionCode);
                    // autoselect correct country if user entered country code too
                    if (phone.hasCountryCode()) {
                        CountryCode ccLookup = new CountryCode();
                        ccLookup.regionCode = util.getRegionCodeForNumber(phone);
                        ccLookup.countryCode = phone.getCountryCode();
                        int position = ((CountryCodesAdapter) mCountryCode.getAdapter()).getPositionForId(ccLookup);
                        if (position >= 0) {
                            mCountryCode.setSelection(position);
                            cc = (CountryCode) mCountryCode.getItemAtPosition(position);
                        }
                    }
                    // handle special cases
                    handleSpecialCases(phone);
                    if (!util.isValidNumberForRegion(phone, cc.regionCode) && !isSpecialNumber(phone))
                        throw new NumberParseException(NumberParseException.ErrorType.INVALID_COUNTRY_CODE, "invalid number for region " + cc.regionCode);
                }
                catch (NumberParseException e1) {
                    error(R.string.msg_invalid_number);
                    return false;
                }

                // check phone number format
                phoneStr = util.format(phone, PhoneNumberUtil.PhoneNumberFormat.E164);
                if (!PhoneNumberUtils.isWellFormedSmsAddress(phoneStr)) {
                    Log.i(TAG, "not a well formed SMS address");
                }
            }
            else {
                phoneStr = String.format(Locale.US, "+%d%s", cc.countryCode, mPhone.getText().toString());
            }

            // phone is null - invalid number
            if (phoneStr == null) {
                Toast.makeText(this, R.string.warn_invalid_number, Toast.LENGTH_SHORT)
                        .show();
                return false;
            }

            Log.v(TAG, "Using phone number to register: " + phoneStr);
            mPhoneNumber = phoneStr;
        }
        else {
            // we will use the data from the imported key
            mName = null;
            mPhoneNumber = null;
        }

        return true;
    }

    void startValidation(boolean force, boolean fallback) {
        mForce = force;
        enableControls(false);

        if (!checkInput(false)) {
            enableControls(true);
        }
    }

    /**
     * Begins validation of the phone number.
     * Also used by the view definition as the {@link View.OnClickListener}.
     */
    public void validatePhone(View v) {
        keepScreenOn(true);
        startValidation(false, false);
    }


    /** Returns the (parsed) number stored in this device SIM card. */
    @SuppressLint("HardwareIds")
    public Phonenumber.PhoneNumber getMyNumber(Context context) {
        try {
            final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            final String regionCode = tm.getSimCountryIso().toUpperCase(Locale.US);
            return PhoneNumberUtil.getInstance().parse(tm.getLine1Number(), regionCode);
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * Handles special cases in a parsed phone number.
     * @param phoneNumber the phone number to check. It will be modified in place.
     */
    public void handleSpecialCases(Phonenumber.PhoneNumber phoneNumber) {
        PhoneNumberUtil util = PhoneNumberUtil.getInstance();

        // Argentina numbering rules
        int argCode = util.getCountryCodeForRegion("AR");
        if (phoneNumber.getCountryCode() == argCode) {
            // forcibly add the 9 between country code and national number
            long nsn = phoneNumber.getNationalNumber();
            if (firstDigit(nsn) != 9) {
                phoneNumber.setNationalNumber(addSignificantDigits(nsn, 9));
            }
        }
    }

    private int firstDigit(long n) {
        while (n < -9 || 9 < n) n /= 10;
        return (int) Math.abs(n);
    }

    private long addSignificantDigits(long n, int ds) {
        final long orig = n;
        int count = 1;
        while (n < -9 || 9 < n) {
            n /= 10;
            count++;
        }
        long power = ds * (long) Math.pow(10, count);
        return orig + power;
    }

    /** Handles special numbers not handled by libphonenumber. */
    public boolean isSpecialNumber(Phonenumber.PhoneNumber number) {
        if (number.getCountryCode() == 31) {
            // handle special M2M numbers: 11 digits starting with 097[0-8]
            final Pattern regex = Pattern.compile("^97[0-8][0-9]{8}$");
            Matcher m = regex.matcher(String.valueOf(number.getNationalNumber()));
            return m.matches();
        }
        return false;
    }
}
