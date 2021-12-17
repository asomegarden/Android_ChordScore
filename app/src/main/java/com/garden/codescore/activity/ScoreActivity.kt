package com.garden.codescore.activity

import android.content.Intent
import android.graphics.Color
import android.media.Image
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.Dimension
import androidx.appcompat.app.AppCompatActivity
import com.garden.codescore.R
import kotlin.math.roundToInt

class ScoreActivity : AppCompatActivity() {
    lateinit var dynamicAddChordButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_score)

        var linear: LinearLayout = findViewById(R.id.linear_line1)
        var btnBack: ImageButton = findViewById(R.id.btn_back)

        makeChord(chord = "A", linear)

        btnBack.setOnClickListener {
            val intent: Intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun makeChord(chord: String, linearview: LinearLayout){
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
                    if(linearview.right - dynamicTextView.right < getDP(40)){
                        dynamicTextView.width += (linearview.right - dynamicTextView.right)
                    }
                }
            }

            true
        }

        linearview.addView(dynamicTextView)
        if(::dynamicAddChordButton.isInitialized) linearview.removeView(dynamicAddChordButton)
        makeAddChordButton(linearview)
    }

    private fun makeAddChordButton(linearview: LinearLayout){
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
        var moveX: Float = 0f

        dynamicAddChordButton.setOnClickListener {
            makeChord("E", linearview)
        }

        linearview.addView(dynamicAddChordButton)
    }

    private fun getDP(value : Int) : Int = (value * resources.displayMetrics.density).roundToInt()
}