package com.example.petcare

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val btnLogin =
            findViewById<MaterialButton>(
                R.id.btnLogin
            )

        val btnRegister =
            findViewById<MaterialButton>(
                R.id.btnRegister
            )


        // LOGIN
        btnLogin.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    LoginActivity::class.java
                )
            )
        }


        // CREATE ACCOUNT
        btnRegister.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    RegisterActivity::class.java
                )
            )
        }
    }
}