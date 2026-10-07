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
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var layoutFullName: TextInputLayout
    private lateinit var layoutEmail: TextInputLayout
    private lateinit var layoutPassword: TextInputLayout
    private lateinit var layoutConfirmPassword: TextInputLayout

    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText

    private lateinit var btnRegister: MaterialButton


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_register
        )


        // =====================================================
        // FIREBASE
        // =====================================================

        auth =
            FirebaseAuth.getInstance()

        db =
            FirebaseFirestore.getInstance()


        // =====================================================
        // XML
        // =====================================================

        layoutFullName =
            findViewById(
                R.id.layoutFullName
            )

        layoutEmail =
            findViewById(
                R.id.layoutEmail
            )

        layoutPassword =
            findViewById(
                R.id.layoutPassword
            )

        layoutConfirmPassword =
            findViewById(
                R.id.layoutConfirmPassword
            )


        etFullName =
            findViewById(
                R.id.etFullName
            )

        etEmail =
            findViewById(
                R.id.etEmail
            )

        etPassword =
            findViewById(
                R.id.etPassword
            )

        etConfirmPassword =
            findViewById(
                R.id.etConfirmPassword
            )


        val btnBack =
            findViewById<MaterialButton>(
                R.id.btnBack
            )

        btnRegister =
            findViewById(
                R.id.btnRegister
            )

        val btnGoLogin =
            findViewById<MaterialButton>(
                R.id.btnGoLogin
            )


        // =====================================================
        // BACK
        // =====================================================

        btnBack.setOnClickListener {

            finish()
        }


        // =====================================================
        // REGISTER
        // =====================================================

        btnRegister.setOnClickListener {

            registerUser()
        }


        // =====================================================
        // GO TO LOGIN
        // =====================================================

        btnGoLogin.setOnClickListener {

            val intent =
                Intent(
                    this,
                    LoginActivity::class.java
                )

            startActivity(intent)

            finish()
        }
    }


    // =========================================================
    // REGISTER USER
    // =========================================================

    private fun registerUser() {


        // =====================================================
        // CLEAR ERRORS
        // =====================================================

        layoutFullName.error =
            null

        layoutEmail.error =
            null

        layoutPassword.error =
            null

        layoutConfirmPassword.error =
            null


        // =====================================================
        // VALUES
        // =====================================================

        val fullName =
            etFullName.text
                .toString()
                .trim()

        val email =
            etEmail.text
                .toString()
                .trim()

        val password =
            etPassword.text
                .toString()

        val confirmPassword =
            etConfirmPassword.text
                .toString()


        var valid =
            true


        // =====================================================
        // NAME VALIDATION
        // =====================================================

        if (fullName.isEmpty()) {

            layoutFullName.error =
                "Full name is required"

            valid =
                false
        }


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
        // PASSWORD
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


        // =====================================================
        // CONFIRM PASSWORD
        // =====================================================

        if (confirmPassword.isEmpty()) {

            layoutConfirmPassword.error =
                "Please confirm your password"

            valid =
                false

        } else if (
            password != confirmPassword
        ) {

            layoutConfirmPassword.error =
                "Passwords do not match"

            valid =
                false
        }


        if (!valid) {

            Toast.makeText(
                this,
                "Please correct the highlighted fields",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // =====================================================
        // BUTTON LOADING
        // =====================================================

        btnRegister.isEnabled =
            false

        btnRegister.text =
            "Creating Account..."


        // =====================================================
        // FIREBASE AUTH
        // =====================================================

        auth.createUserWithEmailAndPassword(
            email,
            password
        )
            .addOnSuccessListener { authResult ->


                val user =
                    authResult.user


                if (user == null) {

                    resetRegisterButton()

                    Toast.makeText(
                        this,
                        "Unable to create account",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnSuccessListener
                }


                // =============================================
                // STORE PROFILE IN FIRESTORE
                // =============================================

                val userProfile =
                    hashMapOf(
                        "uid" to user.uid,
                        "fullName" to fullName,
                        "email" to email
                    )


                db.collection(
                    "users"
                )
                    .document(
                        user.uid
                    )
                    .set(
                        userProfile
                    )
                    .addOnSuccessListener {


                        Toast.makeText(
                            this,
                            "Account created successfully",
                            Toast.LENGTH_SHORT
                        ).show()


                        // Sign out so user demonstrates login
                        // separately after registration.

                        auth.signOut()


                        val intent =
                            Intent(
                                this,
                                LoginActivity::class.java
                            )


                        intent.flags =
                            Intent.FLAG_ACTIVITY_CLEAR_TOP


                        startActivity(
                            intent
                        )


                        finish()
                    }
                    .addOnFailureListener { exception ->


                        resetRegisterButton()


                        Toast.makeText(
                            this,
                            "Profile save failed: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { exception ->


                resetRegisterButton()


                val message =
                    when {

                        exception.message
                            ?.contains(
                                "email address is already in use",
                                ignoreCase = true
                            ) == true -> {

                            "An account with this email already exists"
                        }


                        exception.message
                            ?.contains(
                                "network",
                                ignoreCase = true
                            ) == true -> {

                            "Please check your internet connection"
                        }


                        else -> {

                            exception.message
                                ?: "Registration failed"
                        }
                    }


                if (
                    message.contains(
                        "already exists",
                        ignoreCase = true
                    )
                ) {

                    layoutEmail.error =
                        message
                }


                Toast.makeText(
                    this,
                    message,
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    // =========================================================
    // RESET BUTTON
    // =========================================================

    private fun resetRegisterButton() {

        btnRegister.isEnabled =
            true

        btnRegister.text =
            "Create Account"
    }
}