package com.example.shoppinglistapp2.activities;

import android.os.Bundle;
import android.util.Log;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.ui.recipes.RecipesParentFragment;
import com.example.shoppinglistapp2.activities.ui.shoppinglist.ShoppingListFragment;
import com.google.android.material.tabs.TabLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;


public class MainActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager viewPager;
    private SectionsPagerAdapter sectionsPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);

        viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);

        tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.getTabAt(0).setIcon(R.drawable.ic_baseline_menu_book_24);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_baseline_format_list_bulleted_24);

        //start on shopping list tab
//        viewPager.setCurrentItem(1, false);
    }

    //enable the back button in action bar to go to previous fragment
    @Override
    public void onBackPressed() {
        Log.d("TOM_TEST", "onBackPressed: ");
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 1) {
            fragmentManager.popBackStackImmediate();
        } else {
            super.onBackPressed();
        }
    }

    public void showUpButton() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void hideUpButton() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        public SectionsPagerAdapter(FragmentManager fm, int behaviour) {
            super(fm, behaviour);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = null;
            switch (position) {
                case 0:
                    fragment = RecipesParentFragment.newInstance();
                    break;
                case 1:
                    fragment = new ShoppingListFragment();
                    break;
            }
            return fragment;
        }
        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getResources().getString(R.string.title_recipes);
                case 1:
                    return getResources().getString(R.string.title_shopping_list);
            }
            return null;
        }


    }
}