package com.ipower.tattoo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;


public class SettingsActivity extends PreferenceActivity {

    private static iPowerApplication app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_settings);

        app = (iPowerApplication)getApplicationContext();

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment()).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = super.onCreateView(inflater, container, savedInstanceState);
            if(view != null) {
                ListView listView = (ListView) view.findViewById(android.R.id.list);
                listView.setPadding(iPowerApplication.dpToPx(5), iPowerApplication.dpToPx(10), iPowerApplication.dpToPx(5), iPowerApplication.dpToPx(10));
            }

            return view;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences);

            Preference preference;
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(iPowerApplication.context);

            preference = findPreference("setting_footswitch_mode");
            String[] footswitch_modes = getResources().getStringArray(R.array.footswitch_modes);
            List<String> footswitch_mode_values = Arrays.asList(getResources().getStringArray(R.array.footswitch_mode_values));
            preference.setSummary(footswitch_modes[footswitch_mode_values.indexOf(sharedPreferences.getString("setting_footswitch_mode", "1"))]);

            /*preference = findPreference("setting_language");
            String[] languages = getResources().getStringArray(R.array.languages);
            List<String> language_values = Arrays.asList(getResources().getStringArray(R.array.language_values));
            preference.setSummary(languages[language_values.indexOf(sharedPreferences.getString("setting_language", "en"))]);*/

            preference = findPreference("setting_device_name");
            if (app.isConnState()) {
                preference.setSummary(sharedPreferences.getString("setting_device_name_" + app.getmDevice().getAddress(), ""));
            } else {
                preference.setSummary("");
            }

            preference = findPreference("setting_device_password");
            if (sharedPreferences.getString("setting_device_password", "").length() > 0) {
                preference.setSummary("•••");
            } else {
                preference.setSummary("");
            }

            ShortcutPreference shortcutPreference = (ShortcutPreference) findPreference("setting_liner");
            String text = String.format("%.2f", iPowerApplication.intToVolts(sharedPreferences.getInt("setting_liner", 0)));
            shortcutPreference.setSummary(shortcutPreference.getSuffix() == null ? text : text.concat(" " + shortcutPreference.getSuffix()));

            shortcutPreference = (ShortcutPreference) findPreference("setting_shader");
            text = String.format("%.2f", iPowerApplication.intToVolts(sharedPreferences.getInt("setting_shader", 0)));
            shortcutPreference.setSummary(shortcutPreference.getSuffix() == null ? text : text.concat(" " + shortcutPreference.getSuffix()));

            PresetPreference presetPreference = (PresetPreference) findPreference("setting_machine_1");
            String name = sharedPreferences.getString("setting_machine_1_name", null);
            if (name != null)
                presetPreference.setTitle(name);
            text = String.format("%.2f", iPowerApplication.intToVolts(sharedPreferences.getInt("setting_machine_1", 0)));
            presetPreference.setSummary(presetPreference.getSuffix() == null ? text : text.concat(" " + presetPreference.getSuffix()));

            presetPreference = (PresetPreference) findPreference("setting_machine_2");
            name = sharedPreferences.getString("setting_machine_2_name", null);
            if (name != null)
                presetPreference.setTitle(name);
            text = String.format("%.2f", iPowerApplication.intToVolts(sharedPreferences.getInt("setting_machine_2", 0)));
            presetPreference.setSummary(presetPreference.getSuffix() == null ? text : text.concat(" " + presetPreference.getSuffix()));

            presetPreference = (PresetPreference) findPreference("setting_machine_3");
            name = sharedPreferences.getString("setting_machine_3_name", null);
            if (name != null)
                presetPreference.setTitle(name);
            text = String.format("%.2f", iPowerApplication.intToVolts(sharedPreferences.getInt("setting_machine_3", 0)));
            presetPreference.setSummary(presetPreference.getSuffix() == null ? text : text.concat(" " + presetPreference.getSuffix()));

            presetPreference = (PresetPreference) findPreference("setting_machine_4");
            name = sharedPreferences.getString("setting_machine_4_name", null);
            if (name != null)
                presetPreference.setTitle(name);
            text = String.format("%.2f", iPowerApplication.intToVolts(sharedPreferences.getInt("setting_machine_4", 0)));
            presetPreference.setSummary(presetPreference.getSuffix() == null ? text : text.concat(" " + presetPreference.getSuffix()));

            presetPreference = (PresetPreference) findPreference("setting_machine_5");
            name = sharedPreferences.getString("setting_machine_5_name", null);
            if (name != null)
                presetPreference.setTitle(name);
            text = String.format("%.2f", iPowerApplication.intToVolts(sharedPreferences.getInt("setting_machine_5", 0)));
            presetPreference.setSummary(presetPreference.getSuffix() == null ? text : text.concat(" " + presetPreference.getSuffix()));

            AccountPreference accountPreference = (AccountPreference) findPreference("setting_account_name");
            accountPreference.setTitle(sharedPreferences.getString("setting_account_name", ""));
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("setting_footswitch_mode")) {
                Preference preference = findPreference(key);
                String[] footswitch_modes = getResources().getStringArray(R.array.footswitch_modes);
                List<String> footswitch_mode_values = Arrays.asList(getResources().getStringArray(R.array.footswitch_mode_values));
                preference.setSummary(footswitch_modes[footswitch_mode_values.indexOf(sharedPreferences.getString(key, "1"))]);

                if (app.isConnState()) {
                    byte buf[] = new byte[]{(byte)0x00, (byte)0x00, (byte)0x55};

                    if (sharedPreferences.getString(key, "1") == "3") { //In-app toggle
                        buf[0] = 0x73;
                    } else if (sharedPreferences.getString(key, "1") == "2") { //Toggle
                        buf[0] = 0x53;
                    } else { //Continuous
                        buf[0] = 0x13;
                    }

                    app.sendBluetoothData(buf);
                }
            } else if (key.equals("setting_language")) {
                /*Preference preference = findPreference(key);
                String[] languages = getResources().getStringArray(R.array.languages);
                List<String> language_values = Arrays.asList(getResources().getStringArray(R.array.language_values));
                preference.setSummary(languages[language_values.indexOf(sharedPreferences.getString(key, "en"))]);*/
            } else if (key.indexOf("setting_device_name_") == 0) {
                Preference preference = findPreference("setting_device_name");
                if (app.isConnState()) {
                    preference.setSummary(sharedPreferences.getString(key, ""));
                } else {
                    preference.setSummary("");
                }
            } else if (key.equals("setting_device_password")) {
                Preference preference = findPreference(key);
                if (sharedPreferences.getString(key, "").length() > 0) {
                    preference.setSummary("•••");
                } else {
                    preference.setSummary("");
                }
            } else if (key.equals("setting_liner") || key.equals("setting_shader")) {
                ShortcutPreference preference = (ShortcutPreference) findPreference(key);
                String text = String.format("%.2f", iPowerApplication.intToVolts(sharedPreferences.getInt(key, 0)));
                preference.setSummary(preference.getSuffix() == null ? text : text.concat(" " + preference.getSuffix()));
            } else if (key.equals("setting_machine_1")) {
                PresetPreference preference = (PresetPreference) findPreference(key);
                String text = String.format("%.2f", iPowerApplication.intToVolts(sharedPreferences.getInt(key, 0)));
                preference.setSummary(preference.getSuffix() == null ? text : text.concat(" " + preference.getSuffix()));
            } else if (key.equals("setting_machine_2")) {
                PresetPreference preference = (PresetPreference) findPreference(key);
                String text = String.format("%.2f", iPowerApplication.intToVolts(sharedPreferences.getInt(key, 0)));
                preference.setSummary(preference.getSuffix() == null ? text : text.concat(" " + preference.getSuffix()));
            } else if (key.equals("setting_machine_3")) {
                PresetPreference preference = (PresetPreference) findPreference(key);
                String text = String.format("%.2f", iPowerApplication.intToVolts(sharedPreferences.getInt(key, 0)));
                preference.setSummary(preference.getSuffix() == null ? text : text.concat(" " + preference.getSuffix()));
            } else if (key.equals("setting_machine_4")) {
                PresetPreference preference = (PresetPreference) findPreference(key);
                String text = String.format("%.2f", iPowerApplication.intToVolts(sharedPreferences.getInt(key, 0)));
                preference.setSummary(preference.getSuffix() == null ? text : text.concat(" " + preference.getSuffix()));
            } else if (key.equals("setting_machine_5")) {
                PresetPreference preference = (PresetPreference) findPreference(key);
                String text = String.format("%.2f", iPowerApplication.intToVolts(sharedPreferences.getInt(key, 0)));
                preference.setSummary(preference.getSuffix() == null ? text : text.concat(" " + preference.getSuffix()));
            } else if (key.equals("setting_machine_1_name")) {
                PresetPreference preference = (PresetPreference) findPreference("setting_machine_1");
                String name = sharedPreferences.getString(key, null);
                if (name != null)
                    preference.setTitle(name);
            } else if (key.equals("setting_machine_2_name")) {
                PresetPreference preference = (PresetPreference) findPreference("setting_machine_2");
                String name = sharedPreferences.getString(key, null);
                if (name != null)
                    preference.setTitle(name);
            } else if (key.equals("setting_machine_3_name")) {
                PresetPreference preference = (PresetPreference) findPreference("setting_machine_3");
                String name = sharedPreferences.getString(key, null);
                if (name != null)
                    preference.setTitle(name);
            } else if (key.equals("setting_machine_4_name")) {
                PresetPreference preference = (PresetPreference) findPreference("setting_machine_4");
                String name = sharedPreferences.getString(key, null);
                if (name != null)
                    preference.setTitle(name);
            } else if (key.equals("setting_machine_5_name")) {
                PresetPreference preference = (PresetPreference) findPreference("setting_machine_5");
                String name = sharedPreferences.getString(key, null);
                if (name != null)
                    preference.setTitle(name);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        }

        @Override
        public void onPause() {
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }
    }
}
