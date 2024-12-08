package com.ldihackos.tellvport.views

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.ldihackos.tellvport.R
import com.ldihackos.tellvport.databinding.ActivityNewMainBinding
import com.ldihackos.tellvport.utils.LanguagePref
import com.ldihackos.tellvport.utils.setLocale
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewMainBinding

    public override fun attachBaseContext(newBase: Context?) {
        val language = LanguagePref.getLanguage(newBase!!)
        val context = newBase.setLocale(language)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation_view)
        val navController = findNavController(R.id.nav_host_fragment)

        NavigationUI.setupWithNavController(bottomNavigationView, navController)
    }

    fun updateLanguage(language: String) {
        LanguagePref.saveLanguage(this, language)
        recreate() // Recreate the activity to apply the new locale
    }

    fun toggleBottomBar() {
        if (binding.bottomNavigationView.visibility == View.VISIBLE) {
            binding.bottomNavigationView.animate()
                .translationY(binding.bottomNavigationView.height.toFloat()) // Slide down
                .setDuration(100) // Animation duration in milliseconds
                .withEndAction {
                    binding.bottomNavigationView.visibility = View.GONE
                }
                .start()
        } else {
            binding.bottomNavigationView.apply {
                visibility = View.VISIBLE
                translationY = height.toFloat() // Start below the screen
                animate()
                    .translationY(0f) // Slide up to its original position
                    .setDuration(100) // Animation duration in milliseconds
                    .start()
            }
        }
    }

}
