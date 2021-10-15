package com.example.shoppinglistapp2.activities.mainContentFragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.navigation.Navigation;
import androidx.viewpager.widget.ViewPager;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.mainContentFragments.mealplan.MealPlanFragment;
import com.example.shoppinglistapp2.activities.mainContentFragments.recipes.RecipesParentFragment;
import com.example.shoppinglistapp2.activities.mainContentFragments.shoppinglist.ShoppingListParentFragment;
import com.example.shoppinglistapp2.activities.mainContentFragments.shoppinglist.list.ShoppingListFragment;
import com.example.shoppinglistapp2.databinding.FragmentMainContentBinding;
import com.example.shoppinglistapp2.helpers.KeyboardHelper;
import com.google.android.material.tabs.TabLayout;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MainContentFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainContentFragment extends Fragment {


    public static final int SHOPPING_LIST_VIEWPAGER_INDEX = 0;
    public static final int RECIPE_LIST_VIEWPAGER_INDEX = 1;
    public static final int MEAL_PLAN_VIEWPAGER_INDEX = 2;

    private TabLayout tabLayout;
    private ViewPager viewPager;
    private SectionsPagerAdapter sectionsPagerAdapter;

    private FragmentMainContentBinding binding;

    public MainContentFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static MainContentFragment newInstance() {
        MainContentFragment fragment = new MainContentFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentMainContentBinding.inflate(inflater, container, false);

        //enable menu
        setHasOptionsMenu(true);

        // Inflate the layout for this fragment
        if(sectionsPagerAdapter == null) {
            sectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        viewPager = binding.mainContentViewPager;
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setAdapter(sectionsPagerAdapter);

        tabLayout = binding.tabLayout;
        tabLayout.setupWithViewPager(viewPager);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        tabLayout.getTabAt(MainContentFragment.MEAL_PLAN_VIEWPAGER_INDEX).setIcon(R.drawable.ic_calender);
        tabLayout.getTabAt(MainContentFragment.RECIPE_LIST_VIEWPAGER_INDEX).setIcon(R.drawable.ic_baseline_menu_book_24);
        tabLayout.getTabAt(MainContentFragment.SHOPPING_LIST_VIEWPAGER_INDEX).setIcon(R.drawable.ic_baseline_format_list_bulleted_24);

        //hide keyboard when we swap tabs
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                KeyboardHelper.hideKeyboard(requireActivity());
            }

            @Override
            public void onPageSelected(int position) {
                KeyboardHelper.hideKeyboard(requireActivity());
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        //hide tab layout when keyboard opened
        KeyboardVisibilityEvent.setEventListener(requireActivity(),
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

        //swap viewpager to given page if specified in args
        int goToIndex = getArguments().getInt("setViewpagerTo");
        if (goToIndex != -1) {
            viewPager.setCurrentItem(goToIndex);
        }
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        public SectionsPagerAdapter(FragmentManager fm, int behaviour) {
            super(fm, behaviour);
        }

        @Override
        public Fragment getItem(int position) {
            KeyboardHelper.hideKeyboard(requireActivity());
            Fragment fragment = null;
            switch (position) {
                case MainContentFragment.MEAL_PLAN_VIEWPAGER_INDEX:
                    fragment = MealPlanFragment.newInstance();
                    break;
                case MainContentFragment.RECIPE_LIST_VIEWPAGER_INDEX:
                    fragment = RecipesParentFragment.newInstance();
                    break;
                case MainContentFragment.SHOPPING_LIST_VIEWPAGER_INDEX:
                    fragment = new ShoppingListParentFragment();
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
                case MainContentFragment.MEAL_PLAN_VIEWPAGER_INDEX:
                    return getResources().getString(R.string.title_meal_plan);
                case MainContentFragment.RECIPE_LIST_VIEWPAGER_INDEX:
                    return getResources().getString(R.string.title_recipes);
                case MainContentFragment.SHOPPING_LIST_VIEWPAGER_INDEX:
                    return getResources().getString(R.string.title_shopping_list);
            }
            return null;
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        menu.clear();
        super.onCreateOptionsMenu(menu, inflater);

        //inflate current viewpager page's specific menu
        final int[] menuResourceIds = new int[]{
                R.menu.shopping_list_action_bar,
                R.menu.recipe_list_action_bar,
                R.menu.meal_plan_action_bar
        };
        inflater.inflate(menuResourceIds[viewPager.getCurrentItem()], menu);

        //inflate settings icon
//        inflater.inflate(R.menu.main_content_menu, menu);
    }
    /** Respond to menu items from action bar being pressed */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("TOM_TEST", "onOptionsItemSelected: " + item.toString());
        switch (item.getItemId()) {
            //settings pressed
            case R.id.action_settings:
                Navigation.findNavController(requireView()).navigate(
                        MainContentFragmentDirections.actionMainContentFragmentToRootSettingsFragment());

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}