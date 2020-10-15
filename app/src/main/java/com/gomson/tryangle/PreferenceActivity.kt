package com.gomson.tryangle

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity

class PreferenceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preference)

        supportFragmentManager.beginTransaction()
            .replace(R.id.preferenceLayout, PreferenceFragment())
            .commit()
    }

}