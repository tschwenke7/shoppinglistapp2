package com.example.shoppinglistapp2.activities;

import android.os.Bundle;
import android.util.Log;

import com.example.shoppinglistapp2.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_recipe_list, R.id.nav_shopping_list)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);


//        //add animations to bottom navigation bar
//        NavOptions options = new NavOptions.Builder()
//                .setLaunchSingleTop(true)
//
//                .setEnterAnim(R.anim.slide_in_right)
//                .setExitAnim(R.anim.wait_anim)
//                .build();
//
//        navView.setOnNavigationItemSelectedListener((item -> {
//            switch(item.getItemId()){
//                case R.id.nav_shopping_list:
//                    navController.navigate(R.id.nav_shopping_list, null, options);
//                    break;
//                case R.id.nav_recipe_list:
//                    navController.navigate(R.id.nav_recipe_list, null, options);
//            }
//            return true;
//        }));


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

}