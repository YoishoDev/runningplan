package de.hirola.runningplan.ui.info;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.hirola.runningplan.R;
import de.hirola.runningplan.ui.runningplans.RunningPlanDetailsFragment;
import de.hirola.runningplan.util.AppLogManager;
import de.hirola.sportslibrary.model.RunningPlan;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InfoFragment extends Fragment implements View.OnClickListener {

    private final static String TAG = InfoFragment.class.getSimpleName();

    private AppLogManager logManager; // app logger
    // recycler view list adapter
    private RecyclerView recyclerView;
    // menu items
    private Map<Integer, MenuItem> menuItemMap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // app logger
        logManager = AppLogManager.getInstance(requireContext());
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View infoView = inflater.inflate(R.layout.fragment_info, container, false);
        // build the menu item map
        menuItemMap = new HashMap<>(3);
        menuItemMap.putIfAbsent(0, new MenuItem(R.drawable.baseline_info_black_36, R.string.menu_item_info, new AboutFragment()));
        menuItemMap.putIfAbsent(1, new MenuItem(R.drawable.baseline_list_black_36, R.string.menu_item_license, new LicenseFragment()));
        menuItemMap.putIfAbsent(2, new MenuItem(R.drawable.baseline_help_black_36, R.string.menu_item_help, new Fragment()));
        InfoMenuItemRecyclerView listAdapter = new InfoMenuItemRecyclerView(requireContext(), menuItemMap);
        // menu fragments on click
        listAdapter.setOnClickListener(this);
        recyclerView = infoView.findViewById(R.id.recyclerView_info_menu_items);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(listAdapter);
        return infoView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onClick(View v) {
        int itemPosition = recyclerView.getChildLayoutPosition(v);
        showFragmentForMenuItemAtIndex(itemPosition);
    }

    private void showFragmentForMenuItemAtIndex(int index) {
        if (menuItemMap.size() > index) {
            MenuItem menuItem = menuItemMap.get(index);
            RunningPlanDetailsFragment detailsFragment = null;
            List<Fragment> fragments = getParentFragmentManager().getFragments();
            for (Fragment fragment : fragments) {
                if (fragment instanceof RunningPlanDetailsFragment) {
                    detailsFragment = (RunningPlanDetailsFragment) fragment;
                    break;
                }
            }
            if (detailsFragment == null || detailsFragment.getUUID().equalsIgnoreCase(uuid)) {
                // create a new fragment
                detailsFragment = RunningPlanDetailsFragment.newInstance(uuid);
            }
            // starts the RunningPlanDetailsFragment
            showFragment(detailsFragment);
        }
    }

    private void showFragment(Fragment fragment) {
        // hides the info fragment
        FragmentTransaction fragmentTransaction = getParentFragmentManager().beginTransaction();
        fragmentTransaction.hide(this);
        // starts the menu item fragment
        fragmentTransaction.replace(R.id.fragment_running_plan_container, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
}