package com.example.shoppinglistapp2.activities;

import android.app.Activity;
import android.os.Bundle;

import com.example.shoppinglistapp2.R;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {


    private Activity activity = this;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


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