package com.gomson.tryangle.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.gomson.tryangle.R
import com.gomson.tryangle.domain.GuideTabItem
import com.gomson.tryangle.view.guide_image_view.GuideImageAdapter
import com.gomson.tryangle.view.guide_image_view.GuideImageListView
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.view_guide_image_category_tab_layout.view.*

private const val TAG = "ImageCategoryTabLayout"

class GuideImageCategoryTabLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    :LinearLayout(context, attrs, defStyleAttr) {

    private val guideTabItemList = ArrayList<GuideTabItem>()
    private val guideImageListViewList = ArrayList<GuideImageListView>()
    private var listener: GuideImageAdapter.OnClickGuideImage? = null

    init {
        inflate(context, R.layout.view_guide_image_category_tab_layout, this)

        tabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                Log.d(TAG, "onTabSelected")
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                Log.d(TAG, "onTabUnselected")
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                Log.d(TAG, "onTabReselected")
            }
        })
    }

    fun addTab(guideTabItem: GuideTabItem) {
        val tab = tabLayout.newTab()
        tab.text = guideTabItem.name
        tab.id = guideTabItemList.size
        tabLayout.addTab(tab)

        guideTabItemList.add(guideTabItem)
        val imageListView = GuideImageListView(context)
        imageListView.layoutParams = FrameLayout.LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )

        imageListView.visibility = if (tab.id == 0) View.VISIBLE else View.INVISIBLE
        if (listener != null)
            imageListView.getAdapter().setOnClickGuideImageListener(listener!!)
        imageListView.getAdapter().addImageUrlList(guideTabItem.imageUrlList)
        guideImageListViewList.add(imageListView)
        contentLayout.addView(imageListView)
        contentLayout.invalidate()
        invalidate()
    }

    fun addImageUrlList(urlList: ArrayList<String>) {
        guideImageListViewList[0].getAdapter().setImageUrlList(urlList)
    }

    fun setOnClickGuideImageListener(listener: GuideImageAdapter.OnClickGuideImage) {
        this.listener = listener

        for (imageListView in guideImageListViewList) {
            imageListView.getAdapter().setOnClickGuideImageListener(listener)
        }
    }
}