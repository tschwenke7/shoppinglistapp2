package com.example.shoppinglistapp2.helpers;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

import com.google.android.material.chip.Chip;

public class RecursiveViewHelpers {
    public static void setClickable(View view, boolean clickable) {
        if (view != null) {
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    setClickable(viewGroup.getChildAt(i), clickable);
                }
            }
            view.setClickable(clickable);
        }
    }

    public static void setEnabled(View view, boolean enabled) {
        if (view != null) {
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    setEnabled(viewGroup.getChildAt(i), enabled);
                }
            }
            if(view instanceof Button || view instanceof EditText) {
                view.setEnabled(enabled);
            }
        }
    }
}
