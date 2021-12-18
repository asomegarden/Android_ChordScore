package com.garden.chordscore.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.Dimension
import androidx.appcompat.app.AppCompatActivity
import com.garden.chordscore.R
import kotlin.math.abs
import kotlin.math.roundToInt

class ScoreActivity : AppCompatActivity() {
    lateinit var dynamicAddChordButton: Button
    var lineList: MutableList<LinearLayout> = mutableListOf()
    var editList: MutableList<EditText> = mutableListOf()
    var btnList: MutableList<Button> = mutableListOf()
    var chordListMap: MutableMap<LinearLayout, MutableList<AutoCompleteTextView>> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_score)

        val btnBack: ImageButton = findViewById(R.id.btn_back)
        val btnNewLine: ImageButton = findViewById(R.id.btn_new_line)
        val linearParent: LinearLayout = findViewById(R.id.linear_parent)

        makeLine(linearParent)

        btnBack.setOnClickListener {
            val intent: Intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        btnNewLine.setOnClickListener {
            makeLine(parentView = linearParent)
        }
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
    private fun makeChord(linearView: LinearLayout){
        val dynamicTextView = AutoCompleteTextView(this).apply {
            width = getDP(60)
            height = getDP(40)
            background = getDrawable(R.color.black)
            setTextColor(Color.WHITE)
            setTextSize(Dimension.DP, getDP(15).toFloat())
            gravity = Gravity.CENTER

            threshold = 1

            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END


            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(5, 5, 5, 5);
            layoutParams = lp
            //text = chord
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
        if(::dynamicAddChordButton.isInitialized) linearView.removeView(dynamicAddChordButton)
        makeAddChordButton(linearView)

    }

    private fun makeAddChordButton(linearView: LinearLayout){
        dynamicAddChordButton = Button(this).apply {
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

        if(lineList.size > btnList.size){
            btnList.add(dynamicAddChordButton)
        }else{
            var index: Int = lineList.indexOf(linearView)
            linearView.removeView(btnList[index])
            btnList[index] = dynamicAddChordButton
        }

        linearView.addView(dynamicAddChordButton)
    }

    private fun getDP(value : Int) : Int = (value * resources.displayMetrics.density).roundToInt()
}