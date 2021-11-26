package de.hirola.runningplan.ui.runningplans;

import android.os.Bundle;
import androidx.fragment.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import de.hirola.runningplan.R;
import de.hirola.sportslibrary.model.RunningPlan;

import java.time.LocalDate;

/**
 * A fragment representing a list of Items.
 */
public class RunningPlanListFragment extends ListFragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public RunningPlanListFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static RunningPlanListFragment newInstance(int columnCount) {
        RunningPlanListFragment fragment = new RunningPlanListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // sample data
        RunningPlan[] list = new RunningPlan[2];
        RunningPlan r1 = new RunningPlan();
        r1.setName("Laufplan 1");
        r1.setRemarks("Erster Laufplan");
        r1.setStartDate(LocalDate.now());
        RunningPlan r2 = new RunningPlan();
        r2.setName("Laufplan 2");
        r2.setRemarks("Zweiter Laufplan");

        list[0] = r1;
        list[1] = r2;

        // adapter
        RunningPlanArrayAdapter runningPlanArrayAdapter = new RunningPlanArrayAdapter(getContext(), list);

        setListAdapter(runningPlanArrayAdapter);

        return inflater.inflate(R.layout.fragment_running_plans, container, false);

    }


}