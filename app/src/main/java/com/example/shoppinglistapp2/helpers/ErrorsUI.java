package com.example.shoppinglistapp2.helpers;

import android.content.Context;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.shoppinglistapp2.R;

public class ErrorsUI {
    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    public static void showToast(Context context, int resourceId) {
        Toast.makeText(context, context.getResources().getString(resourceId), Toast.LENGTH_LONG).show();
    }

    public static void showDefaultToast(Context context) {
        Toast.makeText(context, context.getResources().getString(R.string.unknown_error), Toast.LENGTH_LONG).show();
    }

    public static void showAlert(Context context, int resourceId) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.error_title)
                .setMessage(resourceId)
                .setPositiveButton(R.string.ok, null)
                .show();
    }
}
