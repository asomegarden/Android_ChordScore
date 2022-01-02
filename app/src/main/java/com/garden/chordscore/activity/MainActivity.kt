package com.garden.chordscore.activity

import android.annotation.SuppressLint
import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.BaseColumns
import androidx.recyclerview.widget.RecyclerView
import com.garden.chordscore.customrecyclerview.CustomItemAdapter
import com.garden.chordscore.customrecyclerview.ScoreFileData
import com.garden.chordscore.R
import com.garden.chordscore.database.FileContract
import com.garden.chordscore.database.ScoreContract
import java.sql.Time
import java.util.*
import androidx.core.app.ActivityCompat

import android.graphics.Color
import android.media.Image
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import java.io.File
import java.text.FieldPosition


class MainActivity : AppCompatActivity() {
    private lateinit var scoreDBHelper: ScoreContract.ScoreDBHelper
    private lateinit var fileDBHelper: FileContract.FileDBHelper

    private lateinit var fileAdapter: CustomItemAdapter
    private val data = mutableListOf<ScoreFileData>()
    private lateinit var listview: RecyclerView

    private lateinit var btnBack: ImageButton
    private lateinit var folderName: TextView

    private var curFolderData: MutableMap<String, String> = mutableMapOf()

    private var previousPos: String = ""
    private var moveFile: String = ""

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
        val btnMoveDrop: Button = findViewById(R.id.btn_move_drop)
        val btnMoveCancel: Button = findViewById(R.id.btn_move_cancel)
        val textMoveFile: TextView = findViewById(R.id.textview_move_file)
        val layoutMove: RelativeLayout = findViewById(R.id.layout_move)
        val editSearch: EditText = findViewById(R.id.edit_search)

        btnBack = findViewById(R.id.btn_back)
        folderName = findViewById(R.id.text_current_locate)
        listview = findViewById(R.id.listview)

        if(intent.hasExtra("id")){
            initRecycler()
            loadFolder(intent.getStringExtra("id").toString())
        }else {
            initRecycler()
            initMainFolder()
            btnBack.visibility = View.GONE
        }

        btnCreateFolder.setOnClickListener{
            var id = System.currentTimeMillis().hashCode().toString()
            val count = data.count{
                if(it.name.length > 9) it.name.slice(0..9) == "new folder" else false
            }

            createFile(id, if(count == 0)"new folder" else "new folder ($count)")

            updateCurFolderData(FileContract.FileEntry.COLUMN_NAME_HAVING_FILES, id)
        }

        btnCreateNote.setOnClickListener{
            val intent: Intent = Intent(this, ScoreActivity::class.java)

            val id = System.currentTimeMillis().toString()
            val count = data.count{ if(it.name.length > 7) it.name.slice(0..8) == "new score" else false }

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
            db?.insert(ScoreContract.ScoreEntry.TABLE_NAME, null, values)
            scoreDBHelper.close()

            createFile(id = id, name = name, isFolder = false)
            updateCurFolderData(FileContract.FileEntry.COLUMN_NAME_HAVING_FILES, id)

            intent.putExtra("id", id)
            intent.putExtra("prev", curFolderData[FileContract.FileEntry.COLUMN_NAME_ID])
            startActivity(intent)
        }

        btnBack.setOnClickListener {
            loadFolder(curFolderData[FileContract.FileEntry.COLUMN_NAME_PREV].toString())
        }

        btnMoveDrop.setOnClickListener {
            if(previousPos != curFolderData[FileContract.FileEntry.COLUMN_NAME_ID]) {
                updateFileData(
                    FileContract.FileEntry.COLUMN_NAME_HAVING_FILES,
                    moveFile,
                    previousPos,
                    true
                )
                updateFileData(FileContract.FileEntry.COLUMN_NAME_PREV, curFolderData[FileContract.FileEntry.COLUMN_NAME_ID].toString(), moveFile)
                updateCurFolderData(FileContract.FileEntry.COLUMN_NAME_HAVING_FILES, moveFile)
                loadFolder(curFolderData[FileContract.FileEntry.COLUMN_NAME_ID].toString())
            }
            layoutMove.visibility = View.GONE
        }

        btnMoveCancel.setOnClickListener {
            layoutMove.visibility = View.GONE
        }

        editSearch.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(edit: Editable?) {
                var backupList: MutableList<ScoreFileData> = mutableListOf()
                loadFolder(curFolderData[FileContract.FileEntry.COLUMN_NAME_ID].toString())
                data.forEach {
                    if(it.name.contains(edit.toString())){
                        backupList.add(it)
                    }
                }

                data.clear()
                data.addAll(backupList)

                applyRecycler()
            }
        })

        this.fileAdapter.setOnItemClickListener(object: CustomItemAdapter.OnItemClickListener{
            override fun onClick(v: View, fileData: ScoreFileData, position: Int) {
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

        this.fileAdapter.setOnItemLongClickListener(object: CustomItemAdapter.OnItemLongClickListener{
            override fun onLongClick(v: View, fileData: ScoreFileData, position: Int) {
                val popupMenu = PopupMenu(this@MainActivity, v, Gravity.END)
                menuInflater.inflate(R.menu.listview_menu, popupMenu.menu)
                popupMenu.setOnMenuItemClickListener ( object: PopupMenu.OnMenuItemClickListener {
                    override fun onMenuItemClick(selMenu: MenuItem?): Boolean {
                        when(selMenu!!.itemId){
                            R.id.menu_rename->{
                                popupRenameMenu(fileData, position)
                                layoutMove.visibility = View.GONE
                                return true
                            }
                            R.id.menu_delete->{
                                popupDeleteMenu(fileData)
                                layoutMove.visibility = View.GONE
                                return true
                            }
                            R.id.menu_move->{
                                textMoveFile.text = fileData.name
                                moveFile = fileData.id
                                previousPos = curFolderData[FileContract.FileEntry.COLUMN_NAME_ID].toString()
                                layoutMove.visibility = View.VISIBLE
                            }
                        }
                        return false
                    }
                })
                popupMenu.show()
            }
        })

    }

    private fun popupDeleteMenu(fileData: ScoreFileData){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete")
        builder.setMessage("Really?")

        builder.setPositiveButton("yes") { _, _ ->
            data.remove(fileData)

            updateCurFolderData(FileContract.FileEntry.COLUMN_NAME_HAVING_FILES, fileData.id, true)
            applyRecycler()

            if (!fileData.isDirectory) {
                val db = ScoreContract.ScoreDBHelper(this).writableDatabase
                val selection = "${ScoreContract.ScoreEntry.COLUMN_NAME_ID} LIKE ?"
                val selectionArgs = arrayOf(fileData.id)
                db.delete(ScoreContract.ScoreEntry.TABLE_NAME, selection, selectionArgs)
                db.close()
            } else {
                clearFolder(fileData.id)
            }

            val dbFile = FileContract.FileDBHelper(this).writableDatabase
            val selectionFile = "${FileContract.FileEntry.COLUMN_NAME_ID} LIKE ?"
            val selectionArgsFile = arrayOf(fileData.id)
            dbFile.delete(FileContract.FileEntry.TABLE_NAME, selectionFile, selectionArgsFile)
            dbFile.close()
        }
        builder.setNegativeButton("no", null)
        builder.show()

    }

    private fun clearFolder(id: String){
        val dbRD = FileContract.FileDBHelper(this).readableDatabase
        val dbWR = FileContract.FileDBHelper(this).writableDatabase

        val selectionRead = "${FileContract.FileEntry.COLUMN_NAME_ID} = ?"

        val selectionFile = "${FileContract.FileEntry.COLUMN_NAME_ID} LIKE ?"

        val projection = arrayOf(FileContract.FileEntry.COLUMN_NAME_HAVING_FILES,
            FileContract.FileEntry.COLUMN_NAME_IS_FOLDER)

        val selectionArgsRead = arrayOf(id)

        val cursor = dbRD.query(
            FileContract.FileEntry.TABLE_NAME,
            projection,
            selectionRead,
            selectionArgsRead,
            null,
            null,
            null
        )

        if(cursor.moveToFirst()){
            val isFolder = cursor.getString(1)

            if(isFolder.toBoolean()){
                val havingFiles = cursor.getString(0).split("|")

                havingFiles.forEach {
                    if(it.isNotEmpty()){
                        clearFolder(it)
                    }
                }

                havingFiles.forEach {
                    val selectionArgsFile = arrayOf(it)
                    dbWR.delete(FileContract.FileEntry.TABLE_NAME, selectionFile, selectionArgsFile)
                }
            }
        }else{
            dbRD.close()
            dbWR.close()

            return
        }

        dbRD.close()
        dbWR.close()
    }

    private fun popupRenameMenu(fileData: ScoreFileData, position: Int){
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.custom_dialog_rename, null)
        
        val textView: TextView = view.findViewById(R.id.dialogRenameText)
        val editText: EditText = view.findViewById(R.id.dialogRenameEdit)
        textView.text = "Enter a new file name"
        editText.setText(fileData.name)
        
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Rename")
            .setPositiveButton("done"){
                    _, _ ->
            }.setNegativeButton("cancel"){
                    _, _ ->
            }.create()
        alertDialog.setView(view)
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if(editText.text.toString() == ""){
                textView.text = "Enter more than one character"
                textView.setTextColor(Color.RED)
            }else{
                updateFileData(FileContract.FileEntry.COLUMN_NAME_FOLDER_NAME, editText.text.toString(), fileData.id)
                data[position] = ScoreFileData(editText.text.toString(), fileData.author, fileData.isDirectory, fileData.img, fileData.id, fileData.prevFolderID)

                //if ScoreFile need update Score DB
                if(!fileData.isDirectory) {
                    val db = ScoreContract.ScoreDBHelper(this).readableDatabase
                    val projection = arrayOf(
                        ScoreContract.ScoreEntry.COLUMN_NAME_TITLE,
                        ScoreContract.ScoreEntry.COLUMN_NAME_LINES,
                        ScoreContract.ScoreEntry.COLUMN_NAME_CHORDS,
                        ScoreContract.ScoreEntry.COLUMN_NAME_LYRICS
                    )

                    val selection = "${ScoreContract.ScoreEntry.COLUMN_NAME_ID} = ?"
                    val selectionArgs = arrayOf(fileData.id)

                    val cursor = db.query(
                        ScoreContract.ScoreEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        null
                    )

                    var lines = ""
                    var chords = ""
                    var lyrics = ""

                    if (cursor.moveToFirst()) {
                        lines = cursor.getString(1)
                        chords = cursor.getString(2)
                        lyrics = cursor.getString(3)
                    }

                    db.close()

                    val dbWR = ScoreContract.ScoreDBHelper(this).writableDatabase
                    var values = ContentValues().apply {
                        put(ScoreContract.ScoreEntry.COLUMN_NAME_ID, fileData.id)
                        put(ScoreContract.ScoreEntry.COLUMN_NAME_TITLE, editText.text.toString())
                        put(ScoreContract.ScoreEntry.COLUMN_NAME_LINES, lines)
                        put(ScoreContract.ScoreEntry.COLUMN_NAME_CHORDS, chords)
                        put(ScoreContract.ScoreEntry.COLUMN_NAME_LYRICS, lyrics)
                    }

                    dbWR?.update(
                        ScoreContract.ScoreEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs
                    )

                    dbWR.close()
                }

                applyRecycler()
                alertDialog.dismiss()
            }
        }

        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
            alertDialog.dismiss()
        }

    }

    private fun updateCurFolderData(updateKey: String, updateValue: String, subtract: Boolean = false){
        if(updateKey == FileContract.FileEntry.COLUMN_NAME_HAVING_FILES){
            if(subtract){
                curFolderData[updateKey] = curFolderData[updateKey]?.replace("$updateValue|", "").toString()
            }else{
                curFolderData[updateKey] += ("$updateValue|")
            }

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

        db?.update(
            FileContract.FileEntry.TABLE_NAME,
            values,
            selection,
            selectionArgs
        )
    }

    private fun updateFileData(updateKey: String, updateValue: String, id: String, subtract: Boolean = false){
        //read data
        fileDBHelper = FileContract.FileDBHelper(this)
        val dbRead = fileDBHelper.readableDatabase

        val projection = arrayOf(
            FileContract.FileEntry.COLUMN_NAME_IS_FOLDER,
            FileContract.FileEntry.COLUMN_NAME_FOLDER_NAME,
            FileContract.FileEntry.COLUMN_NAME_HAVING_FILES,
            FileContract.FileEntry.COLUMN_NAME_PREV)

        val selectionRead = "${FileContract.FileEntry.COLUMN_NAME_ID} = ?"
        val selectionArgsRead = arrayOf(id)

        val cursor = dbRead.query(
            FileContract.FileEntry.TABLE_NAME,
            projection,
            selectionRead,
            selectionArgsRead,
            null,
            null,
            null
        )

        var isFolder: String
        var name: String
        var having: String
        var prev: String

        if(cursor.moveToFirst()) {

            isFolder = cursor.getString(0)
            name = cursor.getString(1)
            having = cursor.getString(2)
            prev = cursor.getString(3)

            dbRead.close()
        }else {
            dbRead.close()
            return
        }

        //write(update) data

        when(updateKey){
            FileContract.FileEntry.COLUMN_NAME_HAVING_FILES -> {
                if(subtract){
                    having = having.replace("$updateValue|", "")
                    Log.d("MOVETEST", having)
                }else{
                    having += "$updateValue|"
                }
            }
            FileContract.FileEntry.COLUMN_NAME_FOLDER_NAME ->
                name = updateValue
            FileContract.FileEntry.COLUMN_NAME_IS_FOLDER ->
                isFolder = updateValue
            FileContract.FileEntry.COLUMN_NAME_PREV ->
                prev = updateValue
        }


        fileDBHelper = FileContract.FileDBHelper(this)
        val db = fileDBHelper.writableDatabase

        var values = ContentValues().apply {
            put(FileContract.FileEntry.COLUMN_NAME_ID, id)
            put(FileContract.FileEntry.COLUMN_NAME_IS_FOLDER, isFolder)
            put(FileContract.FileEntry.COLUMN_NAME_HAVING_FILES, having)
            put(FileContract.FileEntry.COLUMN_NAME_FOLDER_NAME, name)
            put(FileContract.FileEntry.COLUMN_NAME_PREV, prev)
        }

        val selection = "${FileContract.FileEntry.COLUMN_NAME_ID} LIKE ?"
        val selectionArgs = arrayOf(id)

        db?.update(
            FileContract.FileEntry.TABLE_NAME,
            values,
            selection,
            selectionArgs
        )
        db.close()
    }

    private fun loadFolder(id: String): Boolean{
        data.clear()

        btnBack.visibility = if(id == "MAIN") View.GONE else View.VISIBLE
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
        db?.insert(FileContract.FileEntry.TABLE_NAME, null, values)

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
        db?.insert(FileContract.FileEntry.TABLE_NAME, null, values)

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