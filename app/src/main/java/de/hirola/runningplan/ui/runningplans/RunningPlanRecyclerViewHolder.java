package de.hirola.runningplan.ui.runningplans;

import android.content.res.Resources;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import de.hirola.runningplan.R;
import de.hirola.sportslibrary.Global;
import de.hirola.sportslibrary.model.RunningPlan;
import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;

/**
 * Copyright 2021 by Michael Schmidt, Hirola Consulting
 * This software us licensed under the AGPL-3.0 or later.
 *
 * Custom Adapter for RunningPlans to view in list.
 *
 * @author Michael Schmidt (Hirola)
 * @since 1.1.1
 */
public class RunningPlanRecyclerViewHolder extends RecyclerView.ViewHolder {

    TextView nameTextView;
    TextView remarksTextView;
    TextView startDateTextView;
    TextView durationTextView;
    TextView percentCompletedTextView;
    ImageView statusImageView;

    public RunningPlanRecyclerViewHolder(@NonNull @NotNull View itemView) {
        super(itemView);
        statusImageView = itemView.findViewById(R.id.runningplan_row_state_imageview);
        nameTextView = itemView.findViewById(R.id.runningplan_row_name_textview);
        remarksTextView = itemView.findViewById(R.id.runningplan_row_remarks_textview);
        startDateTextView = itemView.findViewById(R.id.runningplan_row_startdate_textview);
        durationTextView = itemView.findViewById(R.id.runningplan_row_duration_textView);
        percentCompletedTextView = itemView.findViewById(R.id.runningplan_row_percentcompleted_textview);
    }
}