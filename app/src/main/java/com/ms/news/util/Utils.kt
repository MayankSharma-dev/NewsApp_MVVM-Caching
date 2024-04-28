package com.ms.news.util

import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

fun Fragment.showSnackbar(
    message: String,
    duration: Int = Snackbar.LENGTH_LONG,
    view: View = requireView()
){
    Snackbar.make(view,message,duration).show()
}

// making extension fun() on Generic(Any) Type
val <T> T.exhaustive: T
    get() = this

inline fun <T:View> T.showIfOrInvisible(condition: (T) -> Boolean){
    if(condition(this)){
        this.visibility = View.VISIBLE
    }else{
        this.visibility = View.INVISIBLE
    }
}

inline fun SearchView.onQueryTextSubmit(crossinline listener: (String)-> Unit){
    // this keyword refers to SearchView Class here.
    this.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
        override fun onQueryTextSubmit(query: String?): Boolean {
            if(!query.isNullOrBlank()){
                listener(query)
            }
            return true
        }

        // will not be using this in the project here.
        // that's why true is returned.
        override fun onQueryTextChange(newText: String?): Boolean {
            return true
        }
    })
}