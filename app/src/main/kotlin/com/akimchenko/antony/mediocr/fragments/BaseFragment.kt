package com.akimchenko.antony.mediocr.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.utils.NotificationCenter
import kotlinx.android.synthetic.main.toolbar.*


abstract class BaseFragment: Fragment(), NotificationCenter.Observer {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity as MainActivity? ?: return
        activity.setSupportActionBar(toolbar)
    }

    override fun onResume() {
        super.onResume()
        NotificationCenter.addObserver(this)
    }

    override fun onPause() {
        super.onPause()
        NotificationCenter.removeObserver(this)
    }

    override fun onNotification(id: Int, `object`: Any?) {
        //catch notification where it need
    }

    open fun onBackPressed() {
        activity?.supportFragmentManager?.popBackStack()
    }
}