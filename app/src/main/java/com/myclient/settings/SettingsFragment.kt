package com.myclient.settings

import android.os.Bundle
import android.widget.Toast
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.myclient.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        //config del switch
        val switchPreferenceCompat = findPreference<SwitchPreferenceCompat>(getString(R.string.pref_offers_key))
        switchPreferenceCompat?.setOnPreferenceChangeListener { preference, newValue ->
            //verificar que sea de tipo boolean
            (newValue as? Boolean)?.let { isChecked ->
                //creacion de topic
                val topic = getString(R.string.settings_topic_offers)
                if (isChecked){
                    //suscripcion al topic
                    Firebase.messaging.subscribeToTopic(topic)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Notificaciones activadas", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    //desuscripcion al topic
                    Firebase.messaging.unsubscribeFromTopic(topic)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Notificaciones desactivadas", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            true
        }
    }
}