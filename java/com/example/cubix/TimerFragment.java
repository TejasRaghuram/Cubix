package com.example.cubix;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class TimerFragment extends Fragment {

    private FirebaseAuth mAuth;
    FirebaseUser user;
    private FirebaseDatabase firebaseDatabase;
    DatabaseReference sessionRef;

    TextView time;
    TextView scramble;
    TextView average;
    TextView single;
    TextView ao5;
    TextView ao12;
    Spinner session;
    ListView timeList;
    ArrayList<Double> currentTimes;
    TimeAdapter timeAdapter;
    boolean timing;
    Timer timer;
    int timeCount;
    String[] moves = {
            "U", "U'", "U2",
            "R", "R'", "R2",
            "L", "L'", "L2",
            "D", "D'", "D2",
            "F", "F'", "F2",
            "B", "B'", "B2",
    };
    ArrayList<String> spinnerSessions;
    ArrayAdapter<String> sessionAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_timer, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        sessionRef = firebaseDatabase.getReference().child("Users").child(user.getUid()).child("Sessions");

        View view = getView();
        if(view != null)
        {
            time = view.findViewById(R.id.timer_time);
            scramble = view.findViewById(R.id.timer_scramble);
            session = view.findViewById(R.id.timer_session);
            timeList = view.findViewById(R.id.timer_timelist);
            average = view.findViewById(R.id.timer_average);
            single = view.findViewById(R.id.timer_single);
            ao5 = view.findViewById(R.id.timer_ao5);
            ao12 = view.findViewById(R.id.timer_ao12);

            currentTimes = new ArrayList<>();
            timeAdapter = new TimeAdapter(getActivity().getApplicationContext(), R.layout.timer_time, currentTimes);
            timeList.setAdapter(timeAdapter);
            spinnerSessions = new ArrayList<>();
            sessionAdapter = new ArrayAdapter<String>(getActivity().getApplicationContext(), R.layout.timer_sessionlayout, spinnerSessions);
            sessionAdapter.setDropDownViewResource(R.layout.timer_sessionopenlayout);
            session.setAdapter(sessionAdapter);
            sessionRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if(snapshot.getChildrenCount() + 1 != spinnerSessions.size())
                    {
                        spinnerSessions.clear();
                        for(int i = 0; i < snapshot.getChildrenCount() + 1; i++)
                        {
                            spinnerSessions.add("Session " + (i + 1));
                            sessionAdapter.notifyDataSetChanged();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
            session.setSelection(0);

            sessionRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    //currentTimes.clear();
                    /*if(snapshot.getKey().equals(session.getSelectedItem().toString()))
                    {
                        for(DataSnapshot dataSnapshot : snapshot.getChildren())
                        {
                            currentTimes.add((double)(dataSnapshot.getValue()));
                        }
                        timeAdapter.notifyDataSetChanged();
                    }*/
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

            session.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    sessionRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            currentTimes.clear();
                            for(DataSnapshot dataSnapshot : snapshot.getChildren())
                            {
                                if(dataSnapshot.getKey().equals(session.getSelectedItem().toString()))
                                {
                                    for(DataSnapshot ds : dataSnapshot.getChildren())
                                    {
                                        currentTimes.add((double)(ds.getValue()));
                                    }
                                    timeAdapter.notifyDataSetChanged();
                                    double avg = 0;
                                    double min = Integer.MAX_VALUE;
                                    for(int i = 0; i < currentTimes.size(); i++)
                                    {
                                        avg += currentTimes.get(i);
                                        if(currentTimes.get(i) < min)
                                        {
                                            min = currentTimes.get(i);
                                        }
                                    }
                                    avg /= (currentTimes.size());
                                    avg *= 100;
                                    avg = (int)(avg);
                                    avg /= 100;
                                    average.setText("Average: " + getTime(avg));
                                    single.setText("Best Single: " + getTime(min));
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            scramble.setText(generateScramble());
            timer = new Timer();
            timing = false;
            timeCount = 0;
            time.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(event.getAction() == MotionEvent.ACTION_DOWN)
                    {
                        time.setTextColor(Color.parseColor("#15945b"));
                        time.setText("0:00.00");
                    }
                    else if(event.getAction() == MotionEvent.ACTION_UP)
                    {
                        time.setTextColor(Color.BLACK);
                        if(timing) {
                            timing = false;
                            timeCount = 0;
                            timer.cancel();
                            timer = new Timer();
                            scramble.setText(generateScramble());

                            sessionRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    boolean found = false;
                                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                        System.out.println(dataSnapshot.getKey());
                                        System.out.println(session.getSelectedItem().toString());
                                        if (dataSnapshot.getKey().equals(session.getSelectedItem().toString())) {
                                            found = true;
                                            HashMap<String, Double> times = new HashMap<>();
                                            int i = 0;
                                            for (DataSnapshot t : dataSnapshot.getChildren()) {
                                                times.put(String.valueOf(i), (double)(t.getValue()));
                                                i++;
                                            }
                                            double current = Double.parseDouble(time.getText().toString().substring(2, 4));
                                            current += Double.parseDouble(time.getText().toString().substring(0, 1)) * 60;
                                            current += Double.parseDouble(time.getText().toString().substring(5, 7)) / 100;
                                            times.put(String.valueOf(i), current);
                                            sessionRef.child(session.getSelectedItem().toString())
                                                    .setValue(times);
                                        }
                                    }
                                    if(!found)
                                    {
                                        HashMap<String, Double> times = new HashMap<>();
                                        double current = Double.parseDouble(time.getText().toString().substring(2, 4));
                                        current += Double.parseDouble(time.getText().toString().substring(0, 1)) * 60;
                                        current += Double.parseDouble(time.getText().toString().substring(5, 7)) / 100;
                                        times.put("0", current);
                                        sessionRef.child(session.getSelectedItem().toString())
                                                .setValue(times);
                                    }
                                    currentTimes.clear();
                                    for(DataSnapshot dataSnapshot : snapshot.getChildren())
                                    {
                                        if(dataSnapshot.getKey().equals(session.getSelectedItem().toString()))
                                        {
                                            for(DataSnapshot ds : dataSnapshot.getChildren())
                                            {
                                                currentTimes.add((double)(ds.getValue()));
                                            }
                                            System.out.println(currentTimes.size());
                                            timeAdapter.notifyDataSetChanged();
                                        }
                                    }
                                    double avg = 0;
                                    double min = Integer.MAX_VALUE;
                                    for(int i = 0; i < currentTimes.size(); i++)
                                    {
                                        avg += currentTimes.get(i);
                                        if(currentTimes.get(i) < min)
                                        {
                                            min = currentTimes.get(i);
                                        }
                                    }
                                    double current = Double.parseDouble(time.getText().toString().substring(2, 4));
                                    current += Double.parseDouble(time.getText().toString().substring(0, 1)) * 60;
                                    current += Double.parseDouble(time.getText().toString().substring(5, 7)) / 100;
                                    avg += current;
                                    avg /= (currentTimes.size() + 1);
                                    avg *= 100;
                                    avg = (int)(avg);
                                    avg /= 100;
                                    if(current < min)
                                    {
                                        min = current;
                                    }
                                    average.setText("Average: " + getTime(avg));
                                    single.setText("Best Single: " + getTime(min));
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });

                        }
                        else
                        {
                            timing = true;
                            timer.scheduleAtFixedRate(new TimerTask() {
                                @Override
                                public void run() {
                                    timeCount++;
                                    int temp = timeCount;
                                    int cs = temp % 100;
                                    String csStr = String.valueOf(cs);
                                    if(cs < 10)
                                    {
                                        csStr = "0" + csStr;
                                    }
                                    temp -= cs;
                                    temp /= 100;
                                    int s = temp % 60;
                                    String sStr = String.valueOf(s);
                                    if(s < 10)
                                    {
                                        sStr = "0" + sStr;
                                    }
                                    temp -= s;
                                    int min = temp / 60;
                                    String minStr = String.valueOf(min);
                                    time.setText(minStr + ":" + sStr + "." + csStr);
                                    if(min >= 5)
                                    {
                                        timing = false;
                                        timeCount = 0;
                                        time.setText("0:00.00");
                                        timer.cancel();
                                    }
                                }
                            }, 0, 10);
                        }
                    }
                    return true;
                }
            });
        }
    }

    public String generateScramble()
    {
        String[] scrambleMoves = new String[10];
        String result = "";
        for(int i = 0; i < 10; i++)
        {
            do {
                scrambleMoves[i] = moves[(int)(Math.random() * moves.length)];
            }while(i != 0 && scrambleMoves[i].charAt(0) == scrambleMoves[i - 1].charAt(0));
            result += scrambleMoves[i];
            if(i != 9)
            {
                result += " ";
            }
        }
        return result;
    }

    public String getTime(double timeDouble)
    {
        timeDouble *= 100;
        int timeInt = (int)(timeDouble);

        String cs = String.valueOf(timeInt % 100);
        if(timeInt % 100 < 10)
        {
            cs = "0" + cs;
        }
        timeInt -= timeInt % 100;
        timeInt /= 100;

        String s = String.valueOf(timeInt % 60);
        if(timeInt % 60 < 10)
        {
            s = "0" + s;
        }
        timeInt -= timeInt % 60;

        String m = String.valueOf(timeInt / 60);

        return m + ":" + s + "." + cs;
    }
}

class TimeAdapter extends ArrayAdapter<Double>
{
    Context activityContext;
    int xml;
    ArrayList<Double> list;
    TextView number;
    TextView time;
    TextView ao5;
    TextView ao12;

    public TimeAdapter(@NonNull Context context, int resource, @NonNull ArrayList<Double> solves)
    {
        super(context, resource, solves);
        activityContext = context;
        xml = resource;
        list = solves;
    }


    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
    {
        View myCustomView;
        LayoutInflater layoutInflater = (LayoutInflater)activityContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        myCustomView = layoutInflater.inflate(xml, null);

        number = myCustomView.findViewById(R.id.time_number);
        time = myCustomView.findViewById(R.id.time_time);
        ao5 = myCustomView.findViewById(R.id.time_ao5);
        ao12 = myCustomView.findViewById(R.id.time_ao12);

        if(list.size() > 0)
        {
            number.setText(String.valueOf(position + 1));

            time.setText(getTime(list.get(position)));

            if(position >= 4)
            {
                ArrayList<Double> avgTemp = new ArrayList<>();
                for(int i = position; i > position - 5; i--)
                {
                    avgTemp.add(list.get(i));
                }
                Collections.sort(avgTemp);
                avgTemp.remove(avgTemp.size() - 1);
                avgTemp.remove(0);

                double avg = 0;
                for(int i = 0; i < 3; i++)
                {
                    avg += avgTemp.get(i);
                }
                avg /= 3;
                avg = (int)(avg * 100);
                avg /= 100;

                ao5.setText(getTime(avg));
            }

            if(position >= 11)
            {
                ArrayList<Double> avgTemp = new ArrayList<>();
                for(int i = position; i > position - 12; i--)
                {
                    avgTemp.add(list.get(i));
                }
                Collections.sort(avgTemp);
                avgTemp.remove(avgTemp.size() - 1);
                avgTemp.remove(0);

                double avg = 0;
                for(int i = 0; i < 10; i++)
                {
                    avg += avgTemp.get(i);
                }
                avg /= 10;
                avg = (int)(avg * 100);
                avg /= 100;

                ao12.setText(getTime(avg));
            }
        }

        return myCustomView;
    }

    public String getTime(double timeDouble)
    {
        timeDouble *= 100;
        int timeInt = (int)(timeDouble);

        String cs = String.valueOf(timeInt % 100);
        if(timeInt % 100 < 10)
        {
            cs = "0" + cs;
        }
        timeInt -= timeInt % 100;
        timeInt /= 100;

        String s = String.valueOf(timeInt % 60);
        if(timeInt % 60 < 10)
        {
            s = "0" + s;
        }
        timeInt -= timeInt % 60;

        String m = String.valueOf(timeInt / 60);

        return m + ":" + s + "." + cs;
    }
}
