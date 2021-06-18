package com.example.shoppinglistapp2.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.shoppinglistapp2.db.dao.IngredientDao;
import com.example.shoppinglistapp2.db.dao.MealPlanDao;
import com.example.shoppinglistapp2.db.dao.RecipeDao;
import com.example.shoppinglistapp2.db.dao.SlItemDao;
import com.example.shoppinglistapp2.db.dao.TagDao;
import com.example.shoppinglistapp2.db.tables.Ingredient;
import com.example.shoppinglistapp2.db.tables.MealPlan;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.example.shoppinglistapp2.db.tables.SlItem;
import com.example.shoppinglistapp2.db.tables.Tag;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Recipe.class, Ingredient.class, SlItem.class, Tag.class, MealPlan.class}, version = 11, exportSchema = false)
public abstract class SlaDatabase extends RoomDatabase {
    public abstract RecipeDao recipeDao();
    public abstract IngredientDao ingredientDao();
    public abstract SlItemDao slItemDao();
    public abstract TagDao tagDao();
    public abstract MealPlanDao mealPlanDao();

    private static volatile SlaDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    static SlaDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (SlaDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            SlaDatabase.class, "sla_database")
                            .addCallback(sRoomDatabaseCallback)
                            .addMigrations(MIGRATION_10_11)
//                            .fallbackToDestructiveMigration()
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
        }
    };

    private static final Migration MIGRATION_7_8 = new Migration(7,8){
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(new StringBuilder()
                    .append("PRAGMA foreign_keys=off;")
                    .append("BEGIN TRANSACTION;\n")
                    .append("ALTER TABLE slitems RENAME TO old_table;\n")
                    .append("CREATE TABLE slitems\n")
                    .append("(\n")
                    .append("id INTEGER PRIMARY KEY AUTOINCREMENT,\n")
                    .append("name VARCHAR NOT NULL,\n")
                    .append("qty1 VARCHAR,\n")
                    .append("unit1 VARCHAR,\n")
                    .append("qty2 VARCHAR,\n")
                    .append("unit2 VARCHAR,\n")
                    .append("checked INTEGER DEFAULT = '0'\n")
                    .append(");\n")
                    .append("INSERT INTO slitems SELECT * FROM old_table;\n")
                    .append("COMMIT;\n")
                    .append("PRAGMA foreign_keys=on;")
                    .toString());
        }
    };

    private static final Migration MIGRATION_9_10 = new Migration(9,10){
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(new StringBuilder()
                    .append("ALTER TABLE slitems ADD list_id INTEGER DEFAULT 2; \n")
                    .append("UPDATE slitems SET list_id = 2; \n")
                    .toString());

            database.execSQL(new StringBuilder()
                    .append("CREATE TABLE meal_plans\n")
                    .append("(\n")
                    .append("id INTEGER PRIMARY KEY AUTOINCREMENT,\n")
                    .append("plan_id INTEGER DEFAULT 1,\n")
                    .append("day_id INTEGER,\n")
                    .append("day_title VARCHAR,\n")
                    .append("recipe_id INTEGER REFERENCES recipe(id) ON DELETE CASCADE,\n")
                    .append("notes VARCHAR\n")
                    .append(");\n")
                    .toString());
        }
    };
    private static final Migration MIGRATION_10_11 = new Migration(10,11){
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(new StringBuilder()
                    .append("BEGIN TRANSACTION;\n")
                    .toString());

            database.execSQL(new StringBuilder()
                    .append("ALTER TABLE meal_plans RENAME TO temp_table; \n")
                    .toString());

            database.execSQL(new StringBuilder()
                    .append("CREATE TABLE meal_plans\n")
                    .append("(\n")
                    .append("id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,\n")
                    .append("plan_id INTEGER DEFAULT 1 NOT NULL,\n")
                    .append("day_id INTEGER NOT NULL,\n")
                    .append("day_title VARCHAR,\n")
                    .append("recipe_id INTEGER REFERENCES recipes(id) ON DELETE CASCADE,\n")
                    .append("notes VARCHAR\n")
                    .append(");\n")
                    .toString());

            database.execSQL(new StringBuilder()
                    .append("INSERT INTO meal_plans(id, plan_id, day_id, day_title, recipe_id, notes)\n")
                    .append("SELECT * FROM temp_table;\n")
                    .toString());

            database.execSQL(new StringBuilder()
                    .append("COMMIT;")
                    .toString());
        }
    };
}
