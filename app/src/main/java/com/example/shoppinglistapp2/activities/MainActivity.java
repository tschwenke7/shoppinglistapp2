package com.example.shoppinglistapp2.activities;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.View;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.ui.ViewPagerNavigationCallback;
import com.example.shoppinglistapp2.activities.ui.mealplan.MealPlanFragment;
import com.example.shoppinglistapp2.activities.ui.recipes.RecipesParentFragment;
import com.example.shoppinglistapp2.activities.ui.shoppinglist.ShoppingListFragment;
import com.example.shoppinglistapp2.helpers.KeyboardHider;
import com.google.android.material.tabs.TabLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;


public class MainActivity extends AppCompatActivity implements MealPlanFragment.Callback, ViewPagerNavigationCallback {

    private TabLayout tabLayout;
    private ViewPager viewPager;
    private SectionsPagerAdapter sectionsPagerAdapter;
    private Activity activity = this;


    public static final int SHOPPING_LIST_VIEWPAGER_INDEX = 0;
    public static final int RECIPE_LIST_VIEWPAGER_INDEX = 1;
    public static final int MEAL_PLAN_VIEWPAGER_INDEX = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);

        viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);

        tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.getTabAt(MEAL_PLAN_VIEWPAGER_INDEX).setIcon(R.drawable.ic_calender);
        tabLayout.getTabAt(RECIPE_LIST_VIEWPAGER_INDEX).setIcon(R.drawable.ic_baseline_menu_book_24);
        tabLayout.getTabAt(SHOPPING_LIST_VIEWPAGER_INDEX).setIcon(R.drawable.ic_baseline_format_list_bulleted_24);

        //hide keyboard when we swap tabs
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                KeyboardHider.hideKeyboard(activity);
            }

            @Override
            public void onPageSelected(int position) {
                KeyboardHider.hideKeyboard(activity);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        //hide tab layout when keyboard opened
        KeyboardVisibilityEvent.setEventListener(this,
                isOpen -> {
            if(isOpen) {
                tabLayout.setVisibility(View.GONE);
            }
            else {
                //delay return to make it look less bad/hide flicker
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(() -> tabLayout.setVisibility(View.VISIBLE), 100);
            }
        });
    }

    //enable the back button in action bar to go to previous fragment
    @Override
    public void onBackPressed() {
        super.onBackPressed();
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
            KeyboardHider.hideKeyboard(activity);
            Fragment fragment = null;
            switch (position) {
                case MEAL_PLAN_VIEWPAGER_INDEX:
                    fragment = MealPlanFragment.newInstance();
                    break;
                case RECIPE_LIST_VIEWPAGER_INDEX:
                    fragment = RecipesParentFragment.newInstance();
                    break;
                case SHOPPING_LIST_VIEWPAGER_INDEX:
                    fragment = new ShoppingListFragment();
                    break;
            }
            return fragment;
        }
        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case MEAL_PLAN_VIEWPAGER_INDEX:
                    return getResources().getString(R.string.title_meal_plan);
                case RECIPE_LIST_VIEWPAGER_INDEX:
                    return getResources().getString(R.string.title_recipes);
                case SHOPPING_LIST_VIEWPAGER_INDEX:
                    return getResources().getString(R.string.title_shopping_list);
            }
            return null;
        }
    }

    @Override
    public void setViewpagerTo(int page) {
        viewPager.setCurrentItem(page);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final int[] menuResourceIds = new int[]{
                R.menu.shopping_list_action_bar,
                R.menu.recipe_list_action_bar,
                R.menu.meal_plan_action_bar
        };
        super.onCreateOptionsMenu(menu);
        menu.clear();
        getMenuInflater().inflate(menuResourceIds[viewPager.getCurrentItem()], menu);
        return true;
    }
}