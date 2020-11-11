package com.gomson.tryangle.setting

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.gomson.tryangle.R

class PreferenceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preference)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.preferenceLayout,
                PreferenceFragment()
            )
            .commit()
    }
}