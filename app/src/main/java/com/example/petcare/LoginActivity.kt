package com.example.petcare

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var layoutEmail: TextInputLayout
    private lateinit var layoutPassword: TextInputLayout

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText

    private lateinit var btnLogin: MaterialButton
    private lateinit var btnGoRegister: MaterialButton


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_login
        )


        // =====================================================
        // FIREBASE
        // =====================================================

        auth =
            FirebaseAuth.getInstance()


        // =====================================================
        // CONNECT XML
        // =====================================================

        layoutEmail =
            findViewById(
                R.id.layoutEmail
            )

        layoutPassword =
            findViewById(
                R.id.layoutPassword
            )

        etEmail =
            findViewById(
                R.id.etEmail
            )

        etPassword =
            findViewById(
                R.id.etPassword
            )

        btnLogin =
            findViewById(
                R.id.btnLogin
            )

        btnGoRegister =
            findViewById(
                R.id.btnGoRegister
            )


        // =====================================================
        // LOGIN BUTTON
        // =====================================================

        btnLogin.setOnClickListener {

            loginUser()
        }


        // =====================================================
        // CREATE ACCOUNT
        // =====================================================

        btnGoRegister.setOnClickListener {

            val intent =
                Intent(
                    this,
                    RegisterActivity::class.java
                )

            startActivity(
                intent
            )
        }
    }


    // =========================================================
    // LOGIN
    // =========================================================

    private fun loginUser() {


        // CLEAR PREVIOUS ERRORS

        layoutEmail.error =
            null

        layoutPassword.error =
            null


        // READ VALUES

        val email =
            etEmail.text
                .toString()
                .trim()

        val password =
            etPassword.text
                .toString()


        var valid =
            true


        // =====================================================
        // EMAIL VALIDATION
        // =====================================================

        if (email.isEmpty()) {

            layoutEmail.error =
                "Email is required"

            valid =
                false

        } else if (
            !Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()
        ) {

            layoutEmail.error =
                "Enter a valid email address"

            valid =
                false
        }


        // =====================================================
        // PASSWORD VALIDATION
        // =====================================================

        if (password.isEmpty()) {

            layoutPassword.error =
                "Password is required"

            valid =
                false

        } else if (
            password.length < 6
        ) {

            layoutPassword.error =
                "Password must contain at least 6 characters"

            valid =
                false
        }


        if (!valid) {

            return
        }


        // =====================================================
        // DISABLE BUTTON WHILE LOGIN
        // =====================================================

        btnLogin.isEnabled =
            false

        btnLogin.text =
            "Logging in..."


        // =====================================================
        // FIREBASE LOGIN
        // =====================================================

        auth.signInWithEmailAndPassword(
            email,
            password
        )
            .addOnSuccessListener {


                Toast.makeText(
                    this,
                    "Login successful",
                    Toast.LENGTH_SHORT
                ).show()


                // OPEN HOME

                val intent =
                    Intent(
                        this,
                        HomeActivity::class.java
                    )


                // REMOVE LOGIN/WELCOME FROM BACK STACK

                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK


                startActivity(
                    intent
                )


                finish()
            }
            .addOnFailureListener {


                btnLogin.isEnabled =
                    true

                btnLogin.text =
                    "Log In"


                // Generic message is better than exposing
                // authentication details.

                layoutEmail.error =
                    null

                layoutPassword.error =
                    "Invalid email or password"


                Toast.makeText(
                    this,
                    "Invalid email or password",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}