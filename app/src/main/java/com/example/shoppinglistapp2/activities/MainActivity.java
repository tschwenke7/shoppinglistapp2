package com.example.shoppinglistapp2.activities;

import android.os.Bundle;
import android.view.Menu;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.ui.ViewPagerNavigationCallback;
import com.example.shoppinglistapp2.activities.ui.mealplan.MealPlanFragment;
import com.example.shoppinglistapp2.activities.ui.recipes.RecipesParentFragment;
import com.example.shoppinglistapp2.activities.ui.shoppinglist.ShoppingListFragment;
import com.google.android.material.tabs.TabLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;


public class MainActivity extends AppCompatActivity implements MealPlanFragment.Callback, ViewPagerNavigationCallback {

    private TabLayout tabLayout;
    private ViewPager viewPager;
    private SectionsPagerAdapter sectionsPagerAdapter;
//    private Stack<Integer> viewPagerBackStack = new Stack<>();
//    private boolean saveToBackStack = true;
//    private int lastPage = 0;

    public static int MEAL_PLAN_VIEWPAGER_INDEX = 0;
    public static int RECIPE_LIST_VIEWPAGER_INDEX = 1;
    public static int SHOPPING_LIST_VIEWPAGER_INDEX = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);

        viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);

        tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.getTabAt(0).setIcon(R.drawable.ic_calender);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_baseline_menu_book_24);
        tabLayout.getTabAt(2).setIcon(R.drawable.ic_baseline_format_list_bulleted_24);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                invalidateOptionsMenu();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        //we don't want multiple copies of the same page on backstack
//                //so duplicates replace the previous occurrence
//                if(viewPagerBackStack.contains(lastPage)){
//                    viewPagerBackStack.remove(Integer.valueOf(lastPage));
//                }
//                //add the previous page to the viewpager back stack, unless we navigated by pressing
//                //the back button (i.e. saveToBackStack == false)
//                if(saveToBackStack){
//                    viewPagerBackStack.push(lastPage);
//                }
//                lastPage = position;

        //start on shopping list tab
//        viewPager.setCurrentItem(1, false);
    }

    //enable the back button in action bar to go to previous fragment
    @Override
    public void onBackPressed() {
//        else if (!viewPagerBackStack.empty()){
//            saveToBackStack = false;
//            viewPager.setCurrentItem(viewPagerBackStack.pop());
//            saveToBackStack = true;
//        }
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
            Fragment fragment = null;
            switch (position) {
                case 0:
                    fragment = MealPlanFragment.newInstance();
                    break;
                case 1:
                    fragment = RecipesParentFragment.newInstance();
                    break;
                case 2:
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
                case 0:
                    return getResources().getString(R.string.title_meal_plan);
                case 1:
                    return getResources().getString(R.string.title_recipes);
                case 2:
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
                R.menu.meal_plan_action_bar,
                R.menu.recipe_list_action_bar,
                R.menu.shopping_list_action_bar
        };
        super.onCreateOptionsMenu(menu);
        menu.clear();
        getMenuInflater().inflate(menuResourceIds[viewPager.getCurrentItem()], menu);
        return true;
    }
}