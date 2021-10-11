package com.example.shoppinglistapp2.activities;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.activities.importRecipes.ImportRecipesFragmentDirections;
import com.google.common.io.Files;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


public class MainActivity extends AppCompatActivity {


    private static final String TAG = "T_DBG_MAIN_ACTIVITY";
    private Activity activity = this;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: " + getIntent().toString());
        if(getIntent() != null) {
            handleIntent(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    public void handleIntent(Intent intent) {
        String action = intent.getAction();

        if(Intent.ACTION_VIEW.equals(action)) {
            String scheme = intent.getScheme();
            ContentResolver resolver = getContentResolver();

            if (scheme.compareTo(ContentResolver.SCHEME_CONTENT) == 0) {
                Uri uri = intent.getData();
                StringBuilder builder = new StringBuilder();

                try (InputStream inputStream = resolver.openInputStream(uri)){

                    Reader reader = new BufferedReader(
                            new InputStreamReader(
                                    inputStream, Charset.forName(StandardCharsets.UTF_8.name())));
                    int c;
                    while ((c = reader.read()) != -1) {
                        builder.append((char) c);
                    }

                    Log.d(TAG, builder.toString());

                    NavDirections navAction = ImportRecipesFragmentDirections
                            .actionGlobalImportRecipesFragment(builder.toString());

                    NavHostFragment navHostFragment =
                            (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
                    NavController navController = navHostFragment.getNavController();

                    navController
                            .navigate(navAction);

                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
            else if (scheme.compareTo(ContentResolver.SCHEME_FILE) == 0) {
                Uri uri = intent.getData();
                try {
                    Files.toString(new File(uri.getPath()), StandardCharsets.UTF_8);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }

                Log.v("tag" , "File intent detected: " + action + " : " + intent.getDataString() + " : " + intent.getType() + " : ");
                InputStream input = null;
                try {
                    input = resolver.openInputStream(uri);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
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

}