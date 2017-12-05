# GetCountryCode

Use compile 'com.googlecode.libphonenumber:libphonenumber:8.0.1' depencency. This makes task easy and provide accurate result because its by google
https://github.com/googlei18n/libphonenumber


Then you can make your own Custom Adapter like I have made here:

    public class CountryCodesAdapter extends BaseAdapter {

    private final LayoutInflater mInflater;
    private final List<CountryCode> mData;
    private final int mViewId;
    private final int mDropdownViewId;
    private int mSelected;

    public static final class CountryCode implements Comparable<String> {
        public String regionCode;
        public int countryCode;
        public String regionName;

        @Override
        public int compareTo(String another) {
            return regionCode != null && another != null ? regionCode.compareTo(another) : 1;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;

            if (o != null && o instanceof CountryCode) {
                CountryCode other = (CountryCode) o;

                return regionCode != null &&
                        regionCode.equals(other.regionCode);
            }

            return false;
        }

        @Override
        public String toString() {
            return regionCode;
        }
    }

    public CountryCodesAdapter(Context context, int viewId, int dropdownViewId) {
        this(context, new ArrayList<CountryCode>(), viewId, dropdownViewId);
    }

    public CountryCodesAdapter(Context context, List<CountryCode> data, int viewId, int dropdownViewId) {
        mInflater = LayoutInflater.from(context);
        mData = data;
        mViewId = viewId;
        mDropdownViewId = dropdownViewId;
    }

    public void add(CountryCode entry) {
        mData.add(entry);
    }

    public void add(String regionCode) {
        CountryCode cc = new CountryCode();
        cc.regionCode = regionCode;
        cc.countryCode = PhoneNumberUtil.getInstance().getCountryCodeForRegion(regionCode);
        cc.regionName = getRegionDisplayName(regionCode, Locale.getDefault());
        mData.add(cc);
    }

    public void clear() {
        mData.clear();
    }

    public void sort(Comparator<? super CountryCode> comparator) {
        Collections.sort(mData, comparator);
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        CountryCode e = mData.get(position);
        return (e != null) ? e.countryCode : -1;
    }

    public int getPositionForId(CountryCode cc) {
        return cc != null ? mData.indexOf(cc) : -1;
    }

    public void setSelected(int position) {
        mSelected = position;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        CheckedTextView textView;
        View view;
        if (convertView == null) {
            view = mInflater.inflate(mDropdownViewId, parent, false);
            textView = (CheckedTextView) view.findViewById(android.R.id.text1);
            view.setTag(textView);
        }
        else {
            view = convertView;
            textView = (CheckedTextView) view.getTag();
        }

        CountryCode e = mData.get(position);

        StringBuilder text = new StringBuilder(5)
                .append(e.regionName)
                .append(" (+")
                .append(e.countryCode)
                .append(')');

        textView.setText(text);
        textView.setChecked((mSelected == position));

        return view;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView;
        View view;
        if (convertView == null) {
            view = mInflater.inflate(mViewId, parent, false);
            textView = (TextView) view.findViewById(android.R.id.text1);
            view.setTag(textView);
        }
        else {
            view = convertView;
            textView = (TextView) view.getTag();
        }

        CountryCode e = mData.get(position);

        StringBuilder text = new StringBuilder(3)
                .append('+')
                .append(e.countryCode)
                .append(" (")
                .append(e.regionName)
                .append(')');

        textView.setText(text);

        return view;
    }

    /** Returns the localized region name for the given region code. */
    public String getRegionDisplayName(String regionCode, Locale language) {
        return (regionCode == null || regionCode.equals("ZZ") ||
                regionCode.equals(PhoneNumberUtil.REGION_CODE_FOR_NON_GEO_ENTITY))
                ? "" : new Locale("", regionCode).getDisplayCountry(language);
    }
    }
    
Then you can create the Activity where you want to display all country codes, I have made a layout like this:

    <?xml version="1.0" encoding="utf-8"?>
    <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
        android:layout_height="match_parent"
        android:layout_width="match_parent"
        android:orientation="vertical">
        <include layout="@layout/toolbar_simple"/>
        <ScrollView
            android:layout_width="fill_parent"
            android:layout_height="fill_parent"
            android:scrollbars="vertical" >

            <LinearLayout
                android:layout_width="fill_parent"
                android:layout_height="wrap_content"
                android:gravity="center_horizontal"
                android:orientation="vertical"
                android:padding="18dip" >

                <Spinner android:id="@+id/phone_cc"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:spinnerMode="dialog"/>

                <EditText
                    android:id="@+id/phone_number"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:hint="@string/hint_validation_number"
                    android:inputType="phone" />

                <Button
                    android:id="@+id/button_validate"
                    android:layout_width="match_parent"
                    android:layout_height="fill_parent"
                    android:text="@string/button_validate" />

               </LinearLayout>

        </ScrollView>
    </LinearLayout>



My java Activity code is:

    import android.annotation.SuppressLint;
    import android.content.Context;
    import android.os.Bundle;
    import android.telephony.TelephonyManager;
    import android.view.View;
    import android.widget.AdapterView;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.Spinner;

    import com.amitupadhyay.getcountrycode.CountryCodesAdapter.CountryCode;
    import com.google.i18n.phonenumbers.PhoneNumberUtil;
    import com.google.i18n.phonenumbers.Phonenumber;

    import java.util.Comparator;
    import java.util.HashSet;
    import java.util.Locale;
    import java.util.Set;

    public class NumberValidation extends ToolbarActivity {

        private Spinner mCountryCode;
        private EditText mPhone;
        private Button mValidateButton;

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
    }
