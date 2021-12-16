package com.garden.codescore.activity

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import com.garden.codescore.R
import com.garden.codescore.customrecyclerview.*


class MainActivity : AppCompatActivity() {
    private lateinit var profileAdapter: CustomItemAdapter
    private val data = mutableListOf<ScoreFileData>()
    private lateinit var listview: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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