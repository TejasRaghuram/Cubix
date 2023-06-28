package com.example.cubix;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LayoutActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    FirebaseUser user;
    TabLayout screenSelector;
    ViewPager2 view;
    ViewPagerAdapter viewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout);

        getSupportActionBar().hide();

        mAuth = FirebaseAuth.getInstance();

        user = mAuth.getCurrentUser();

        screenSelector = findViewById(R.id.layout_tabs);
        view = findViewById(R.id.layout_view);
        viewAdapter = new ViewPagerAdapter(this);
        view.setAdapter(viewAdapter);

        screenSelector.getTabAt(2).getIcon().setColorFilter(Color.parseColor("#BC0E0E"), PorterDuff.Mode.SRC_IN);

        screenSelector.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if(tab.getPosition() == 2)
                {
                    mAuth.signOut();
                    Intent i = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(i);
                    finish();
                }
                view.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        view.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                screenSelector.getTabAt(position).select();
            }
        });

    }
}

class ViewPagerAdapter extends FragmentStateAdapter {
    HomeFragment home;
    TimerFragment timer;
    CompetitionFragment competition;
    SolverFragment solver;

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);

        home = new HomeFragment();
        timer = new TimerFragment();
        competition = new CompetitionFragment();
        solver = new SolverFragment();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if(position == 0)
        {
            return home;
        }
        else if(position == 1)
        {
            return timer;
        }
        /*else if(position == 2)
        {
            return competition;
        }
        else if(position == 3)
        {
            return solver;
        }*/
        return null;
    }

    @Override
    public int getItemCount() {
        return 2;
    }

}
