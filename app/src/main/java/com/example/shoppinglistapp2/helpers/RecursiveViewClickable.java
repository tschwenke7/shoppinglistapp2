package com.example.shoppinglistapp2.helpers;

import android.view.View;
import android.view.ViewGroup;

public class RecursiveViewClickable {
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
}
