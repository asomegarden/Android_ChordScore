package com.garden.chordscore.activity

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.BaseColumns
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.garden.chordscore.customrecyclerview.CustomItemAdapter
import com.garden.chordscore.customrecyclerview.ScoreFileData
import com.garden.chordscore.R
import com.garden.chordscore.database.FileContract
import com.garden.chordscore.database.ScoreContract
import java.sql.Time
import java.util.*
import androidx.core.app.ActivityCompat

import android.content.SharedPreferences
import android.util.Log
import android.view.View

import android.widget.Toast





class MainActivity : AppCompatActivity() {
    private lateinit var scoreDBHelper: ScoreContract.ScoreDBHelper
    private lateinit var fileDBHelper: FileContract.FileDBHelper

    private lateinit var fileAdapter: CustomItemAdapter
    private val data = mutableListOf<ScoreFileData>()
    private lateinit var listview: RecyclerView

    private lateinit var folderName: TextView

    private var curFolderData: MutableMap<String, String> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        curFolderData.apply {
            put(FileContract.FileEntry.COLUMN_NAME_ID, "")
            put(FileContract.FileEntry.COLUMN_NAME_IS_FOLDER, "")
            put(FileContract.FileEntry.COLUMN_NAME_FOLDER_NAME, "")
            put(FileContract.FileEntry.COLUMN_NAME_HAVING_FILES, "")
            put(FileContract.FileEntry.COLUMN_NAME_PREV, "none")
        }

        val btnCreateFolder: ImageButton = findViewById(R.id.btn_create_folder)
        val btnCreateNote: ImageButton = findViewById(R.id.btn_create_note)

        folderName = findViewById(R.id.text_current_locate)
        listview = findViewById(R.id.listview)

        if(intent.hasExtra("id")){
            initRecycler()
            loadFolder(intent.getStringExtra("id").toString())
        }else {
            initRecycler()
            initMainFolder()
        }

        btnCreateFolder.setOnClickListener{
            var id = System.currentTimeMillis().hashCode().toString()
            val count = data.count{ it.name.slice(0..9) == "new folder" }

            createFile(id, if(count == 0)"new folder" else "new folder ($count)")

            updateCurFolderData(FileContract.FileEntry.COLUMN_NAME_HAVING_FILES, id)
        }

        btnCreateNote.setOnClickListener{
            val intent: Intent = Intent(this, ScoreActivity::class.java)

            val id = System.currentTimeMillis().toString()
            val count = data.count{ it.name.slice(0..8) == "new score" }

            val name = if(count == 0) "new score" else "new score ($count)"
            //create_new_note
            scoreDBHelper = ScoreContract.ScoreDBHelper(this)
            val db = scoreDBHelper.writableDatabase
            var values = ContentValues().apply {
                put(ScoreContract.ScoreEntry.COLUMN_NAME_ID, id)
                put(ScoreContract.ScoreEntry.COLUMN_NAME_TITLE, name)
                put(ScoreContract.ScoreEntry.COLUMN_NAME_LINES, 1)
                put(ScoreContract.ScoreEntry.COLUMN_NAME_CHORDS, "")
                put(ScoreContract.ScoreEntry.COLUMN_NAME_LYRICS, "")
            }
            val newRowId = db?.insert(ScoreContract.ScoreEntry.TABLE_NAME, null, values)
            scoreDBHelper.close()
            
            //이때 Score 데이터 생성과 동시에 File 데이터도 생성해서 폴더에 따로 포함시켜줘야함
            //일단 목이 아파서 오늘은 여기까지

            createFile(id = id, name = name, isFolder = false)
            updateCurFolderData(FileContract.FileEntry.COLUMN_NAME_HAVING_FILES, id)

            intent.putExtra("id", id)
            intent.putExtra("prev", curFolderData[FileContract.FileEntry.COLUMN_NAME_ID])
            startActivity(intent)
        }

        fileAdapter?.setOnItemClickListener(object: CustomItemAdapter.OnItemClickListener{
            override fun onItemClick(v: View, fileData: ScoreFileData, position: Int) {
                if(fileData.isDirectory){
                    loadFolder(fileData.id)
                }else{
                    val intent: Intent = Intent(this@MainActivity, ScoreActivity::class.java)

                    intent.putExtra("id", fileData.id)
                    intent.putExtra("prev", curFolderData[FileContract.FileEntry.COLUMN_NAME_ID])
                    startActivity(intent)
                }
            }
        })

    }

    private fun updateCurFolderData(updateKey: String, updateValue: String){
        if(updateKey == FileContract.FileEntry.COLUMN_NAME_HAVING_FILES){
            curFolderData[updateKey] += ("$updateValue|")
        }else{
            curFolderData[updateKey] = updateValue
        }

        fileDBHelper = FileContract.FileDBHelper(this)
        val db = fileDBHelper.writableDatabase

        var values = ContentValues().apply {
            put(FileContract.FileEntry.COLUMN_NAME_ID, curFolderData[FileContract.FileEntry.COLUMN_NAME_ID])
            put(FileContract.FileEntry.COLUMN_NAME_IS_FOLDER, curFolderData[FileContract.FileEntry.COLUMN_NAME_IS_FOLDER])
            put(FileContract.FileEntry.COLUMN_NAME_HAVING_FILES, curFolderData[FileContract.FileEntry.COLUMN_NAME_HAVING_FILES])
            put(FileContract.FileEntry.COLUMN_NAME_FOLDER_NAME, curFolderData[FileContract.FileEntry.COLUMN_NAME_FOLDER_NAME])
            put(FileContract.FileEntry.COLUMN_NAME_PREV, curFolderData[FileContract.FileEntry.COLUMN_NAME_PREV])
        }

        val selection = "${FileContract.FileEntry.COLUMN_NAME_ID} LIKE ?"
        val selectionArgs = arrayOf(curFolderData[FileContract.FileEntry.COLUMN_NAME_ID])

        val count = db?.update(
            FileContract.FileEntry.TABLE_NAME,
            values,
            selection,
            selectionArgs
        )
    }

    private fun loadFolder(id: String): Boolean{
        data.clear()
        fileDBHelper = FileContract.FileDBHelper(this)
        val db = fileDBHelper.readableDatabase

        val projection = arrayOf(
            FileContract.FileEntry.COLUMN_NAME_ID,
            FileContract.FileEntry.COLUMN_NAME_IS_FOLDER,
            FileContract.FileEntry.COLUMN_NAME_FOLDER_NAME,
            FileContract.FileEntry.COLUMN_NAME_HAVING_FILES,
            FileContract.FileEntry.COLUMN_NAME_PREV)

        val selection = "${FileContract.FileEntry.COLUMN_NAME_ID} = ?"
        val selectionArgs = arrayOf(id)

        val cursor = db.query(
            FileContract.FileEntry.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        if(cursor.moveToFirst()){

            curFolderData[FileContract.FileEntry.COLUMN_NAME_ID] = cursor.getString(0)
            curFolderData[FileContract.FileEntry.COLUMN_NAME_IS_FOLDER] = cursor.getString(1)
            curFolderData[FileContract.FileEntry.COLUMN_NAME_FOLDER_NAME] = cursor.getString(2)
            curFolderData[FileContract.FileEntry.COLUMN_NAME_HAVING_FILES] = cursor.getString(3)
            curFolderData[FileContract.FileEntry.COLUMN_NAME_PREV] = cursor.getString(4)

            val fileIDList: MutableList<String> = mutableListOf<String>()
            folderName.text = cursor.getString(2)

            for(fileData in cursor.getString(3).split("|")){
                if(fileData.length > 1) fileIDList.add(fileData)
            }

            loadFileFromID(fileIDList)

        }else{
            fileDBHelper.close()
            return false
        }

        fileDBHelper.close()
        return true
    }

    private fun loadFileFromID(idList: MutableList<String>){
        fileDBHelper = FileContract.FileDBHelper(this)
        val db = fileDBHelper.readableDatabase

        val projection = arrayOf(
            FileContract.FileEntry.COLUMN_NAME_ID,
            FileContract.FileEntry.COLUMN_NAME_IS_FOLDER,
            FileContract.FileEntry.COLUMN_NAME_FOLDER_NAME)

        for(id in idList){

            val selection = "${FileContract.FileEntry.COLUMN_NAME_ID} = ?"
            val selectionArgs = arrayOf(id)

            val cursor = db.query(
                FileContract.FileEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
            )

            if(cursor.moveToFirst()){
                if(cursor.getString(1).toBoolean()){
                    data.add(ScoreFileData(img = R.drawable.ic_folder, name = cursor.getString(2), isDirectory = true, id = cursor.getString(0),
                        author = "", prevFolderID = curFolderData[FileContract.FileEntry.COLUMN_NAME_ID].toString()))
                }else{
                    loadScoreFromID(cursor.getString(0))
                }
            }
        }

        applyRecycler()

        fileDBHelper.close()
    }

    private fun loadScoreFromID(id: String){
        scoreDBHelper = ScoreContract.ScoreDBHelper(this)
        val db = scoreDBHelper.readableDatabase

        val projection = arrayOf(
            ScoreContract.ScoreEntry.COLUMN_NAME_TITLE)

        val selection = "${ScoreContract.ScoreEntry.COLUMN_NAME_ID} = ?"
        val selectionArgs = arrayOf(id)

        val cursor = db.query(
            ScoreContract.ScoreEntry.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        if(cursor.moveToFirst()){
            data.add(ScoreFileData(img = R.drawable.ic_file, name = cursor.getString(0),
                isDirectory = false, id = id, author = cursor.getString(0), prevFolderID = curFolderData[FileContract.FileEntry.COLUMN_NAME_ID].toString()))
        }
        scoreDBHelper.close()
    }

    private fun createFile(id: String, name: String = "Home", isFolder: Boolean = true){
        fileDBHelper = FileContract.FileDBHelper(this)
        val db = fileDBHelper.writableDatabase
        var values = ContentValues().apply {
            put(FileContract.FileEntry.COLUMN_NAME_FOLDER_NAME, name)
            put(FileContract.FileEntry.COLUMN_NAME_HAVING_FILES, "")
            put(FileContract.FileEntry.COLUMN_NAME_ID, id)
            put(FileContract.FileEntry.COLUMN_NAME_IS_FOLDER, isFolder.toString())
            put(FileContract.FileEntry.COLUMN_NAME_PREV, curFolderData[FileContract.FileEntry.COLUMN_NAME_ID])
        }
        val newRowId = db?.insert(FileContract.FileEntry.TABLE_NAME, null, values)

        data.add(ScoreFileData(img = if(isFolder)R.drawable.ic_folder else R.drawable.ic_file, name = name,
            isDirectory = isFolder, id = id, author = "", prevFolderID = curFolderData[FileContract.FileEntry.COLUMN_NAME_ID].toString()
        ))

        applyRecycler()
        fileDBHelper.close()
    }

    private fun createMainFolder(id: String, name: String = "Home"){
        fileDBHelper = FileContract.FileDBHelper(this)
        val db = fileDBHelper.writableDatabase

        var values = ContentValues().apply {
            put(FileContract.FileEntry.COLUMN_NAME_FOLDER_NAME, name)
            put(FileContract.FileEntry.COLUMN_NAME_HAVING_FILES, "")
            put(FileContract.FileEntry.COLUMN_NAME_ID, id)
            put(FileContract.FileEntry.COLUMN_NAME_IS_FOLDER, true.toString())
            put(FileContract.FileEntry.COLUMN_NAME_PREV, "none")
        }
        val newRowId = db?.insert(FileContract.FileEntry.TABLE_NAME, null, values)

        curFolderData[FileContract.FileEntry.COLUMN_NAME_ID] = id
        curFolderData[FileContract.FileEntry.COLUMN_NAME_IS_FOLDER] = true.toString()
        curFolderData[FileContract.FileEntry.COLUMN_NAME_FOLDER_NAME] = name
        curFolderData[FileContract.FileEntry.COLUMN_NAME_HAVING_FILES] = ""
        curFolderData[FileContract.FileEntry.COLUMN_NAME_PREV] = "none"

        folderName.text = name
        fileDBHelper.close()
    }

    private fun initMainFolder(){
        if(!loadFolder("MAIN")){
            createMainFolder("MAIN")
        }
    }

    private fun initRecycler(){
        fileAdapter = CustomItemAdapter(this)
        listview.adapter = fileAdapter
        fileAdapter.data = data
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun applyRecycler() {
        fileAdapter.notifyDataSetChanged()
    }

    //뒤로가기 두번 누르면 종료
    private var backKeyPressedTime: Long = 0
    private var toast: Toast? = null
    val PREFNAME = "Preferences"

    override fun onBackPressed() {
        if(curFolderData[FileContract.FileEntry.COLUMN_NAME_PREV] == "none"){
            if (System.currentTimeMillis() > backKeyPressedTime + 2000) {
                backKeyPressedTime = System.currentTimeMillis()
                toast = Toast.makeText(this, "뒤로 버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT)
                toast?.show()
                return
            }

            if (System.currentTimeMillis() <= backKeyPressedTime + 2000) {
                val settings = getSharedPreferences(PREFNAME, MODE_PRIVATE)
                val editor = settings.edit()
                editor.putBoolean("Appexec", true)
                editor.apply()
                ActivityCompat.finishAffinity(this)
                toast?.cancel()
            }
        }else{
            loadFolder(curFolderData[FileContract.FileEntry.COLUMN_NAME_PREV].toString())
        }
    }
}