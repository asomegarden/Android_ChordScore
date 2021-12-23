package com.garden.chordscore.customrecyclerview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.garden.chordscore.R

class CustomItemAdapter(private val context: Context) : RecyclerView.Adapter<CustomItemAdapter.ViewHolder>() {

    var data = mutableListOf<ScoreFileData>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.custom_recycler_item,parent,false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    interface OnItemClickListener{
        fun onItemClick(v:View, data:ScoreFileData, position: Int)
    }
    private var listener: OnItemClickListener? = null
    fun setOnItemClickListener(listener: OnItemClickListener){
        this.listener = listener
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val textName: TextView = itemView.findViewById(R.id.text_name)
        private val textAuthor: TextView = itemView.findViewById(R.id.text_author)
        private val imgIcon: ImageView = itemView.findViewById(R.id.image_icon)

        fun bind(item: ScoreFileData) {
            textName.text = item.name
            textAuthor.text = if(item.isDirectory) "" else item.author
            Glide.with(itemView).load(item.img).into(imgIcon)

            val pos = adapterPosition
            if(pos!= RecyclerView.NO_POSITION){
                itemView.setOnClickListener{
                    listener?.onItemClick(itemView, item, pos)
                }
            }

        }
    }


}