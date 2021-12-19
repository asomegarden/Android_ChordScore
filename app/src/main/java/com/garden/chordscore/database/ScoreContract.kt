package com.garden.chordscore.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns

object ScoreContract {
    object ScoreEntry: BaseColumns{
        const val TABLE_NAME = "scores"
        const val COLUMN_NAME_ID = "id"
        //(title + lines + chords + lyrics + create_time).hashcode()
        const val COLUMN_NAME_TITLE = "title"
        const val COLUMN_NAME_LINES = "lines"
        const val COLUMN_NAME_CHORDS = "chords"
        //1|E|40$1|Am7|60$
        const val COLUMN_NAME_LYRICS = "lyrics"
        //우리 다시 만난거라 그 골목길 어귀에서|지난 여름 그날처럼 난 또다시 설레이고|
    }

    private const val SQL_CREATE_ENTRIES =
        "CREATE TABLE ${ScoreEntry.TABLE_NAME} (" +
                "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                "${ScoreEntry.COLUMN_NAME_ID} TEXT," +
                "${ScoreEntry.COLUMN_NAME_TITLE} TEXT," +
                "${ScoreEntry.COLUMN_NAME_LINES} INTEGER," +
                "${ScoreEntry.COLUMN_NAME_CHORDS} TEXT," +
                "${ScoreEntry.COLUMN_NAME_LYRICS} TEXT)"

    private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${ScoreEntry.TABLE_NAME}"

    class ScoreDBHelper (context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION){
        override fun onCreate(db: SQLiteDatabase?) {
            db?.execSQL(SQL_CREATE_ENTRIES)
        }

        override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
            db?.execSQL(SQL_DELETE_ENTRIES)
            onCreate(db)
        }

        override fun onDowngrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
            onUpgrade(db, oldVersion, newVersion)
        }

        companion object{
            const val DATABASE_VERSION = 1
            const val DATABASE_NAME = "ScoreDB.db"
        }
    }
}