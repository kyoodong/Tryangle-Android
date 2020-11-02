package com.gomson.tryangle.album

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.gomson.tryangle.OnItemClickListener
import com.gomson.tryangle.R
import kotlinx.android.synthetic.main.item_guide_image.view.*


class BucketAdapter(private val context: Context, private val bucketList: MutableList<Bucket>) :
    RecyclerView.Adapter<BucketAdapter.BucketViewHolder>() {

    var callback: OnItemClickListener<Bucket>? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BucketViewHolder {
        val view = LayoutInflater.from(context).inflate(
            R.layout.item_bucket,
            parent, false
        )

        return BucketViewHolder(view)
    }

    override fun getItemCount(): Int {
        return bucketList.size
    }

    public fun setOnItemClickListner(callback: OnItemClickListener<Bucket>) {
        this.callback = callback
    }

    override fun onBindViewHolder(holder: BucketViewHolder, position: Int) {
        val item = bucketList[position]
        holder.bind(position, item)
    }

    inner class BucketViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val imageView = itemView.findViewById<ImageView>(R.id.image)
        val displayNameView = itemView.findViewById<TextView>(R.id.displayName)
        val imageNumView = itemView.findViewById<TextView>(R.id.imageNum)
        val multiOption = MultiTransformation(CenterCrop(), RoundedCorners(20));

        fun bind(position: Int, item: Bucket) {
            Glide.with(itemView)
                .load(item.images[0].contentUri) // Uri of the picture
                .apply(RequestOptions.bitmapTransform(multiOption))
                .into(imageView)

            displayNameView.text = item.name
            itemView.imageView
            imageNumView.text = item.images.size.toString()
            itemView.setOnClickListener {
                callback?.onItemClick(it, position, item)
            }
        }
    }
}