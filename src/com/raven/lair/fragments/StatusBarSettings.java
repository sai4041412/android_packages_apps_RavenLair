package com.raven.lair.fragments;

import com.android.internal.logging.nano.MetricsProto;

import android.os.Bundle;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;
import android.content.ContentResolver;
import android.content.res.Resources;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceCategory;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragment;
import androidx.preference.SwitchPreference;
import android.provider.Settings;
import com.android.settings.R;

import java.util.Locale;
import android.text.TextUtils;
import android.view.View;

import com.android.settings.SettingsPreferenceFragment;
import com.corvus.support.preferences.CustomSeekBarPreference;
import com.corvus.support.preferences.SystemSettingSwitchPreference;
import com.android.settings.Utils;
import android.util.Log;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

public class StatusBarSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String NETWORK_TRAFFIC_FONT_SIZE  = "network_traffic_font_size";

    private ListPreference mNetTrafficLocation;
    private ListPreference mNetTrafficType;
    private ListPreference mNetTrafficLayout;
    private CustomSeekBarPreference mNetTrafficSize;
    private CustomSeekBarPreference mThreshold;
    private SystemSettingSwitchPreference mShowArrows;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.statusbar);

        PreferenceScreen prefSet = getPreferenceScreen();
	final ContentResolver resolver = getActivity().getContentResolver();

        int NetTrafficSize = Settings.System.getInt(resolver,
                Settings.System.NETWORK_TRAFFIC_FONT_SIZE, 42);
        mNetTrafficSize = (CustomSeekBarPreference) findPreference(NETWORK_TRAFFIC_FONT_SIZE);
        mNetTrafficSize.setValue(NetTrafficSize / 1);
        mNetTrafficSize.setOnPreferenceChangeListener(this);

        int type = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_TYPE, 0, UserHandle.USER_CURRENT);
        mNetTrafficType = (ListPreference) findPreference("network_traffic_type");
        mNetTrafficType.setValue(String.valueOf(type));
        mNetTrafficType.setSummary(mNetTrafficType.getEntry());
        mNetTrafficType.setOnPreferenceChangeListener(this);

        int netlayout = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_LAYOUT, 0, UserHandle.USER_CURRENT);
        mNetTrafficLayout = (ListPreference) findPreference("network_traffic_layout");
        mNetTrafficLayout.setValue(String.valueOf(netlayout));
        mNetTrafficLayout.setSummary(mNetTrafficLayout.getEntry());
        mNetTrafficLayout.setOnPreferenceChangeListener(this);

        mNetTrafficLocation = (ListPreference) findPreference("network_traffic_location");
        int location = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_VIEW_LOCATION, 0, UserHandle.USER_CURRENT);
        mNetTrafficLocation.setOnPreferenceChangeListener(this);

        int value = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD, 1, UserHandle.USER_CURRENT);
        mThreshold = (CustomSeekBarPreference) findPreference("network_traffic_autohide_threshold");
        mThreshold.setValue(value);
        mThreshold.setOnPreferenceChangeListener(this);
        mShowArrows = (SystemSettingSwitchPreference) findPreference("network_traffic_arrow");

        int netMonitorEnabled = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_STATE, 0, UserHandle.USER_CURRENT);
        if (netMonitorEnabled == 1) {
            mNetTrafficLocation.setValue(String.valueOf(location+1));
            updateTrafficLocation(location+1);
        } else {
            mNetTrafficLocation.setValue("0");
            updateTrafficLocation(0); 
        }
        mNetTrafficLocation.setSummary(mNetTrafficLocation.getEntry());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
	if (preference == mNetTrafficLocation) {
            int location = Integer.valueOf((String) objValue);
            int index = mNetTrafficLocation.findIndexOfValue((String) objValue);
            mNetTrafficLocation.setSummary(mNetTrafficLocation.getEntries()[index]);
            if (location > 0) {
                // Convert the selected location mode from our list {0,1,2} and store it to "view location" setting: 0=sb; 1=expanded sb
                Settings.System.putIntForUser(getActivity().getContentResolver(),
                        Settings.System.NETWORK_TRAFFIC_VIEW_LOCATION, location-1, UserHandle.USER_CURRENT);
                // And also enable the net monitor
                Settings.System.putIntForUser(getActivity().getContentResolver(),
                        Settings.System.NETWORK_TRAFFIC_STATE, 1, UserHandle.USER_CURRENT);
                updateTrafficLocation(location+1);
            } else { // Disable net monitor completely
                Settings.System.putIntForUser(getActivity().getContentResolver(),
                        Settings.System.NETWORK_TRAFFIC_STATE, 0, UserHandle.USER_CURRENT);
                updateTrafficLocation(location);
            }
            return true;
        } else if (preference == mNetTrafficLayout) {
            int val = Integer.valueOf((String) objValue);
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.NETWORK_TRAFFIC_LAYOUT, val,
                    UserHandle.USER_CURRENT);
            int index = mNetTrafficLayout.findIndexOfValue((String) objValue);
            mNetTrafficLayout.setSummary(mNetTrafficLayout.getEntries()[index]);
            return true;
        } else if (preference == mThreshold) {
            int val = (Integer) objValue;
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD, val,
                    UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mNetTrafficType) {
            int val = Integer.valueOf((String) objValue);
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.NETWORK_TRAFFIC_TYPE, val,
                    UserHandle.USER_CURRENT);
            int index = mNetTrafficType.findIndexOfValue((String) objValue);
            mNetTrafficType.setSummary(mNetTrafficType.getEntries()[index]);
            return true;
        }  else if (preference == mNetTrafficSize) {
            int width = ((Integer)objValue).intValue();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NETWORK_TRAFFIC_FONT_SIZE, width);
            return true;
        }
        return false;
    }

    public void updateTrafficLocation(int location) {
        switch(location){
            case 0:
                mThreshold.setEnabled(false);
                mShowArrows.setEnabled(false);
                mNetTrafficType.setEnabled(false);
                mNetTrafficLayout.setEnabled(false);
                mNetTrafficSize.setEnabled(false);
                break;
            case 1:
            case 2:
                mThreshold.setEnabled(true);
                mShowArrows.setEnabled(true);
                mNetTrafficType.setEnabled(true);
                mNetTrafficLayout.setEnabled(true);
                mNetTrafficSize.setEnabled(true);
                break;
            default:
                break;
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.CORVUS;
    }
}
