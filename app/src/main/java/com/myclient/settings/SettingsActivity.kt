package com.myclient.settings

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import com.myclient.R

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        //confg titulo
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.title = getString(R.string.product_menu_settings)
        }

        //vinculacion del fragment con la activity
        val fragment = SettingsFragment()
        supportFragmentManager
            .beginTransaction()
            .add(R.id.containerMain, fragment)
            .commit()
    }

    //confg boton de retroceso
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home){
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }
}