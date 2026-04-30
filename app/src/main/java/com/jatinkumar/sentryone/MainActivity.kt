package com.jatinkumar.sentryone

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.jatinkumar.sentryone.Fragments.ContactFragment
import com.jatinkumar.sentryone.Fragments.HistoryFragment
import com.jatinkumar.sentryone.Fragments.HomeFragment
import com.jatinkumar.sentryone.Fragments.SettingsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomView : BottomNavigationView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomView= findViewById(R.id.bottomNavigationView)

        if (savedInstanceState== null){
            replaceFragment(HomeFragment())
        }
        bottomView.setOnItemSelectedListener{
            when(it.itemId){
                R.id.home -> replaceFragment(HomeFragment())
                R.id.contact -> replaceFragment(ContactFragment())
                R.id.history -> replaceFragment(HistoryFragment())
                R.id.setting -> replaceFragment(SettingsFragment())
                else ->{
                }
            }
            true
        }
    }
    private fun replaceFragment(fragment: Fragment){
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, fragment)
            .commit()
    }
}