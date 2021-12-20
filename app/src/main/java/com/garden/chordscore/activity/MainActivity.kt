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


class MainActivity : AppCompatActivity() {
    private lateinit var scoreDBHelper: ScoreContract.ScoreDBHelper
    private lateinit var fileDBHelper: FileContract.FileDBHelper

    private lateinit var profileAdapter: CustomItemAdapter
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
            put(FileContract.FileEntry.COLUMN_NAME_FILE_ID, "")
        }

        val btnCreateFolder: ImageButton = findViewById(R.id.btn_create_folder)
        val btnCreateNote: ImageButton = findViewById(R.id.btn_create_note)

        folderName = findViewById(R.id.text_current_locate)
        listview = findViewById(R.id.listview)

        initMainFolder()
        //현재 CreateFolder 메소드가 폴더를 생성하는데 첫 실행 때 메인 폴더를 생성할 경우에는 
        // 현재 폴더를 바꾸는 거지만 평소에는 현재 폴더에 하위 폴더를 생성함 그래서 구분이 필요함 수정 필요
        
        // 구분 완료
        
        
        //이제 뒤로가기 누르면 이전 폴더로 가야됨. 그래서 파일 정보를 저장할 때 상위 폴더의 아이디를 저장할 필요가 있음
        //또 이제 악보 파일에서 이름을 수정하면 목록 파일에서도 다른 이름으로 보여져야함. 그래서 아예 파일의 경우에는 이름을 
        //이름과 작곡가 데이터등을 직접 불러와서 쓰는걸로 하는게 어떨까 싶음
        //name 필드는 폴더만 사용하는 걸로 그리고 악보 데이터베이스와 파일 데이터베이스는 완전 다르므로 아이디를 공유해도 될듯함
        //그래서 isFolder가 false이면 그 아이디를 그대로 사용
        //그래서 FileID 필드는 삭제해도 될듯 지금 파일 데이터베이스가 조금 모호한 필드 구분이라 이렇게 수정하면 좀 괜찮아질듯
        //그리고 중복되는 이름의 경우에는 뒤에 1을 붙이는 식으로 구분할 필요가 있음 (1)을 붙이면 될듯
        //그러려면 파일 이름을 모두 리스트에 저장해두고 중복 개수에 따라 처리할 필요가 있음


        btnCreateFolder.setOnClickListener{
            var id = System.currentTimeMillis().hashCode().toString()
            createFile(id, "new folder")
            updateCurFolderData(FileContract.FileEntry.COLUMN_NAME_HAVING_FILES, id)
        }

        btnCreateNote.setOnClickListener{
            val intent: Intent = Intent(this, ScoreActivity::class.java)

            val id = System.currentTimeMillis().toString()
            //create_new_note
            scoreDBHelper = ScoreContract.ScoreDBHelper(this)
            val db = scoreDBHelper.writableDatabase
            var values = ContentValues().apply {
                put(ScoreContract.ScoreEntry.COLUMN_NAME_ID, id)
                put(ScoreContract.ScoreEntry.COLUMN_NAME_TITLE, "new score")
                put(ScoreContract.ScoreEntry.COLUMN_NAME_LINES, 1)
                put(ScoreContract.ScoreEntry.COLUMN_NAME_CHORDS, "")
                put(ScoreContract.ScoreEntry.COLUMN_NAME_LYRICS, "")
            }
            val newRowId = db?.insert(ScoreContract.ScoreEntry.TABLE_NAME, null, values)
            scoreDBHelper.close()
            
            //이때 Score 데이터 생성과 동시에 File 데이터도 생성해서 폴더에 따로 포함시켜줘야함
            //일단 목이 아파서 오늘은 여기까지

            createFile(id = id, name = "new score", isFolder = false, fileID = id)
            updateCurFolderData(FileContract.FileEntry.COLUMN_NAME_HAVING_FILES, id)

            intent.putExtra("id", id)
            startActivity(intent)
        }

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
            put(FileContract.FileEntry.COLUMN_NAME_FILE_ID, curFolderData[FileContract.FileEntry.COLUMN_NAME_FILE_ID])
            put(FileContract.FileEntry.COLUMN_NAME_HAVING_FILES, curFolderData[FileContract.FileEntry.COLUMN_NAME_HAVING_FILES])
            put(FileContract.FileEntry.COLUMN_NAME_FOLDER_NAME, curFolderData[FileContract.FileEntry.COLUMN_NAME_FOLDER_NAME])
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
            FileContract.FileEntry.COLUMN_NAME_FILE_ID)

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
            curFolderData[FileContract.FileEntry.COLUMN_NAME_FILE_ID] = cursor.getString(4)

            val fileIDList: MutableList<String> = mutableListOf<String>()
            folderName.text = cursor.getString(2)

            for(fileData in cursor.getString(3).split("|")){
                if(fileData.length > 1) fileIDList.add(fileData)
            }

            loadFileFromID(fileIDList)

            applyRecycler()
        }else return false

        fileDBHelper.close()
        return true
    }

    private fun createFile(id: String, name: String = "Home", fileID: String = "", isFolder: Boolean = true){
        fileDBHelper = FileContract.FileDBHelper(this)
        val db = fileDBHelper.writableDatabase
        var values = ContentValues().apply {
            put(FileContract.FileEntry.COLUMN_NAME_FOLDER_NAME, name)
            put(FileContract.FileEntry.COLUMN_NAME_FILE_ID, fileID)
            put(FileContract.FileEntry.COLUMN_NAME_HAVING_FILES, "")
            put(FileContract.FileEntry.COLUMN_NAME_ID, id)
            put(FileContract.FileEntry.COLUMN_NAME_IS_FOLDER, isFolder.toString())
        }
        val newRowId = db?.insert(FileContract.FileEntry.TABLE_NAME, null, values)

        data.add(ScoreFileData(img = if(isFolder)R.drawable.ic_folder else R.drawable.ic_file, name = name, isDirectory = true, id = id, author = ""))

        applyRecycler()
        fileDBHelper.close()
    }

    private fun createFolderCurrent(id: String, name: String = "Home"){
        fileDBHelper = FileContract.FileDBHelper(this)
        val db = fileDBHelper.writableDatabase

        var values = ContentValues().apply {
            put(FileContract.FileEntry.COLUMN_NAME_FOLDER_NAME, name)
            put(FileContract.FileEntry.COLUMN_NAME_FILE_ID, "")
            put(FileContract.FileEntry.COLUMN_NAME_HAVING_FILES, "")
            put(FileContract.FileEntry.COLUMN_NAME_ID, id)
            put(FileContract.FileEntry.COLUMN_NAME_IS_FOLDER, true.toString())
        }
        val newRowId = db?.insert(FileContract.FileEntry.TABLE_NAME, null, values)

        curFolderData[FileContract.FileEntry.COLUMN_NAME_ID] = id
        curFolderData[FileContract.FileEntry.COLUMN_NAME_IS_FOLDER] = true.toString()
        curFolderData[FileContract.FileEntry.COLUMN_NAME_FOLDER_NAME] = name
        curFolderData[FileContract.FileEntry.COLUMN_NAME_HAVING_FILES] = ""
        curFolderData[FileContract.FileEntry.COLUMN_NAME_FILE_ID] = ""

        folderName.text = name
        fileDBHelper.close()
    }

    private fun initMainFolder(){
        if(!loadFolder("MAIN")){
            createFolderCurrent("MAIN")
        }
    }

    private fun loadFileFromID(idList: MutableList<String>){
        fileDBHelper = FileContract.FileDBHelper(this)
        val db = fileDBHelper.readableDatabase

        val projection = arrayOf(
            FileContract.FileEntry.COLUMN_NAME_ID,
            FileContract.FileEntry.COLUMN_NAME_IS_FOLDER,
            FileContract.FileEntry.COLUMN_NAME_FOLDER_NAME,
            FileContract.FileEntry.COLUMN_NAME_FILE_ID)

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
                    data.add(ScoreFileData(img = R.drawable.ic_folder, name = cursor.getString(2), isDirectory = true, id = cursor.getString(0), author = ""))
                }else{
                    loadScoreFromID(cursor.getString(0))
                }
            }
        }

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
            data.add(ScoreFileData(img = R.drawable.ic_file, name = cursor.getString(0), isDirectory = false, id = id, author = cursor.getString(0)))
        }
        scoreDBHelper.close()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun applyRecycler() {
        profileAdapter = CustomItemAdapter(this)
        listview.adapter = profileAdapter

        //rv_profile.addItemDecoration(VerticalItemDecorator(20))
        //rv_profile.addItemDecoration(HorizontalItemDecorator(10))

        profileAdapter.data = data
        profileAdapter.notifyDataSetChanged()
    }
}