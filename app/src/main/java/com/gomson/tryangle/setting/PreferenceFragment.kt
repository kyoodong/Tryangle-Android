package com.gomson.tryangle.setting

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.gomson.tryangle.R

class PreferenceFragment : PreferenceFragmentCompat() {

    companion object{
        val KEY_HIGH_DEFINITION = "highDefinition"
    }

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?
    ) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

}