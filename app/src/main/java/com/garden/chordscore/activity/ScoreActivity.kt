package com.garden.chordscore.activity

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.BaseColumns
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.Dimension
import androidx.appcompat.app.AppCompatActivity
import com.garden.chordscore.R
import com.garden.chordscore.database.FileContract
import com.garden.chordscore.database.ScoreContract
import kotlin.math.abs
import kotlin.math.roundToInt

class ScoreActivity : AppCompatActivity() {
    private lateinit var dbHelper: ScoreContract.ScoreDBHelper
    private lateinit var linearParent: LinearLayout
    private lateinit var editTitle: EditText

    private var lineList: MutableList<LinearLayout> = mutableListOf()
    private var editList: MutableList<EditText> = mutableListOf()
    private var btnList: MutableList<Button> = mutableListOf()
    private var chordListMap: MutableMap<LinearLayout, MutableList<AutoCompleteTextView>> = mutableMapOf()

    private var ID: String = ""

    private var prevFolderID = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_score)

        dbHelper = ScoreContract.ScoreDBHelper(this)

        val btnBack: ImageButton = findViewById(R.id.btn_back)
        val btnNewLine: ImageButton = findViewById(R.id.btn_new_line)

        editTitle = findViewById(R.id.edit_name)
        linearParent = findViewById(R.id.linear_parent)

        if(intent.hasExtra("id")){
            ID = intent.getStringExtra("id").toString()
            loadDB(editTitle)
        }else {
            makeLine(linearParent)
        }

        prevFolderID = if(intent.hasExtra("prev")){
            intent.getStringExtra("prev").toString()
        }else{
            "MAIN"
        }

        btnBack.setOnClickListener{ goBack() }
        btnNewLine.setOnClickListener { makeLine(parentView = linearParent) }
    }

    private fun saveDB(){
        val db = dbHelper.writableDatabase

        var title: String = editTitle.text.toString()
        var lines: Int = lineList.size
        var chords: String = ""
        var lineCount = 0
        for ((key, value) in chordListMap){
            for(textView in value){
                chords += (lineCount.toString() + "|" + textView.text.toString() + "|" + textView.width.toString() + "$")
            }
            lineCount++
        }
        var lyrics: String = ""
        for(lyric in editList){
            lyrics += (lyric.text.toString() + "|")
        }

        var values = ContentValues().apply {
            put(ScoreContract.ScoreEntry.COLUMN_NAME_ID, (title + chords + lyrics).hashCode().toString())
            put(ScoreContract.ScoreEntry.COLUMN_NAME_TITLE, title)
            put(ScoreContract.ScoreEntry.COLUMN_NAME_LINES, lines)
            put(ScoreContract.ScoreEntry.COLUMN_NAME_CHORDS, chords)
            put(ScoreContract.ScoreEntry.COLUMN_NAME_LYRICS, lyrics)
        }

        if(ID.isEmpty()){
            val newRowId = db?.insert(ScoreContract.ScoreEntry.TABLE_NAME, null, values)
        }else{
            val selection = "${ScoreContract.ScoreEntry.COLUMN_NAME_ID} LIKE ?"
            val selectionArgs = arrayOf(ID)
            values.remove(ScoreContract.ScoreEntry.COLUMN_NAME_ID)
            values.put(ScoreContract.ScoreEntry.COLUMN_NAME_ID, ID)
            val count = db?.update(
                ScoreContract.ScoreEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs
            )
        }

        dbHelper.close()
    }

    private fun loadDB(editTitle: EditText){
        dbHelper = ScoreContract.ScoreDBHelper(this)
        val db = dbHelper.readableDatabase

        val projection = arrayOf(BaseColumns._ID, ScoreContract.ScoreEntry.COLUMN_NAME_ID,
            ScoreContract.ScoreEntry.COLUMN_NAME_TITLE,
            ScoreContract.ScoreEntry.COLUMN_NAME_LINES,
            ScoreContract.ScoreEntry.COLUMN_NAME_CHORDS,
            ScoreContract.ScoreEntry.COLUMN_NAME_LYRICS)

        val selection = "${ScoreContract.ScoreEntry.COLUMN_NAME_ID} = ?"
        val selectionArgs = arrayOf(ID)

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
            editTitle.setText(cursor.getString(2))

            for(index in 0 until cursor.getInt(3)){
                makeLine(parentView = linearParent)
            }

            for(chordData in cursor.getString(4).split("$")){
                val chordDataSplit = chordData.split("|")
                if(chordDataSplit.size > 1) {
                    makeChord(lineList[chordDataSplit[0].toInt()], myChord = chordDataSplit[1], myWidth = chordDataSplit[2].toInt())
                }
            }

            val allLyrics = cursor.getString(5).split("|")
            for(index in 0 until allLyrics.lastIndex){
                editList[index]?.setText(allLyrics[index])
            }
        }

        db.close()
    }

    private fun removeDB(id: String){
        val db = dbHelper.writableDatabase
        val selection = "${ScoreContract.ScoreEntry.COLUMN_NAME_ID} LIKE ?"
        val selectionArgs = arrayOf(id)
        val deletedRows = db.delete(ScoreContract.ScoreEntry.TABLE_NAME, selection, selectionArgs)
        db.close()

        val dbRead = FileContract.FileDBHelper(this).readableDatabase

        val projection = arrayOf(
            FileContract.FileEntry.COLUMN_NAME_IS_FOLDER,
            FileContract.FileEntry.COLUMN_NAME_FOLDER_NAME,
            FileContract.FileEntry.COLUMN_NAME_HAVING_FILES,
            FileContract.FileEntry.COLUMN_NAME_PREV)

        val selectionRead = "${FileContract.FileEntry.COLUMN_NAME_ID} = ?"
        val selectionArgsRead = arrayOf(prevFolderID)

        val cursor = dbRead.query(
            FileContract.FileEntry.TABLE_NAME,
            projection,
            selectionRead,
            selectionArgsRead,
            null,
            null,
            null
        )

        var isFolder = ""
        var name = ""
        var having = ""
        var prev = ""

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

        having.replace("$id|", "")

        var values = ContentValues().apply {
            put(FileContract.FileEntry.COLUMN_NAME_ID, prevFolderID)
            put(FileContract.FileEntry.COLUMN_NAME_IS_FOLDER, isFolder)
            put(FileContract.FileEntry.COLUMN_NAME_HAVING_FILES, having)
            put(FileContract.FileEntry.COLUMN_NAME_FOLDER_NAME, name)
            put(FileContract.FileEntry.COLUMN_NAME_PREV, prev)
        }

        val dbFile = FileContract.FileDBHelper(this).writableDatabase

        val count = dbFile?.update(
            FileContract.FileEntry.TABLE_NAME,
            values,
            selection,
            selectionArgs
        )

        val selectionFile = "${FileContract.FileEntry.COLUMN_NAME_ID} LIKE ?"
        val selectionArgsFile = arrayOf(id)
        val deleteRowsFile = dbFile.delete(FileContract.FileEntry.TABLE_NAME, selectionFile, selectionArgsFile)
        dbFile.close()

        goBack()
    }

    private fun makeLine(parentView: LinearLayout){
        val dynamicLine = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = getDrawable(R.color.white)

            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams = lp
        }

        val dynamicEditText = EditText(this).apply {
            height = getDP(40)
            setTextColor(Color.BLACK)
            setTextSize(Dimension.DP, getDP(15).toFloat())
            gravity = Gravity.CENTER_VERTICAL

            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams = lp
        }

        chordListMap[dynamicLine] = mutableListOf()
        lineList.add(dynamicLine)
        editList.add(dynamicEditText)

        parentView.addView(dynamicLine)
        parentView.addView(dynamicEditText)

        makeAddChordButton(dynamicLine)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun makeChord(linearView: LinearLayout, myChord: String = "", myWidth: Int = getDP(40)){
        val dynamicTextView = AutoCompleteTextView(this).apply {
            width = myWidth
            height = getDP(40)
            background = getDrawable(R.color.black)
            setTextColor(Color.WHITE)
            setTextSize(Dimension.DP, getDP(15).toFloat())
            gravity = Gravity.CENTER

            threshold = 1

            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END

            setText(myChord)


            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(5, 5, 5, 5);
            layoutParams = lp
        }

        val chords = resources.getStringArray(R.array.Chords)
        dynamicTextView.setAdapter(ArrayAdapter(this, R.layout.custom_dropdown_item, chords))

        chordListMap[linearView]?.add(dynamicTextView)

        var moveX: Float = 0.0F
        dynamicTextView.setOnTouchListener{ _, event ->
            fun removeHideChord(index: Int, list: MutableList<AutoCompleteTextView>){
                while(index != -1 && index < list.size-1){
                    var removeTextView = list.last()
                    linearView.removeView(removeTextView)
                    list.remove(removeTextView)
                }
            }

            fun checkAndFixOver(textView: AutoCompleteTextView): Boolean{
                if(linearView.right - textView.right < getDP(60)){
                    textView.width += (linearView.right - textView.right)

                    var list = chordListMap[linearView]
                    var index: Int = list!!.indexOf(textView)

                    removeHideChord(index, list)
                    return true
                }
                return false
            }

            when(event.action){
                MotionEvent.ACTION_DOWN -> {
                    moveX = event.rawX
                }
                MotionEvent.ACTION_MOVE -> {
                    //dynamicTextView.width += ((event.rawX - moveX) * 0.05).toInt()
                    dynamicTextView.width = event.rawX.toInt() - dynamicTextView.left
                }
                MotionEvent.ACTION_UP -> {
                    if(dynamicTextView.width < getDP(60)){
                        dynamicTextView.width = getDP(60)
                    }
                    var list = chordListMap[linearView]
                    var index = list!!.indexOf(dynamicTextView)
                    while(index != list.size){
                        if(checkAndFixOver(list[index++])) break
                    }

                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

                    if(abs(moveX-event.rawX) < 5){
                        dynamicTextView.requestFocus()
                        imm.showSoftInput(dynamicTextView, 0)
                    }
                    else {
                        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
                        currentFocus?.clearFocus()
                    }
                }
            }

            true
        }

        linearView.addView(dynamicTextView)
        makeAddChordButton(linearView)

    }

    private fun makeAddChordButton(linearView: LinearLayout){
        var dynamicAddChordButton = Button(this).apply {
            background = getDrawable(R.drawable.ic_add_box)

            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(5, 5, 5, 5);
            lp.width= getDP(40)
            lp.height = getDP(40)
            layoutParams = lp
        }

        dynamicAddChordButton.setOnClickListener {
            makeChord(linearView)
        }

        //원래는 다이나믹 버튼 인스턴스를 저장하는 변수 하나로 중복을 검사했는데
        //이제 라인마다 버튼이 존재해야 하기 때문에
        //각 라인마다 같은 인덱스를 가지는 버튼 리스트를 만들어
        //라인별로 버튼의 삭제 및 추가를 담당

        if(lineList.size > btnList.size){
            btnList.add(dynamicAddChordButton)
        }else{
            var index: Int = lineList.indexOf(linearView)
            linearView.removeView(btnList[index])
            btnList[index] = dynamicAddChordButton
        }

        linearView.addView(dynamicAddChordButton)
    }

    override fun onBackPressed() = goBack()

    private fun goBack(){
        saveDB()

        val intent: Intent = Intent(this, MainActivity::class.java)
        intent.putExtra("id", prevFolderID)
        startActivity(intent)
    }

    private fun getDP(value : Int) : Int = (value * resources.displayMetrics.density).roundToInt()
}