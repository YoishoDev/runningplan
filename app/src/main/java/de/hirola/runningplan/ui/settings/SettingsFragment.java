package de.hirola.runningplan.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import androidx.preference.*;
import de.hirola.runningplan.R;
import de.hirola.runningplan.RunningPlanApplication;
import de.hirola.runningplan.model.RunningPlanViewModel;
import de.hirola.sportsapplications.Global;
import de.hirola.sportsapplications.SportsLibrary;
import de.hirola.sportsapplications.model.User;
import de.hirola.runningplan.util.ModalOptionDialog;
import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

/**
 * Copyright 2021 by Michael Schmidt, Hirola Consulting
 * This software us licensed under the AGPL-3.0 or later.
 *
 * Fragment for app settings.
 *
 * @author Michael Schmidt (Hirola)
 * @since v0.1
 */
public class SettingsFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener, EditTextPreference.OnBindEditTextListener {

    private SportsLibrary sportsLibrary;
    private RunningPlanViewModel viewModel;
    private User appUser;
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // app logger
        sportsLibrary = ((RunningPlanApplication) requireActivity().getApplication()).getSportsLibrary();
        // set the preference ui elements
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        // get the view model for data handling
        viewModel = new RunningPlanViewModel(requireActivity().getApplication(), null);
        // load the app user
        appUser = viewModel.getAppUser();
        // load the preferences
        sharedPreferences = requireContext()
                .getSharedPreferences(getString(R.string.preference_file), Context.MODE_PRIVATE);
        // set default app values if not set
        setAppDefaults();
        // get all preferences from screen
        ArrayList<Preference> preferenceList = getPreferenceList(getPreferenceScreen(), new ArrayList<>());
        // set saved values in ui
        setPreferenceValues(preferenceList);
    }

    @Override
    public boolean onPreferenceChange(@NotNull Preference preference, Object newValue) {
        // add values to local datastore
        String key = preference.getKey();
        // add shared preferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        // boolean preferences
        if (preference instanceof SwitchPreferenceCompat) {
            // add trainings
            if (key.equalsIgnoreCase(Global.UserPreferencesKeys.SAVE_TRAINING)) {
                editor.putBoolean(key, (Boolean) newValue);
            }
            // use location data
            if (key.equalsIgnoreCase(Global.UserPreferencesKeys.USE_LOCATIONS)) {
                // TODO: request for..
                editor.putBoolean(key, (Boolean) newValue);
            }
            // use fine location data
            if (key.equalsIgnoreCase(Global.UserPreferencesKeys.USE_LOCATIONS))
            {
                // TODO: request for..
                editor.putBoolean(key, (Boolean) newValue);
            }
            // use notifications
            if (key.equalsIgnoreCase(Global.UserPreferencesKeys.USE_NOTIFICATIONS)) {
                editor.putBoolean(key, (Boolean) newValue);
            }
            // use sync
            if (key.equalsIgnoreCase(Global.UserPreferencesKeys.USE_SYNC))
            {
                editor.putBoolean(key, (Boolean) newValue);
            }
            // set debug mode
            if (key.equalsIgnoreCase(Global.UserPreferencesKeys.DEBUG_MODE))
            {
                editor.putBoolean(key, (Boolean) newValue);
            }
            // send debug logs or not
            if (key.equalsIgnoreCase(Global.UserPreferencesKeys.SEND_DEBUG_LOG))
            {
                editor.putBoolean(key, (Boolean) newValue);
            }
        }
        // text preferences
        if (preference instanceof EditTextPreference) {
            // email address
            if (key.equalsIgnoreCase(Global.UserPreferencesKeys.USER_EMAIL_ADDRESS)) {
                // TODO: check if valid email format
                String emailAddress = (String) newValue;
                editor.putString(key, emailAddress);
                appUser.setEmailAddress(emailAddress);
            }
            // max pulse
            if (key.equalsIgnoreCase(Global.UserPreferencesKeys.USER_MAX_PULSE)) {
                String maxPulse = (String) newValue;
                try {
                    Integer.parseInt(maxPulse);
                } catch (NumberFormatException exception) {
                    if (sportsLibrary.isDebugMode()) {
                        Logger.debug(exception);
                    }
                    return false;
                }
                editor.putString(key, maxPulse);
                appUser.setMaxPulse(Integer.parseInt(maxPulse));
            }
        }
        // list preferences
        if (preference instanceof ListPreference) {
            if (newValue != null) {
                int index = ((ListPreference) preference).findIndexOfValue((String) newValue);
                String value = "0";
                if (index > -1) {
                    CharSequence[] entryValues = ((ListPreference) preference).getEntryValues();
                    if (entryValues.length > index) {
                        value = (String) entryValues[index];
                    }
                }
                // gender
                if (key.equalsIgnoreCase(Global.UserPreferencesKeys.USER_GENDER)) {
                    editor.putString(key, value);
                    // try to calculate the max pulse
                    calculateMaxPulse();
                    try {
                        appUser.setGender(Integer.parseInt(value));
                    } catch (NumberFormatException exception) {
                        // preference can not set
                        // alert dialog to user
                        ModalOptionDialog.showMessageDialog(ModalOptionDialog.DialogStyle.WARNING,
                                requireContext(),
                                "",
                                getString(R.string.preferences_not_set),
                                getString(R.string.ok));
                        if (sportsLibrary.isDebugMode()) {
                            exception.printStackTrace();
                        }
                        return false;
                    }
                }
                // training level
                if (key.equalsIgnoreCase(Global.UserPreferencesKeys.USER_TRAINING_LEVEL)) {
                    editor.putString(key, value);
                    try {
                        appUser.setTrainingLevel(Integer.parseInt(value));
                    } catch (NumberFormatException exception) {
                        // preference can not set
                        // alert dialog to user
                        ModalOptionDialog.showMessageDialog(ModalOptionDialog.DialogStyle.WARNING,
                                requireContext(),
                                getString(R.string.warning),
                                getString(R.string.preferences_not_set),
                                getString(R.string.ok));
                        if (sportsLibrary.isDebugMode()) {
                            exception.printStackTrace();
                        }
                        return false;
                    }
                }
                    // birthday
                if (key.equalsIgnoreCase(Global.UserPreferencesKeys.USER_BIRTHDAY)) {
                    // build a birthday from selected year
                    editor.putString(key, value);
                    // try to calculate the max pulse
                    calculateMaxPulse();
                    try {
                        int year = Integer.parseInt(value);
                        if (year == 0) {
                            year = LocalDate.now().getYear();
                        }
                        LocalDate birthday = LocalDate.now().withYear(year);
                        appUser.setBirthday(birthday);
                    } catch (NumberFormatException exception) {
                        // preference can not set
                        // alert dialog to user
                        ModalOptionDialog.showMessageDialog(ModalOptionDialog.DialogStyle.WARNING,
                                requireContext(),
                                getString(R.string.warning),
                                getString(R.string.preferences_not_set),
                                getString(R.string.ok));
                        if (sportsLibrary.isDebugMode()) {
                            System.out.println(exception.getMessage());
                        }
                        return false;
                    }
                }
            }
        }
        // save the data
        if (!viewModel.updateObject(appUser)) {
            // data could not saved
            // alert dialog to user
            ModalOptionDialog.showMessageDialog(ModalOptionDialog.DialogStyle.CRITICAL,
                    requireContext(),
                    getString(R.string.error),
                    getString(R.string.save_data_error),
                    getString(R.string.ok));
            if (sportsLibrary.isDebugMode()) {
                Logger.debug("Error while saving settings.");
            }
            return false;
        }
        // add the shared preferences
        editor.apply();
        return true;
    }

    @Override
    public void onBindEditText(@NotNull EditText editText) {
        // only numbers in some preferences
        editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
    }

    private void setAppDefaults() {
        // use notification by default
        // do not look at your phone all the time while running
        boolean useNotifications =
                sharedPreferences.getBoolean(Global.UserPreferencesKeys.USE_NOTIFICATIONS, true);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(Global.UserPreferencesKeys.USE_NOTIFICATIONS, useNotifications);
        editor.apply();
    }

    private void setPreferenceValues(@NotNull ArrayList<Preference> preferenceList) {
        for (Preference preference : preferenceList) {
            if (preference != null) {
                // add the listener
                preference.setOnPreferenceChangeListener(this);
                // get the preference key
                String key = preference.getKey();
                // boolean values
                if (preference instanceof SwitchPreferenceCompat) {
                    try {
                        // preference ui key == shared preference key
                        ((SwitchPreferenceCompat) preference)
                                .setChecked(sharedPreferences.getBoolean(key, false));
                    } catch (ClassCastException exception) {
                        ((SwitchPreferenceCompat) preference).setChecked(false);
                        if (sportsLibrary.isDebugMode()) {
                            Logger.debug(null, exception);
                        }
                    }
                } else if (preference instanceof ListPreference) {
                    if (key.equalsIgnoreCase(Global.UserPreferencesKeys.USER_GENDER)) {
                        // gender: fill the list dynamically with values
                        // Integer = entryValues, String = entries
                        Map<Integer, String> genderValues = Global.TrainingParameter.genderValues;
                        Iterator<String> entriesIterator = genderValues.values().iterator();
                        int arrayCount = genderValues.size();
                        String[] entries = new String[arrayCount];
                        int index = 0;
                        while (entriesIterator.hasNext()) {
                            String entry = entriesIterator.next();
                            // the list contains the strings for resources
                            // load the strings
                            if (entry.length() > 0) {
                                try {
                                    int resourceStringId = requireContext().getResources().getIdentifier(entry,
                                            "string", requireContext().getPackageName());
                                    entry = requireContext().getString(resourceStringId);
                                } catch (Resources.NotFoundException exception) {
                                    entry = getString(R.string.preference_not_found);
                                    if (sportsLibrary.isDebugMode()) {
                                        Logger.debug(null, exception);
                                    }
                                }
                            }
                            entries[index] = entry;
                            index++;
                        }
                        ((ListPreference) preference).setEntries(entries);
                        Iterator<Integer> entryValuesIterator = genderValues.keySet().iterator();
                        String[] entryValues = new String[arrayCount];
                        index = 0;
                        while (entryValuesIterator.hasNext()) {
                            entryValues[index] = String.valueOf(entryValuesIterator.next());
                            index++;
                        }
                        ((ListPreference) preference).setEntryValues(entryValues);
                    } else if (key.equalsIgnoreCase(Global.UserPreferencesKeys.USER_TRAINING_LEVEL)) {
                        // training level: fill the list dynamically with values
                        // Integer = entryValues, String = entries
                        Map<Integer, String> trainingLevel = Global.TrainingParameter.trainingLevel;
                        Iterator<String> entriesIterator = trainingLevel.values().iterator();
                        int arrayCount = trainingLevel.size();
                        String[] entries = new String[arrayCount];
                        int index = 0;
                        while (entriesIterator.hasNext()) {
                            String entry = entriesIterator.next();
                            // the list contains the strings for resources
                            // load the strings
                            if (entry.length() > 0) {
                                try {
                                    int resourceStringId = requireContext().getResources().getIdentifier(entry,
                                            "string", requireContext().getPackageName());
                                    entry = requireContext().getString(resourceStringId);
                                } catch (Resources.NotFoundException exception) {
                                    entry = getString(R.string.preference_not_found);
                                    if (sportsLibrary.isDebugMode()) {
                                        Logger.debug(null, exception);
                                    }
                                }
                            }
                            entries[index] = entry;
                            index++;
                        }
                        ((ListPreference) preference).setEntries(entries);
                        Iterator<Integer> entryValuesIterator = trainingLevel.keySet().iterator();
                        String[] entryValues = new String[arrayCount];
                        index = 0;
                        while (entryValuesIterator.hasNext()) {
                            entryValues[index] = String.valueOf(entryValuesIterator.next());
                            index++;
                        }
                        ((ListPreference) preference).setEntryValues(entryValues);
                    } else if (key.equalsIgnoreCase(Global.UserPreferencesKeys.USER_BIRTHDAY)) {
                        {
                            // birthday(year)
                            // fill the list dynamically with values
                            // year of birthday: actual year - max. 120
                            int actualYear = LocalDate.now().getYear();
                            int maxYearOfBirth = actualYear - 120;
                            int index = 0;
                            String[] entries = new String[120];
                            while (maxYearOfBirth < actualYear) {
                                entries[index] = String.valueOf(maxYearOfBirth);
                                maxYearOfBirth++;
                                index++;
                            }
                            ((ListPreference) preference).setEntries(entries);
                            ((ListPreference) preference).setEntryValues(entries);
                        }
                    }
                } else if (preference instanceof EditTextPreference){
                    // String value (EditTextPreference)
                    try {
                        // preference ui key == shared preference key
                        ((EditTextPreference) preference)
                                .setText(sharedPreferences.getString(preference.getKey(), ""));
                        // in some preferences only numbers allowed
                        if (preference.getKey().equalsIgnoreCase(Global.UserPreferencesKeys.USER_MAX_PULSE)) {
                            ((EditTextPreference) preference).setOnBindEditTextListener(this);
                        }
                    } catch (ClassCastException exception) {
                        ((EditTextPreference) preference).setText("");
                        if (sportsLibrary.isDebugMode()) {
                            Logger.debug(null, exception);
                        }
                    }
                }
            }
        }
        // set the use location of true
        // dependency by xml -> Exception if preferences not exist
        Preference useLocationDataPref = findPreference(Global.UserPreferencesKeys.USE_LOCATIONS);
        Preference useFineLocationDataPref = findPreference(Global.UserPreferencesKeys.USE_FINE_LOCATIONS);
        if (useLocationDataPref != null && useFineLocationDataPref != null) {
            useFineLocationDataPref.setDependency(useLocationDataPref.getKey());
        }
    }

    // thanks to: https://stackoverflow.com/a/15027088/15577485
    private ArrayList<Preference> getPreferenceList(Preference p, ArrayList<Preference> list) {
        if( p instanceof PreferenceCategory || p instanceof PreferenceScreen) {
            PreferenceGroup pGroup = (PreferenceGroup) p;
            int pCount = pGroup.getPreferenceCount();
            for(int i = 0; i < pCount; i++) {
                getPreferenceList(pGroup.getPreference(i), list); // recursive call
            }
        } else {
            list.add(p);
        }
        return list;
    }

    private void calculateMaxPulse() {
        // Maximalpuls bei Männern = 223 – 0,9 x Lebensalter
        // Maximalpuls bei Frauen = 226 – Lebensalter
        int maxPulse = 0;
        int gender = appUser.getGender();
        // we need the age to calculate
        LocalDate birthday = appUser.getBirthday();
        Period periodBetween = Period.between(birthday, LocalDate.now());
        int age = Math.abs(periodBetween.getYears());

        if (gender < 2 || gender > 3 || age < 18) {
            return;
        }
        if (Global.Defaults.valuesForCalculateMaxPulse.containsKey(gender)) {
            maxPulse = Global.Defaults.valuesForCalculateMaxPulse.get(gender);
            if (gender == 2) {
                // male
                maxPulse = maxPulse - (int) (0.9 * age);
            } else {
                // female
                maxPulse = maxPulse - age;
            }
        }
        // save the data
        appUser.setMaxPulse(maxPulse);
        if (!viewModel.updateObject(appUser)) {
            // data could not saved
            // alert dialog to user
            ModalOptionDialog.showMessageDialog(ModalOptionDialog.DialogStyle.CRITICAL,
                    requireContext(),
                    getString(R.string.error),
                    getString(R.string.save_data_error),
                    getString(R.string.ok));
            if (sportsLibrary.isDebugMode()) {
                Logger.debug("Error while saving user data.");
            }
        }

        // add to preferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Global.UserPreferencesKeys.USER_MAX_PULSE, String.valueOf(maxPulse));
        editor.apply();
        // reload the preference in ui
        Preference preference = findPreference(Global.UserPreferencesKeys.USER_MAX_PULSE);
        if (preference instanceof EditTextPreference) {
            ((EditTextPreference) preference).setText(String.valueOf(maxPulse));
        }
    }
}