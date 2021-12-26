package com.example.shoppinglistapp2.helpers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;

public class Animations {
    public static void fadeSwap(int duration, View toHide, View... toShow) {
        //fade out toHide, setting visibility to gone after completion
        toHide.animate().alpha(0f).setDuration(duration)
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    toHide.setVisibility(View.GONE);
                    toHide.setAlpha(1f);
                }
            });

        //make toShow visible and fade it in
        for (View view: toShow) {
            view.setAlpha(0f);
            view.setVisibility(View.VISIBLE);
            view.animate().alpha(1f).setDuration(duration);
        }

    }
    public static void fadeSwap(View toHide, View... toShow) {
        fadeSwap(300, toHide, toShow);
    }
}
