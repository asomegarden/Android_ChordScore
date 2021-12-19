package com.garden.chordscore.activity

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.garden.chordscore.customrecyclerview.CustomItemAdapter
import com.garden.chordscore.customrecyclerview.ScoreFileData
import com.garden.chordscore.R


class MainActivity : AppCompatActivity() {
    private lateinit var profileAdapter: CustomItemAdapter
    private val data = mutableListOf<ScoreFileData>()
    private lateinit var listview: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnCreateNote: ImageButton = findViewById(R.id.btn_create_note)
        btnCreateNote.setOnClickListener{
            val intent: Intent = Intent(this, ScoreActivity::class.java)
            intent.putExtra("id", "2091687684")
            startActivity(intent)
        }

        listview = findViewById(R.id.listview)
        initRecycler()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initRecycler() {
        profileAdapter = CustomItemAdapter(this)
        listview.adapter = profileAdapter

        //rv_profile.addItemDecoration(VerticalItemDecorator(20))
        //rv_profile.addItemDecoration(HorizontalItemDecorator(10))

        data.apply {
            add(ScoreFileData(img = R.drawable.ic_folder, name = "아이유", author = "장범준", isDirectory = true))
            add(ScoreFileData(img = R.drawable.ic_folder, name = "버스커버스커", author = "장범준", isDirectory = true))
            add(ScoreFileData(img = R.drawable.ic_folder, name = "자작곡", author = "장범준", isDirectory = true))


            add(ScoreFileData(img = R.drawable.ic_file, name = "골목길 어귀에서", author = "장범준", isDirectory = false))
            add(ScoreFileData(img = R.drawable.ic_file, name = "정말로 사랑한다면", author = "장범준", isDirectory = false))
            add(ScoreFileData(img = R.drawable.ic_file, name = "첫사랑", author = "장범준", isDirectory = false))
            add(ScoreFileData(img = R.drawable.ic_file, name = "사말어사", author = "장범준", isDirectory = false))
            add(ScoreFileData(img = R.drawable.ic_file, name = "아름다운 나이", author = "장범준", isDirectory = false))
            add(ScoreFileData(img = R.drawable.ic_file, name = "잘할걸", author = "장범준", isDirectory = false))

            profileAdapter.data = data
            profileAdapter.notifyDataSetChanged()

        }
    }
}