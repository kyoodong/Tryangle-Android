package com.gomson.tryangle

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

class PreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?
    ) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

}