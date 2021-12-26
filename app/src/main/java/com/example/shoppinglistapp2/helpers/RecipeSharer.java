package com.example.shoppinglistapp2.helpers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.example.shoppinglistapp2.R;
import com.example.shoppinglistapp2.db.tables.relations.RecipeWithTagsAndIngredients;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class RecipeSharer {
    public static void launchSharingIntent(Context context, List<RecipeWithTagsAndIngredients> recipesToShare) {
        Gson gson = new Gson();
        String json = gson.toJson(recipesToShare);

        String fileName = context.getString(R.string.share_recipe_default_filename);

        //name the file based on the recipe if there's only one recipe
        if(recipesToShare.size() == 1) {
            fileName = recipesToShare.get(0).getRecipe().getName();
        }

        //create file
        File file = new File(context.getCacheDir(), fileName + ".pmsr");
        file.deleteOnExit();

        //write json to file
        try {
            Files.asCharSink(file, Charsets.UTF_8).write(json);
        } catch (IOException ioException) {
            ErrorsUI.showToast(context, R.string.error_sharing_file);
        }

        //open chooser to choose a method to share the json with
        Intent intent = new Intent(Intent.ACTION_SEND);
        if(file.exists()) {
            Uri contentUri = FileProvider.getUriForFile(context, "com.package.example", file);

            intent.setType("text/json");
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);

            intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_recipe_subject));
            intent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_recipe_body_text));

            context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_recipe_prompt_title)));
        }
        else {
            ErrorsUI.showToast(context, R.string.error_sharing_file);
        }
    }
}
