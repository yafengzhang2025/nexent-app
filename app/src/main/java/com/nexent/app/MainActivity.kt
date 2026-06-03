package com.nexent.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.nexent.app.databinding.ActivityMainBinding
import com.nexent.app.util.PreferenceHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)

        val prefHelper = PreferenceHelper(this)
        if (!prefHelper.isConfigured()) {
            Toast.makeText(this, getString(R.string.please_configure_server), Toast.LENGTH_LONG).show()
            navController.navigate(R.id.settingsFragment)
        }
    }
}
