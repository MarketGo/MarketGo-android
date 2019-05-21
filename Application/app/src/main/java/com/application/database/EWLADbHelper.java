package com.application.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

// 해당 클래스는 helper로 데이터베이스를 컨넥션 연동 시키기 위해 제작된 클래스입니다.
public class EWLADbHelper extends SQLiteOpenHelper {
    private static EWLADbHelper sInstance;

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "EWLA.db";

    private static final String SQL_CREATE_THEME_ENTRIES =
            String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT, %s NUMERIC, %s INTEGER)",
                    ThemeContract.Theme.TABLE_NAME,
                    ThemeContract.Theme.THEME_NAME_ID,
                    ThemeContract.Theme.THEME_NAME_TITLE,
                    ThemeContract.Theme.THEME_NAME_ISLOCKED,
                    ThemeContract.Theme.THEME_NAME_UNLOCKPOINT);

    private static final String SQL_CREATE_UNIT_ENTRIES =
            String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s INTEGER, %s TEXT, %s NUMERIC)",
                    UnitContract.Unit.TABLE_NAME,
                    UnitContract.Unit.UNIT_NAME_THEME_ID,
                    UnitContract.Unit.UNIT_NAME_ID,
                    UnitContract.Unit.UNIT_NAME_TITLE,
                    UnitContract.Unit.UNIT_NAME_HASCROWN);

    private static final String SQL_CREATE_WORD_ENTRIES =
            String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s INTEGER, %s TEXT, %s TEXT, %s TEXT, %s TEXT)",
                    WordContract.WordEntry.TABLE_NAME,
                    WordContract.WordEntry._ID,
                    WordContract.WordEntry.COLUMN_NAME_UNIT_ID,
                    WordContract.WordEntry.COLUMN_NAME_IMAGE_SRC,
                    WordContract.WordEntry.COLUMN_NAME_KOREAN,
                    WordContract.WordEntry.COLUMN_NAME_ENGLISH,
                    WordContract.WordEntry.COLUMN_NAME_SHADOW_SRC);


    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + WordContract.WordEntry.TABLE_NAME;

    private static final String SQL_DELETE_THEME =
            "DROP TABLE IF EXISTS " + ThemeContract.Theme.TABLE_NAME;

    private static final String SQL_DELETE_UNIT =
            "DROP TABLE IF EXISTS " + UnitContract.Unit.TABLE_NAME;

    public static EWLADbHelper getsInstance(Context context) {
        if (sInstance == null) {
            sInstance = new EWLADbHelper(context);
        }
        return sInstance;
    }

    private EWLADbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(SQL_CREATE_WORD_ENTRIES);
        db.execSQL(SQL_CREATE_THEME_ENTRIES);
        db.execSQL(SQL_CREATE_UNIT_ENTRIES);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion == DB_VERSION) {
            db.execSQL(SQL_DELETE_ENTRIES);
            db.execSQL(SQL_DELETE_THEME);
            db.execSQL(SQL_DELETE_UNIT);
            onCreate(db);
        }
    }
}