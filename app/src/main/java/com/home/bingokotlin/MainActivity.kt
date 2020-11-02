package com.home.bingokotlin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import java.util.*

class MainActivity : AppCompatActivity(), FirebaseAuth.AuthStateListener {
    companion object{
        val TAG =  MainActivity::class.java.simpleName
        val RC_SING_IN = 100
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
        FirebaseAuth.getInstance().addAuthStateListener(this)
    }

    override fun onStop() {
        super.onStop()
        FirebaseAuth.getInstance().removeAuthStateListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_signout -> FirebaseAuth.getInstance().signOut()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
       val data = auth.currentUser.also {
           Log.d(TAG, it?.uid)
       } ?: signUp()


        /* if(auth.currentUser == null){
            signUp()

        }else{
            Log.d(TAG, "onAuthStateChanged: ${auth.currentUser?.uid} ")
        }*/
    }

    private fun signUp() {
        startActivityForResult(
            AuthUI.getInstance().createSignInIntentBuilder()
                .setAvailableProviders(
                    Arrays.asList(
                        AuthUI.IdpConfig.EmailBuilder().build(),
                        AuthUI.IdpConfig.GoogleBuilder().build()
                    ))
                .setIsSmartLockEnabled(false)
                .build(), RC_SING_IN
        )
    }
}