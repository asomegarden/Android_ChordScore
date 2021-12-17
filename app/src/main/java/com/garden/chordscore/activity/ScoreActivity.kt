package com.garden.chordscore.activity

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.Dimension
import androidx.appcompat.app.AppCompatActivity
import com.garden.chordscore.R
import kotlin.math.roundToInt

class ScoreActivity : AppCompatActivity() {
    lateinit var dynamicAddChordButton: Button
    var lineList: MutableList<View> = mutableListOf()
    var editList: MutableList<EditText> = mutableListOf()
    var btnList: MutableList<Button> = mutableListOf()

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

        lineList.add(dynamicLine)
        editList.add(dynamicEditText)

        parentView.addView(dynamicLine)
        parentView.addView(dynamicEditText)

        makeAddChordButton(dynamicLine)
    }

    private fun makeChord(chord: String, linearView: LinearLayout){
        val dynamicTextView = TextView(this).apply {
            width = getDP(40)
            height = getDP(40)
            background = getDrawable(R.color.black)
            setTextColor(Color.WHITE)
            setTextSize(Dimension.DP, getDP(15).toFloat())
            gravity = Gravity.CENTER

            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(5, 5, 5, 5);
            layoutParams = lp
            text = chord
        }
        var moveX: Float = 0f

        //리스트에 라인별로 버튼을 넣어서 관리하자. 만약 끝났을 때 리스트의 마지막 요소를 검사해서 right의 상태에 따라 조절해주게 조작할 필요가 있음
        //그리고 버튼은 각 라인별로 딱 하나 유일하게 존재하게 해주어야함


        dynamicTextView.setOnTouchListener{view ,event ->
            when(event.action){
                MotionEvent.ACTION_DOWN -> {
                    moveX = event.rawX
                }
                MotionEvent.ACTION_MOVE -> {
                    dynamicTextView.width += ((event.rawX - moveX) * 0.05).toInt()

                }
                MotionEvent.ACTION_UP -> {
                    if(dynamicTextView.width < getDP(40)){
                        dynamicTextView.width = getDP(40)
                    }
                    if(linearView.right - dynamicTextView.right < getDP(40)){
                        dynamicTextView.width += (linearView.right - dynamicTextView.right)
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
            makeChord("E", linearView)
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