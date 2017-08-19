package com.example.miaojie.star.`interface`

import android.support.v7.widget.Toolbar
import android.widget.TextView

/**
 * Created by miaojie on 2017/8/19.
 */
interface ActionBarController {
    fun setActionBar(toolbar: Toolbar)
    fun requestNoActionBar()
    fun getCurrentDate()
}