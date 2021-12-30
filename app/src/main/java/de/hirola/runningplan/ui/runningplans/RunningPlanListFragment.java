package de.hirola.runningplan.ui.runningplans;

import android.content.Intent;
import android.widget.ListView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import de.hirola.runningplan.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.ListFragment;
import de.hirola.runningplan.model.MutableListLiveData;
import de.hirola.runningplan.model.RunningPlanViewModel;
import de.hirola.sportslibrary.model.RunningPlan;

import java.util.List;

/**
 * Copyright 2021 by Michael Schmidt, Hirola Consulting
 * This software us licensed under the AGPL-3.0 or later.
 *
 * List-Fragment für die Darstellung aller vorhandener Laufpläne (Überblick).
 *

 * @author Michael Schmidt (Hirola)
 * @since 1.1.1
 */
public class RunningPlanListFragment extends ListFragment {

    private ActivityResultLauncher<Intent> activityResultLauncher;
    private RunningPlanViewModel viewModel;
    // cached list of running plans
    private List<RunningPlan> runningPlans;
    private RunningPlanArrayAdapter listAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // load the running plans
        viewModel = new ViewModelProvider(this).get(RunningPlanViewModel.class);
        MutableListLiveData<RunningPlan> mutableRunningPlans = viewModel.getMutableRunningPlans();
        runningPlans = mutableRunningPlans.getValue();
        // visualize th list of running plans
        listAdapter = new RunningPlanArrayAdapter(requireContext(),runningPlans);
        setListAdapter(listAdapter);

        // handle the return from details activity
        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // update the cached list of running plans
                    runningPlans = viewModel.getRunningPlanes();
                    // update the list ui
                    listAdapter.submitList(runningPlans);
                });

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_running_plan_list, container, false);
    }

    @Override
    public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
        showDetailsForRunningPlanAtIndex(position);
    }

    private void showDetailsForRunningPlanAtIndex(int index) {
        if (runningPlans.size() > index) {
            RunningPlan runningPlan = runningPlans.get(index);
            Intent intent = new Intent(requireContext(), RunningPlanDetailsActivity.class);
            // put the uuid for the selected running plan to the details activity
            intent.putExtra("uuid", runningPlan.getUUID());
            // starts the RunningPlanDetailsActivity
            activityResultLauncher.launch(intent);
        }
    }

    // refresh the cached list of running plans
    private void onListChanged(List<RunningPlan> changedList) {
        runningPlans = changedList;
    }
}