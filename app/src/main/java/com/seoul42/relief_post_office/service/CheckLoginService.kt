package com.seoul42.relief_post_office.service

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seoul42.relief_post_office.MainActivity

class CheckLoginService : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userDB: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
        if (auth.currentUser == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            userDB = Firebase.database.reference.child("user")
            userDB.get().addOnSuccessListener {
                var flag = false
                for (user in it.children) {
                    if (auth.uid == user.key) {
                        flag = true
                    }
                }
                if (!flag) {
                    auth.signOut()
                }
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}