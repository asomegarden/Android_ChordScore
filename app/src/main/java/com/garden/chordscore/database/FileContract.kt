package com.garden.chordscore.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns

object FileContract {
    object FileEntry: BaseColumns{
        const val TABLE_NAME = "files"
        const val COLUMN_NAME_ID = "id"
        //폴더또는 파일별 고유 아이디 필요
        const val COLUMN_NAME_IS_FOLDER = "is_folder"
        //Boolean 타입
        const val COLUMN_NAME_FOLDER_NAME = "name"
        //ㅍ폴더명
        const val COLUMN_NAME_HAVING_FILES = "have_files"
        //21321442|134124214|23124124
        //불러와서 파일이면 파일로 로딩 폴더면 폴더로 로딩
        const val COLUMN_NAME_FILE_ID = "file_id"
        //파일이면 파일 아이디 불러와서 이름이랑 표시하는 걸로
    }

    private const val SQL_CREATE_ENTRIES =
        "CREATE TABLE ${FileEntry.TABLE_NAME} (" +
                "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                "${FileEntry.COLUMN_NAME_ID} TEXT," +
                "${FileEntry.COLUMN_NAME_IS_FOLDER} TEXT," +
                "${FileEntry.COLUMN_NAME_FOLDER_NAME} TEXT," +
                "${FileEntry.COLUMN_NAME_HAVING_FILES} TEXT," +
                "${FileEntry.COLUMN_NAME_FILE_ID} TEXT)"

    private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${FileEntry.TABLE_NAME}"

    class FileDBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION){
        override fun onCreate(db: SQLiteDatabase?) {
            db?.execSQL(FileContract.SQL_CREATE_ENTRIES)
        }

        override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
            db?.execSQL(FileContract.SQL_DELETE_ENTRIES)
            onCreate(db)
        }

        override fun onDowngrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
            onUpgrade(db, oldVersion, newVersion)
        }

        companion object{
            const val DATABASE_VERSION = 1
            const val DATABASE_NAME = "FileDB.db"
        }
    }

}