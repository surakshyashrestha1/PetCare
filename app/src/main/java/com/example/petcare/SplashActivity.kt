package com.example.petcare

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash)

        Handler(
            Looper.getMainLooper()
        ).postDelayed({

            val currentUser =
                FirebaseAuth.getInstance().currentUser

            val intent =
                if (currentUser != null) {

                    Intent(
                        this,
                        HomeActivity::class.java
                    )

                } else {

                    Intent(
                        this,
                        MainActivity::class.java
                    )
                }

            startActivity(intent)

            finish()

        }, 1400)
    }
}