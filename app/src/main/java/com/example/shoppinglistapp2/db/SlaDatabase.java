package com.example.shoppinglistapp2.db;

import android.app.slice.Slice;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.shoppinglistapp2.db.dao.IngredientDao;
import com.example.shoppinglistapp2.db.dao.RecipeDao;
import com.example.shoppinglistapp2.db.dao.SlItemDao;
import com.example.shoppinglistapp2.db.dao.TagDao;
import com.example.shoppinglistapp2.db.tables.Ingredient;
import com.example.shoppinglistapp2.db.tables.Recipe;
import com.example.shoppinglistapp2.db.tables.SlItem;
import com.example.shoppinglistapp2.db.tables.Tag;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Recipe.class, Ingredient.class, SlItem.class, Tag.class}, version = 9, exportSchema = false)
public abstract class SlaDatabase extends RoomDatabase {
    public abstract RecipeDao recipeDao();
    public abstract IngredientDao ingredientDao();
    public abstract SlItemDao slItemDao();
    public abstract TagDao tagDao();

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
//                            .addMigrations(MIGRATION_7_8)
                            .fallbackToDestructiveMigration()
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


}
