package com.example.sentryone

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.sentryone.Fragments.ContactFragment
import com.example.sentryone.Fragments.HistoryFragment
import com.example.sentryone.Fragments.HomeFragment
import com.example.sentryone.Fragments.SettingsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomView : BottomNavigationView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        bottomView= findViewById(R.id.bottomNavigationView)
        replaceFragment(HomeFragment())
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
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frameLayout,fragment)
        fragmentTransaction.commit()

    }
}