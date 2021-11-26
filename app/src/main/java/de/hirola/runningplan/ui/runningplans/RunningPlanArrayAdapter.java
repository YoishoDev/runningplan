package de.hirola.runningplan.ui.runningplans;

import de.hirola.runningplan.R;
import de.hirola.sportslibrary.model.RunningPlan;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/**
 * Custom Adapter for RunningPlans
 */
public class RunningPlanArrayAdapter extends ArrayAdapter<RunningPlan> {
    private final Context context;
    private final RunningPlan[] values;

    public RunningPlanArrayAdapter(Context context, RunningPlan[] values) {
        super(context, R.layout.runningplan_row, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.runningplan_row, parent, false);

        // Laufplan darstellen
        RunningPlan runningPlan = values[position];
        // Name und Status
        // TODO: Status-Bild
        TextView nameTextView = rowView.findViewById(R.id.runningplan_row_name_textview);
        nameTextView.setText(runningPlan.getName());
        // Anmerkungen
        TextView remarksTextView = rowView.findViewById(R.id.runningplan_row_remarks_textview);
        remarksTextView.setText(runningPlan.getRemarks());
        // Startdatum
        TextView startDateTextView = rowView.findViewById(R.id.runningplan_row_startdate_textview);
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
        startDateTextView.setText(formatter.format(runningPlan.getStartDate()));
        // Gesamtdauer des Trainings (komplette Trainingszeit in Minuten!)
        TextView durationTextView = rowView.findViewById(R.id.runningplan_row_duration_textView);
        durationTextView.setText("");
        int duration = runningPlan.getDuration();
        if (duration > 0) {
            //  Darstellung in hh:mm
            //  in h und min umrechnen
            int hours = duration / 60;
            int minutes = duration % 60;
            durationTextView.setText(String.format(Locale.GERMANY,"%d:%d",hours, minutes));
        } else {
            durationTextView.setText("00h:00min");
        }
        // Laufplan abgeschlossen in Prozent
        String percentCompleted = String.valueOf(runningPlan.percentCompleted()).concat(" %");
        TextView percentCompletedTextView = rowView.findViewById(R.id.runningplan_row_percentcompleted_textview);
        percentCompletedTextView.setText(percentCompleted);
        return rowView;
    }
}