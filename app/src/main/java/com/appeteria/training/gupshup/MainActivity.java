package com.appeteria.training.gupshup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.appeteria.training.gupshup.Common.NodeNames;
import com.appeteria.training.gupshup.chats.ChatFragment;
import com.appeteria.training.gupshup.findfriends.FindFriendsFragment;
import com.appeteria.training.gupshup.profile.ProfileActivity;
import com.appeteria.training.gupshup.requests.RequestsFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tabLayout = findViewById(R.id.tabMain);
        viewPager = findViewById(R.id.vpMain);

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();


        DatabaseReference databaseReferenceUsers = FirebaseDatabase.getInstance().getReference()
                .child(NodeNames.USERS).child(firebaseAuth.getCurrentUser().getUid());

        databaseReferenceUsers.child(NodeNames.ONLINE).setValue(true);
        databaseReferenceUsers.child(NodeNames.ONLINE).onDisconnect().setValue(false);

        setViewPager();

    }

    class Adapter extends FragmentPagerAdapter{

        public Adapter(@NonNull FragmentManager fm, int behavior) {
            super(fm, behavior);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            switch (position)
            {
                case 0:
                    ChatFragment chatFragment = new ChatFragment();
                    return  chatFragment;
                case 1:
                    RequestsFragment requestsFragment = new RequestsFragment();
                    return  requestsFragment;
                case 2:
                    FindFriendsFragment findFriendsFragment = new FindFriendsFragment();
                    return  findFriendsFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            return tabLayout.getTabCount();
        }
    }


    private void setViewPager(){

        tabLayout.addTab(tabLayout.newTab().setCustomView(R.layout.tab_chat));
        tabLayout.addTab(tabLayout.newTab().setCustomView(R.layout.tab_requests));
        tabLayout.addTab(tabLayout.newTab().setCustomView(R.layout.tab_findfriends));

        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        Adapter  adapter = new Adapter(getSupportFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        viewPager.setAdapter(adapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });


        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if(id==R.id.mnuProfile)
        {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    private  boolean doubleBackPressed = false;

    @Override
    public void onBackPressed() {
       // super.onBackPressed();

        if(tabLayout.getSelectedTabPosition()>0)
        {
            tabLayout.selectTab(tabLayout.getTabAt(0));
        }
        else
        {
            if(doubleBackPressed)
            {
                finishAffinity();
            }
            else
            {
                doubleBackPressed=true;
                Toast.makeText(this, R.string.press_back_to_exit, Toast.LENGTH_SHORT).show();
                //delay
                android.os.Handler handler = new android.os.Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        doubleBackPressed=false;
                    }
                }, 2000);

            }
        }
    }
}














