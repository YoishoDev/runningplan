package de.hirola.runningplan.ui.training;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.*;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import de.hirola.runningplan.R;
import de.hirola.runningplan.RunningPlanApplication;
import de.hirola.runningplan.model.RunningPlanViewModel;
import de.hirola.runningplan.services.training.TrackManager;
import de.hirola.runningplan.services.training.TrainingServiceCallback;
import de.hirola.runningplan.services.training.TrainingServiceConnection;
import de.hirola.runningplan.ui.runningplans.RunningPlansFragment;
import de.hirola.runningplan.util.ModalOptionDialog;
import de.hirola.runningplan.util.TrainingNotificationManager;
import de.hirola.sportsapplications.Global;
import de.hirola.sportsapplications.SportsLibrary;
import de.hirola.sportsapplications.model.UUID;
import de.hirola.sportsapplications.model.*;
import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Copyright 2021 by Michael Schmidt, Hirola Consulting
 * This software us licensed under the AGPL-3.0 or later.
 *
 * Fragment for the training.
 *
 * @author Michael Schmidt (Hirola)
 * @since v0.1
 */
public class TrainingFragment extends Fragment
        implements AdapterView.OnItemSelectedListener, TrainingServiceCallback {

    private SportsLibrary sportsLibrary; // app logging
    private SharedPreferences sharedPreferences; // user and app preferences
    private boolean useNotifications;
    private TrainingNotificationManager notificationManager; // sends notification to user

    // app data
    private RunningPlanViewModel viewModel;
    private User appUser;
    private RunningPlan runningPlan; // the actual running plan, selected by the user
                                     // if no running plan selected, the plan with the lowest order number
                                     // will be selected
    private RunningPlanEntry runningPlanEntry; // training entry for a day
    private RunningUnit runningUnit; // active training unit
    private boolean isTrainingPossible = false; // if no running plan selected, no training is possible
    private boolean isTrainingRunning = false;
    private boolean isTrainingPaused = false;

    // spinner
    private Spinner trainingDaysSpinner;
    private ArrayAdapter<String> trainingDaysSpinnerArrayAdapter;
    private Spinner trainingUnitsSpinner;
    private ArrayAdapter<String>  trainingUnitsSpinnerArrayAdapter;

    // training action button
    private AppCompatImageButton startButton;
    private AppCompatImageButton stopButton;
    private AppCompatImageButton pauseButton;

    // label
    private TextView runningPlanNameLabel;
    private TextView trainingInfolabel;
    private TextView timerLabel; // countdown label

    // training info label
    private ImageView runningPlanStateImageView;
    private ImageView trainingInfoImageView;

    // training data
    private long unitTimeToRun = 0L; // remaining time of running unit in seconds
    private long trainingTime = 0L; // actual time in training in seconds

    private final TrainingServiceConnection trainingServiceConnection =
            new TrainingServiceConnection(this); // trainings service for timer and location updates
    private ActivityResultLauncher<String[]> locationPermissionRequest = null; // must be initialized in onCreate
    private boolean useLocationData; // user want to use location services
    private boolean locationServicesAvailable; // user has using locations allowed and services are available
    private boolean isTrainingServiceConnected = false; //only if connected to service wie can start a training
    private Track.Id trackId = null; // the id of the actual track
    private final BroadcastReceiver backgroundTimeReceiver = new BroadcastReceiver() { // training time from service
        @Override
        public void onReceive(Context context, Intent intent) {
            unitTimeToRun = intent
                    .getExtras()
                    .getLong(TrainingServiceCallback.SERVICE_RECEIVER_INTENT_ACTUAL_DURATION, 0L);
            // refresh the time label
            updateTimerLabel();
            // check the training state
            monitoringTraining();
        }
    };
    private final BroadcastReceiver trackManagerReceiver = new BroadcastReceiver() { // track updates from track manager
        @Override
        public void onReceive(Context context, Intent intent) {
            // show actual track infos
            if (intent.getAction().equals(TrackManager.LOCATION_UPDATE_ACTION)) {
                // get the values
                double distance = intent
                        .getExtras()
                        .getDouble(TrackManager.RECEIVER_INTENT_TRACK_DISTANCE, 0.0);
                // show the values in view
                showTrackInfos(distance);
                return;
            }
            // inform the user, show training infos, save training
            if (intent.getAction().equals(TrackManager.TRACK_COMPLETED_ACTION)) {
                // get the state
                boolean isCompleted = intent
                        .getExtras()
                        .getBoolean(TrackManager.RECEIVER_INTENT_COMPLETE_STATE, false);
                if (!isCompleted) {
                    ModalOptionDialog.showMessageDialog(ModalOptionDialog.DialogStyle.WARNING,
                            requireContext(),
                            getString(R.string.error),
                            getString(R.string.recorded_track_not_completed),
                            getString(R.string.ok));
                    return;
                }
                if (locationServicesAvailable) {
                    // get the recorded data
                    Track recordedTrack = trainingServiceConnection.getRecordedTrack(trackId);
                    if (recordedTrack == null) {
                        ModalOptionDialog.showMessageDialog(ModalOptionDialog.DialogStyle.WARNING,
                                requireContext(),
                                getString(R.string.error),
                                getString(R.string.recorded_track_not_found),
                                getString(R.string.ok));
                    } else {
                        // show training infos
                        showLastTraining();
                        // save training
                        boolean saveTrainings = sharedPreferences
                                .getBoolean(Global.UserPreferencesKeys.SAVE_TRAINING, false);
                        if (saveTrainings) {
                            if (!saveTraining(recordedTrack)) {
                                // error while saving the training
                                ModalOptionDialog.showMessageDialog(ModalOptionDialog.DialogStyle.WARNING,
                                        requireContext(),
                                        getString(R.string.error),
                                        getString(R.string.error_saving_training),
                                        getString(R.string.ok));
                            }
                        }
                    }
                }
            }

        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // restore saved data
        if (savedInstanceState != null) {
            long id = savedInstanceState.getLong("trackId", -1);
            if (id > -1) {
                trackId = new Track.Id(id);
            }
            isTrainingRunning = savedInstanceState.getBoolean("isTrainingRunning", false);
            isTrainingPaused = savedInstanceState.getBoolean("isTrainingPaused", false);
            trainingTime = savedInstanceState.getLong("trainingTime", 0);
        }

        sportsLibrary = ((RunningPlanApplication) requireActivity().getApplication()).getSportsLibrary();
        // enable notification for training infos
        notificationManager = new TrainingNotificationManager(requireActivity().getApplicationContext());

        // app preferences
        sharedPreferences = requireContext().getSharedPreferences(
                getString(R.string.preference_file), Context.MODE_PRIVATE);
        // set the flag - can we send notifications?
        SharedPreferences sharedPreferences =
                requireContext().getSharedPreferences(requireContext().getString(R.string.preference_file), Context.MODE_PRIVATE);
        useNotifications = sharedPreferences.getBoolean(Global.UserPreferencesKeys.USE_NOTIFICATIONS, true);

        // checking location permissions
        useLocationData = sharedPreferences.getBoolean(Global.UserPreferencesKeys.USE_LOCATIONS, false);
        locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts
                            .RequestMultiplePermissions(), result -> {
                        Boolean fineLocationGranted = result.getOrDefault(
                                Manifest.permission.ACCESS_FINE_LOCATION, false);
                // Precise location access granted.
                // No location access granted.
                locationServicesAvailable = fineLocationGranted != null && fineLocationGranted;
                    }
            );

        // register receiver for timer
        requireActivity().registerReceiver(backgroundTimeReceiver,
                new IntentFilter(TrainingServiceCallback.SERVICE_RECEIVER_ACTION));
        // register receiver for track updates
        // with filters for different actions
        IntentFilter locationUpdateAction = new IntentFilter(TrackManager.LOCATION_UPDATE_ACTION);
        IntentFilter completedAction = new IntentFilter(TrackManager.TRACK_COMPLETED_ACTION);
        requireActivity().registerReceiver(trackManagerReceiver,locationUpdateAction);
        requireActivity().registerReceiver(trackManagerReceiver,completedAction);

        // check permissions for locations services
        checkLocationPermissions();
        // check gps
        checkGPSStatus();

        // load the training data
        viewModel = new RunningPlanViewModel(requireActivity().getApplication(), null);
        appUser = viewModel.getAppUser();
        UUID runningPlanUUID = appUser.getActiveRunningPlanUUID();
        if (runningPlanUUID != null) {
            runningPlan = viewModel.getRunningPlanByUUID(runningPlanUUID);
            isTrainingPossible = runningPlan != null;
        }

        // set training data
        setTrainingData();

        // initialize background timer service
        // if training is possible
        handleBackgroundTimerService();
    }

    public View onCreateView(@NotNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View trainingView = inflater.inflate(R.layout.fragment_training, container, false);
        // initialize ui elements
        setViewElements(trainingView);
        return trainingView;
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle savedInstanceState) {
        // add the track id
        if (trackId != null) {
            savedInstanceState.putLong("trackId", trackId.getId());
        }
        // add training state
        savedInstanceState.putBoolean("isTrainingRunning", isTrainingRunning);
        savedInstanceState.putBoolean("isTrainingPaused", isTrainingPaused);
        savedInstanceState.putLong("trainingTime", trainingTime);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        // restore saved data
        if (savedInstanceState != null) {
            long id = savedInstanceState.getLong("trackId", -1);
            if (id > -1) {
                trackId = new Track.Id(id);
            }
            isTrainingRunning = savedInstanceState.getBoolean("isTrainingRunning", false);
            isTrainingPaused = savedInstanceState.getBoolean("isTrainingPaused", false);
            trainingTime = savedInstanceState.getLong("trainingTime", 0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // location permissions can be changed by user
        checkLocationPermissions();
        // check gps
        checkGPSStatus();

        if (isTrainingRunning) {
            // view running image
            trainingInfoImageView.setImageResource(R.drawable.baseline_directions_run_black_24);
        } else {
            // view infos from last training
            showLastTraining();
        }
    }

    @Override
    public void onDestroy() {
        notificationManager = null;
        requireActivity().unregisterReceiver(backgroundTimeReceiver);
        requireActivity().unregisterReceiver(trackManagerReceiver);
        if (!trainingServiceConnection.isTrainingActive()) {
            // unbind service connections only if no training active
            trainingServiceConnection.stopAndUnbindService(requireActivity().getApplicationContext());
        }
        super.onDestroy();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() == R.id.fgmt_training_day_spinner) {
            // user select another training day (running plan entry)
            // change the selected item in training unit spinner
            if (runningPlan != null) {
                List<RunningPlanEntry> runningPlanEntries = runningPlan.getEntries();
                if (runningPlanEntries.size() > position) {
                    // set the active running plan entry
                    runningPlanEntry = runningPlanEntries.get(position);
                    // update the running units list and the info label
                    showRunningPlanEntryInView();
                }
            }
        } else {
            // user select another training unit
            if (runningPlanEntry != null) {
                List<RunningUnit> runningUnits = runningPlanEntry.getRunningUnits();
                if (runningUnits.size() > position) {
                    // set the selected running unit
                    runningUnit = runningUnits.get(position);
                    // set the new training time
                    unitTimeToRun = runningUnit.getDuration();
                }
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onServiceConnected(ServiceConnection connection) {
        isTrainingServiceConnected = true;
    }

    @Override
    public void onServiceDisconnected(ServiceConnection connection) {
            isTrainingServiceConnected = false;
    }

    @Override
    public void onServiceErrorOccurred(String errorMessage) {
        //TODO: alert to user
        if (sportsLibrary.isDebugMode()) {
            Logger.debug(errorMessage);
        }
    }

    // check for permissions to track location updates
    private void checkLocationPermissions() {
        if (useLocationData && isTrainingPossible) {
            // check for location permissions
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                locationServicesAvailable = true;
            } else {
                locationPermissionRequest.launch(new String[] {
                        Manifest.permission.ACCESS_FINE_LOCATION
                });
            }
        }
        if (sportsLibrary.isDebugMode()) {
            Logger.debug("Location updates are allowed: " + locationServicesAvailable);
        }
    }

    // check the gps status of device
    private void checkGPSStatus() {
        LocationManager manager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE );
        locationServicesAvailable =
                locationServicesAvailable  && manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (sportsLibrary.isDebugMode()) {
            Logger.debug("GPS are enabled: " + locationServicesAvailable);
        }
    }

    // initialize background timer service
    private void handleBackgroundTimerService() {
        // bind and start service on application context
        // on fragment the service is destroyed when switching to another fragment
        if (isTrainingPossible && ! isTrainingServiceConnected) {
            trainingServiceConnection.bindAndStartService(requireActivity().getApplicationContext());
            if (sportsLibrary.isDebugMode()) {
                Logger.debug("Service bind and start.");
            }
        }
    }

    private void setViewElements(@NotNull View trainingView) {
        // timer label starts with 00:00:00
        timerLabel = trainingView.findViewById(R.id.fgmt_training_timer_label);
        updateTimerLabel();
        // training state image view
        // first state info ist paused
        runningPlanStateImageView = trainingView.findViewById(R.id.fgmt_training_plan_state_image_view);
        runningPlanStateImageView.setImageResource(R.drawable.active20x20);
        // training state image view
        // first state info ist paused
        trainingInfoImageView = trainingView.findViewById(R.id.fgmt_training_info_image_view);
        trainingInfoImageView.setImageResource(R.drawable.baseline_self_improvement_black_24);
        // initialize the training days spinner
        // if user select another day, the spinner for unit changed too
        trainingDaysSpinner = trainingView.findViewById(R.id.fgmt_training_day_spinner);
        trainingDaysSpinner.setOnItemSelectedListener(this);
        // creating adapter for spinner with empty list
        trainingDaysSpinnerArrayAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_item,
                new ArrayList<>());
        // attaching data adapter to spinner with empty list
        trainingDaysSpinner.setAdapter(trainingDaysSpinnerArrayAdapter);
        // initialize the training units spinner
        trainingUnitsSpinner = trainingView.findViewById(R.id.fgmt_training_running_unit_spinner);
        trainingUnitsSpinner.setOnItemSelectedListener(this);
        // creating adapter for spinner with empty list
        trainingUnitsSpinnerArrayAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_item,
                new ArrayList<>());
        // attaching data adapter to spinner with empty list
        trainingUnitsSpinner.setAdapter(trainingUnitsSpinnerArrayAdapter);
        // Label für den Zugriff initialisieren
        runningPlanNameLabel = trainingView.findViewById(R.id.fgmt_training_name_edittext);
        trainingInfolabel = trainingView.findViewById(R.id.fgmt_training_infos_edit_text);
        // Button listener
        startButton = trainingView.findViewById(R.id.fgmt_training_start_button);
        startButton.setOnClickListener(view1 -> startButtonClicked());
        stopButton = trainingView.findViewById(R.id.fgmt_training_stop_button);
        stopButton.setOnClickListener(view1 -> stopButtonClicked());
        pauseButton = trainingView.findViewById(R.id.fgmt_training_pause_button);
        pauseButton.setOnClickListener(view -> pauseButtonClicked());
        showRunningPlanInView();
        showRunningPlanEntryInView();
    }

    private void showRunningPlanInView() {
        if (runningPlan != null) {
            // display the plan state
            if (runningPlan.isCompleted()) {
                runningPlanStateImageView.setImageResource(R.drawable.completed20x20);
            }
            // display the training days (running plan entries) of running plan in spinner
            // add data to spinner
            // addAll(java.lang.Object[]), insert, remove, clear, sort(java.util.Comparator))
            // automatically call notifyDataSetChanged.
            trainingDaysSpinnerArrayAdapter.clear();
            trainingDaysSpinnerArrayAdapter.addAll(getTrainingDaysAsStrings());
            // select the training date in spinner
            int index = runningPlan.getEntries().indexOf(runningPlanEntry);
            if (index > -1) {
                trainingDaysSpinner.setSelection(index);
            }
            // display the name of active running plan in view
            runningPlanNameLabel.setText(runningPlan.getName());
            showRunningPlanEntryInView();
        } else {
            // no training possible
            // disable the button
            startButton.setEnabled(false);
            pauseButton.setEnabled(false);
            stopButton.setEnabled(false);
        }
    }

    // shows the duration of the running plan entry
    private void showRunningPlanEntryInView() {
        if (runningPlanEntry != null) {
            // show the training units of the current running plan entry in spinner
            // add data to spinner
            // addAll(java.lang.Object[]), insert, remove, clear, sort(java.util.Comparator))
            // automatically call notifyDataSetChanged.
            trainingUnitsSpinnerArrayAdapter.clear();
            trainingUnitsSpinnerArrayAdapter.addAll(getTrainingUnitsAsStrings());
            // show the complete training time
            String durationString = buildStringForDuration(runningPlanEntry.getDuration());
            trainingInfolabel.setText(durationString);
            // display state by image
            if (runningPlanEntry.isCompleted()) {
                trainingInfoImageView.setImageResource(R.drawable.baseline_done_black_24);
            } else {
                trainingInfoImageView.setImageResource(R.drawable.baseline_self_improvement_black_24);
            }
            // set the first unit as active running unit
            runningUnit = runningPlanEntry.getRunningUnits().get(0);
            if (runningUnit != null) {
                activeRunningUnit();
            }
        }
    }

    //
    // training
    //
    // start or restart the training
    private void startTraining() {
        if (isTrainingServiceConnected) {
            // disable ui elements to avoid changing while training runs
            trainingDaysSpinner.setEnabled(false);
            trainingUnitsSpinner.setEnabled(false);
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            pauseButton.setEnabled(true);
            trainingInfolabel.setText(R.string.training_is_running);
            trainingInfoImageView.setImageResource(R.drawable.baseline_directions_run_black_24);
            // set value for countdown
            unitTimeToRun = runningUnit.getDuration() * 60;
            if (isTrainingRunning) {
                //  resume the training, track id can be null
                trainingInfolabel.setText(R.string.continue_training);
                trainingServiceConnection.resumeTraining(unitTimeToRun, trackId);
            } else {
                // start a new training
                trainingInfolabel.setText(R.string.start_training);
                trackId = trainingServiceConnection.startTraining(unitTimeToRun, locationServicesAvailable);
                isTrainingRunning = true;
            }
        }
    }

    private void pauseTraining() {
        if (isTrainingServiceConnected) {
            if (isTrainingRunning) {
                trainingServiceConnection.pauseTraining();
                pauseButton.setEnabled(false);
                stopButton.setEnabled(false);
                startButton.setEnabled(true);
                trainingInfolabel.setText(R.string.pause_training);
                trainingInfoImageView.setImageResource(R.drawable.baseline_self_improvement_black_24);
                isTrainingPaused = true;
            }
        }
    }

    private void cancelTraining() {
        if (isTrainingServiceConnected) {
            if (isTrainingRunning) {
                // ask user
                ModalOptionDialog.showYesNoDialog(
                        requireContext(),
                        getString(R.string.question), getString(R.string.ask_for_cancel_training),
                        getString(R.string.ok), getString(R.string.cancel),
                        button -> {
                            if (button == ModalOptionDialog.Button.OK) {
                                // cancel training
                                trainingServiceConnection.cancelTraining();
                                trainingInfolabel.setText(R.string.cancel_training);
                                trainingInfoImageView.setImageResource(R.drawable.baseline_self_improvement_black_24);
                                unitTimeToRun = 0L;
                                trainingTime = 0L;
                                updateTimerLabel();
                                isTrainingRunning = false;
                                isTrainingPaused = false;
                                // enable spinner and training date button
                                startButton.setEnabled(true);
                                stopButton.setEnabled(false);
                                pauseButton.setEnabled(false);
                                trainingDaysSpinner.setEnabled(true);
                                trainingUnitsSpinner.setEnabled(true);
                            }
                        });
            }
        }
    }

    private void monitoringTraining() {
        if (isTrainingServiceConnected) {
            if (unitTimeToRun == 0) {
                // paused the training, maybe there is another one unit
                pauseTraining();
                // unit completed, add the state
                runningUnit.setCompleted(true);
                runningPlan.completeUnit(runningUnit);
                if (!viewModel.updateObject(runningPlan)) {
                    // TODO: alert to user
                    if (sportsLibrary.isDebugMode()) {
                        Logger.debug("A running unit couldn't set as completed.");
                    }
                }
                // more units of the training section available
                // than continue the training
                // TODO: next unit
                if (nextUnit()) {
                    // send notification
                    showNotificationForNextUnit();
                    trainingInfolabel.setText(R.string.new_training_unit_starts);
                    // resume training with another unit
                    startTraining();
                } else {
                    // all units of the entry (day) completed
                    completeTraining();
                }
            }
        }
    }

    //  all units for entry are completed
    @SuppressLint("DefaultLocale")
    private void completeTraining() {
        // training entry (day) unit completed
        if (isTrainingServiceConnected) {
            // stop training
            trainingServiceConnection.endTraining();
            // update values / flags
            isTrainingRunning = false;
            unitTimeToRun = 0L;
            trainingTime = 0L;
            // refresh timer label
            updateTimerLabel();
            // display infos
            if (runningPlan.isCompleted()) {
                notificationManager.sendNotification(getString(R.string.running_plan_completed));
                trainingInfolabel.setText(R.string.running_plan_completed);
                runningPlanStateImageView.setImageResource(R.drawable.completed20x20);
            } else {
                notificationManager.sendNotification(getString(R.string.training_completed));
                trainingInfolabel.setText(R.string.training_completed);
            }
            trainingInfoImageView.setImageResource(R.drawable.baseline_done_black_24);

            // elements can be operated again
            trainingDaysSpinner.setEnabled(true);
            trainingUnitsSpinner.setEnabled(true);
            startButton.setEnabled(true);
            pauseButton.setEnabled(false);
            stopButton.setEnabled(false);
        }
    }

    // add the training
    private boolean saveTraining(@NonNull Track recordedTrack) {
        // save the track of the training
        if (viewModel.addObject(recordedTrack)) {
            // build the training name and remarks
            String name = getString(R.string.training_of_running_plan) + " " +
                    runningPlan.getName();
            LocalDate today = LocalDate.now(ZoneId.systemDefault());
            DateTimeFormatter formatter = DateTimeFormatter.BASIC_ISO_DATE;
            String remarks = getString(R.string.training_at_date)
                    + " "
                    + formatter.format(today);
            // get the uuid of running type
            SportsLibrary sportsLibrary = ((RunningPlanApplication) requireActivity()
                    .getApplication())
                    .getSportsLibrary();
            UUID trainingTypeUUID = sportsLibrary.getUuidForTrainingType(TrainingType.RUNNING); // can be null
            UUID trackUUID = recordedTrack.getUUID();
            Training training = new Training(name, remarks, today,
                    recordedTrack.getDuration(),
                    recordedTrack.getDistance(),
                    recordedTrack.getAverageSpeed(),
                    recordedTrack.getAltitudeDifference(),
                    trainingTypeUUID, trackUUID);
            return viewModel.addObject(training);
        }
        return false;
    }

    private void startButtonClicked() {
        startTraining();
    }

    private void pauseButtonClicked() {
        pauseTraining();
    }

    private void stopButtonClicked() {
        cancelTraining();
    }

    // set the training data in the view
    // is no plan available - no training is possible
    private void setTrainingData() {
        if (!isTrainingPossible) {
            // info to user
            ModalOptionDialog.showMessageDialog(ModalOptionDialog.DialogStyle.WARNING,
                    requireContext(),
                    getString(R.string.information),
                    getString(R.string.no_active_running_plan),
                    getString(R.string.ok));
            return;
        }
        // check if the users running plan completed
        if (runningPlan.isCompleted()) {
            // try to set the next plan in order
            // otherwise ask user how we proceed further
            handleRunningPlan();
            return;
        }
        // set the next uncompleted training day
        List<RunningPlanEntry> entries = runningPlan.getEntries();
        Optional<RunningPlanEntry> entry = entries
                .stream()
                .filter(r -> !r.isCompleted())
                .findFirst();
        entry.ifPresent(planEntry -> runningPlanEntry = planEntry);
        if (runningPlanEntry != null) {
            List<RunningUnit> units = runningPlanEntry.getRunningUnits();
            // TODO: Liste sortiert?
            Optional<RunningUnit> unit = units
                    .stream()
                    .filter(runningUnit -> !runningUnit.isCompleted())
                    .findFirst();
            unit.ifPresent(value -> runningUnit = value);
        }
    }

    // set the next unit of current running plan entry
    private boolean nextUnit() {
        // welche Trainingseinheit wurde vom Nutzer ausgewählt?
        //  nächsten Trainingsabschnitt setzen
        //  nächsten Trainingsabschnitt in spinner auswählen
        if (runningPlanEntry != null) {
            List<RunningUnit> units = runningPlanEntry.getRunningUnits();
            Optional<RunningUnit> unit = units
                    .stream()
                    .filter(runningUnit -> !runningUnit.isCompleted())
                    .findFirst();
            if (unit.isPresent()) {
                runningUnit = unit.get();
                activeRunningUnit();
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    // set the values if a running unit selected
    // called by nextUnit
    private void activeRunningUnit() {
        List<RunningUnit> units = runningPlanEntry.getRunningUnits();
        // select the unit in spinner
        int index = units.indexOf(runningUnit);
        if (index > -1) {
            trainingUnitsSpinner.setSelection(index);
            // set the training time
            unitTimeToRun = runningUnit.getDuration();
            // update training info label
            // show the training time for the unit
            if (isTrainingRunning) {
                String durationString = getString(R.string.unit_time) + ": " + buildStringForDuration(unitTimeToRun);
                trainingInfolabel.setText(durationString);
            }
        }
    }

    // ask user to reset the plan (repeat),
    // select next plan (by order number) or
    // select plan in list
    private void handleRunningPlan() {
        // active running plan is completed
        // try to set the next plan in order
        // otherwise ask user for action
        List<RunningPlan> runningPlans = viewModel.getRunningPlans();
        // get the next uncompleted plan
        Optional<RunningPlan> optionalRunningPlan = runningPlans
                .stream()
                .filter(runningPlan1 -> !runningPlan1.isCompleted())
                .findFirst();
        if (optionalRunningPlan.isPresent()) {
            // set the next plan as active
            runningPlan = optionalRunningPlan.get();
            appUser.setActiveRunningPlanUUID(runningPlan.getUUID());
            if (viewModel.updateObject(appUser)) {
                isTrainingPossible = true;
                setTrainingData();
            } else {
                isTrainingPossible = false;
            }
        } else {
            isTrainingPossible = false;
        }
        if (!isTrainingPossible) {
            ModalOptionDialog.showOptionsDialog(
                    requireContext(),
                    getString(R.string.running_plan_completed),
                    getString(R.string.title_select_new_running_plan_options),
                    getString(R.string.select_running_plan_option_1),
                    getString(R.string.select_running_plan_option_2),
                    button -> {
                        // reset the active running plan
                        if (button == ModalOptionDialog.Button.OPTION_1) {
                            // reset
                            resetRunningPlan();
                        }
                        // select the next running plan by order number
                        if (button == ModalOptionDialog.Button.OPTION_2) {
                            // select from list, open list fragment with running plans
                            FragmentTransaction fragmentTransaction = getParentFragmentManager().beginTransaction();
                            Fragment fragment = new RunningPlansFragment();
                            fragmentTransaction.replace(R.id.fragment_training_container, fragment);
                            fragmentTransaction.addToBackStack(null);
                            fragmentTransaction.commit();
                        }
                    });
        }
    }

    // Reset the running plan. All units are reset as uncompleted.
    private void resetRunningPlan() {
        if (runningPlan != null) {
            runningPlan.setUncompleted();
            if (viewModel.updateObject(runningPlan)) {
                // refresh ui
                setTrainingData();
            } else {
                // TODO: info to the user
                if (sportsLibrary.isDebugMode()) {
                    Logger.debug("Could not reset running plan.");
                }
            }
        }
    }

    @NotNull
    private String buildStringForDuration(long duration) {
        // show the complete training time
        StringBuilder durationString = new StringBuilder(getString(R.string.total_time)+ " ");
        // display in hour or minutes?
        if (duration < 60) {
            durationString.append(duration);
        } else {
            //  convert to hours and minutes
            long hours = duration / 60;
            long minutes = duration % 60;
            if (minutes > 0) {
                durationString.append(hours);
                durationString.append(" h : ");
                durationString.append(minutes);
                durationString.append(" min");
            } else {
                durationString.append(hours);
                durationString.append(" h");
            }
        }
        return durationString.toString();
    }

    // list of training days from selected running plan as string
    @NotNull
    private List<String> getTrainingDaysAsStrings() {
        List<String> trainingDaysStringList = new ArrayList<>();
        if (runningPlan != null) {
            // monday, day 1 and week 1
            LocalDate startDate = runningPlan.getStartDate();
            Iterator<RunningPlanEntry> iterator= runningPlan
                    .getEntries()
                    .stream()
                    .iterator();
            while (iterator.hasNext()) {
                RunningPlanEntry entry = iterator.next();
                int day = entry.getDay();
                int week = entry.getWeek();
                LocalDate trainingDate = startDate.plusDays(day - 1).plusWeeks(week - 1);
                String trainingDateAsString = trainingDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault())
                        + " ("
                        + trainingDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                        + ")";
                trainingDaysStringList.add(trainingDateAsString);
            }
        }
        return trainingDaysStringList;
    }

    // list of training unit from selected running plan entry as string
    @NotNull
    private List<String> getTrainingUnitsAsStrings() {
        List<String> trainingUnitsStringList = new ArrayList<>();
        if (runningPlanEntry != null) {
            for (RunningUnit runningUnit : runningPlanEntry.getRunningUnits()) {
                // TODO: format spinner
                StringBuilder unitsAsString = new StringBuilder();
                MovementType movementType = runningUnit.getMovementType();
                if (movementType != null) {
                    // the name of movement type
                    // localizations from sports library
                    unitsAsString.append(movementType.getName());
                    // running unit duration
                    unitsAsString.append(" (");
                    unitsAsString.append(runningUnit.getDuration());
                    unitsAsString.append(" min)");

                } else {
                    unitsAsString.append(R.string.movement_type_not_found);
                }
                // add the string value for the movement type to the list
                trainingUnitsStringList.add(unitsAsString.toString());
            }
        }
        return trainingUnitsStringList;
    }

    // format the time and refresh the label
    private void showTrackInfos(double distance) {
        if (isTrainingRunning) {
            double averageSpeed = 0.0d;
            if (distance > 0.0) {
                // duration from unit in minutes
                long duration = runningUnit.getDuration() * 60 - unitTimeToRun;
                trainingTime += duration;
                averageSpeed = distance / trainingTime;
            }
            StringBuilder builder = new StringBuilder(getString(R.string.training_is_running));
            builder.append("\n");
            if (distance < 999) {
                builder.append(String.format(Locale.getDefault(), "%,.2f%s%,.2f%s", averageSpeed * 3.6," km/h, ", distance, " m"));
            } else {
                // value in km
                distance = distance / 1000;
                builder.append(String.format(Locale.getDefault(), "%,.2f%s%,.2f%s", averageSpeed * 3.6," km/h, ", distance, " km"));
            }

            trainingInfolabel.setText(builder);
        }
    }

    // format the time and refresh the label
    private void updateTimerLabel() {
        // duration in seconds
        // 00:00:00 (hh:mm:ss)
        long hh= unitTimeToRun / 3600;
        long mm = (unitTimeToRun % 3600) / 60;
        long ss = unitTimeToRun % 60;
        timerLabel.setText(String.format(Locale.ENGLISH, "%02d:%02d:%02d", hh, mm, ss));
    }

    // send notification for next unit
    private void showNotificationForNextUnit() {
        if (useNotifications) {
            // build the message
            String movementTypeName = runningUnit.getMovementType().getName();
            long duration = 0;
            String notificationMessage = getString(R.string.new_training_unit_starts)
                    + ": "
                    + movementTypeName + " (" + duration + " min)";
            notificationManager.sendNotification(notificationMessage);
            Vibrator vibrator;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                VibratorManager vibratorManager =
                        (VibratorManager) requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE);

                vibrator = vibratorManager.getDefaultVibrator();

            } else {
                // backward compatibility for Android API < 31,
                // VibratorManager was only added on API level 31 release.
                // noinspection deprecation
                vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);

            }

            final int DELAY = 0, VIBRATE = 2000, SLEEP = 1000, START = -1;
            long[] vibratePattern = {DELAY, VIBRATE, SLEEP};

            vibrator.vibrate(VibrationEffect.createWaveform(vibratePattern, START));

        }
    }

    @SuppressLint("DefaultLocale")
    private void showLastTraining() {
        if (trackId != null && isTrainingServiceConnected) {
            // get the recorded data
            Track recordedTrack = trainingServiceConnection.getRecordedTrack(trackId);
            if (recordedTrack != null) {
                // show training infos
                StringBuilder trainingTrackInfoString = new StringBuilder(getString(R.string.training_completed) + "\n");
                trainingTrackInfoString.append(getString(R.string.distance));
                trainingTrackInfoString.append(": ");
                double runningDistance = recordedTrack.getDistance();
                if (runningDistance < 999.99) {
                    //  distance in m
                    trainingTrackInfoString.append(String.format("%,.2f%s",runningDistance, " m"));
                } else {
                    //  distance in km
                    trainingTrackInfoString.append(String.format("%,.2f%s",runningDistance / 1000, " km"));
                }
                trainingTrackInfoString.append("\n");
                //  average speed in km/h
                double averageSpeed = recordedTrack.getAverageSpeed();
                trainingTrackInfoString.append(getString(R.string.speed));
                trainingTrackInfoString.append(": ");
                trainingTrackInfoString.append(String.format("%,.2f%s",averageSpeed, " km/h"));
                trainingInfolabel.setText(trainingTrackInfoString);
            }
        }
    }
}