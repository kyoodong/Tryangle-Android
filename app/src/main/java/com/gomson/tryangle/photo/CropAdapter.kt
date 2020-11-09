package com.gomson.tryangle.photo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.gomson.tryangle.OnItemClickListener
import com.gomson.tryangle.R

class CropAdapter(private val context: Context, private val items: Array<CropRatio>) :
    RecyclerView.Adapter<CropAdapter.ViewHolder>() {

    val whiteColor = ContextCompat.getColor(context, R.color.colorWhite)
    val whiteColorAlpha = ContextCompat.getColor(context, R.color.colorWhiteAlpha)

    var callback: OnItemClickListener<CropRatio>? = null
    var currentRatio = CropRatio.RATIO_1_1
    set(value){
        field = value
        notifyDataSetChanged()
    }


    fun setOnItemClickListener(callback: OnItemClickListener<CropRatio>) {
        this.callback = callback
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            ViewHolder {
        val view = LayoutInflater.from(context).inflate(
            R.layout.item_crop,
            parent, false
        )
        return ViewHolder(view)
    }

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val imageView = itemView.findViewById<ImageView>(R.id.cropImage)
        val textView = itemView.findViewById<TextView>(R.id.cropText)

        fun bind(item: CropRatio) {
            imageView.setImageResource(item.resources)
            textView.text = item.text
            itemView.setOnClickListener {
                callback?.onItemClick(itemView, position, item)
            }
            textView.setTextColor(if (currentRatio != item)  whiteColorAlpha else whiteColor)
            imageView.alpha = if (currentRatio != item)  0.5f else 1f
        }
    }
}




