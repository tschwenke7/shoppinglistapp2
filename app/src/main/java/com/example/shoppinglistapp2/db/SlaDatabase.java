package com.example.shoppinglistapp2.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.shoppinglistapp2.db.dao.IngredientDao;
import com.example.shoppinglistapp2.db.dao.RecipeDao;
import com.example.shoppinglistapp2.db.tables.Ingredient;
import com.example.shoppinglistapp2.db.tables.Recipe;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Recipe.class, Ingredient.class}, version = 1, exportSchema = false)
public abstract class SlaDatabase extends RoomDatabase {
    public abstract RecipeDao recipeDao();
    public abstract IngredientDao ingredientDao();

    private static volatile SlaDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    static SlaDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (SlaDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            SlaDatabase.class, "word_database")
                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    //do stuff before startup if desired
    private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback(){
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);

            //todo - work out how to create a sample recipe to test recycler view
            databaseWriteExecutor.execute(() -> {
                RecipeDao dao = INSTANCE.recipeDao();

                Recipe recipe = new Recipe();
                recipe.setName("Sample recipe");
                recipe.setCookTime(25);
                recipe.setNotes("Sample notes would go here...");
                recipe.setTom_rating(2);
                recipe.setTier_rating(5);
                recipe.setUrl("www.google.com.au");

                dao.insertAll(recipe);
            });
        }
    };
}
