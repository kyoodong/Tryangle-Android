package com.gomson.tryangle.album

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.gomson.tryangle.R
import kotlinx.android.synthetic.main.item_guide_image.view.*

class AlbumAdapter(private val context: Context) :
    RecyclerView.Adapter<AlbumAdapter.ViewHolder>() {

    private val items = arrayListOf<DeviceAlbum>()

    public fun clear(){
        items.clear()
    }

    public fun addAll(list:MutableList<DeviceAlbum>){
        items.addAll(list)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            ViewHolder {
        val view = LayoutInflater.from(context).inflate(
            R.layout.item_album,
            parent, false
        )
        return ViewHolder(view)
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val imageView = itemView.findViewById<ImageView>(R.id.photo)

        fun bind(item: DeviceAlbum) {
            Glide.with(itemView)
                .load(item.contentUri)
                .into(imageView)
        }
    }
}

