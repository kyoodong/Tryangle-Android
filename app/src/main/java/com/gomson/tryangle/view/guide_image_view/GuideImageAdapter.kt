package com.gomson.tryangle.view.guide_image_view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.gomson.tryangle.R
import com.gomson.tryangle.network.NetworkManager
import kotlinx.android.synthetic.main.item_guide_image.view.*

class GuideImageAdapter(val context: Context)
    : RecyclerView.Adapter<GuideImageAdapter.ViewHolder>() {

    private var guideImageUrlList: List<String> = ArrayList()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflator = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = layoutInflator.inflate(R.layout.item_guide_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setItem(guideImageUrlList[position])
    }

    override fun getItemCount(): Int {
        return guideImageUrlList.size
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

        fun setItem(url: String) {
            Glide.with(itemView)
                .load("${NetworkManager.URL}/${url}")
                .into(itemView.imageView)
            itemView.imageView
        }
    }

    fun addImageUrlList(list: List<String>) {
        val count = guideImageUrlList.size
        guideImageUrlList += list
        notifyItemInserted(count)
    }

    fun resetImageUrlList() {
        guideImageUrlList = ArrayList()
        notifyDataSetChanged()
    }
}