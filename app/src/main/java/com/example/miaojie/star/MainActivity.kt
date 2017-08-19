package com.example.miaojie.star
import android.app.FragmentTransaction
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.view.Window
import android.widget.TextView
import com.example.miaojie.star.MatchingConstellation.MatchingConstellaionFragment
import com.example.miaojie.star.`interface`.ActionBarController
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.main.*
import java.util.*

class MainActivity : AppCompatActivity(), ActionBarController {
    override fun getCurrentDate() {
        var c = Calendar.getInstance()
        var month: Int =c.get(Calendar.MONTH)
        month+=1
        var day:Int=c.get(Calendar.DAY_OF_MONTH)
        currentDate.setText(month.toString()+"月"+day.toString()+"日")
    }

    override fun requestNoActionBar() {
        toolBar.visibility= View.GONE
    }

    override fun setActionBar(toolbar: Toolbar) {
        setSupportActionBar(toolbar)
    }

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_match -> {

                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_earth -> {

                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_photo -> {

                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolBar)
        val matchingConstellationFragment:MatchingConstellaionFragment= MatchingConstellaionFragment()
        matchingConstellationFragment.actionBarController =this

        val transaction: FragmentTransaction =fragmentManager.beginTransaction()
        transaction.replace(R.id.content_main,matchingConstellationFragment,"1")
        transaction.commit()
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
    }

}


