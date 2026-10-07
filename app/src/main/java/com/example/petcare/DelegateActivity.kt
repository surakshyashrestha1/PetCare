package com.example.petcare

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class DelegateActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var actDelegatePlan: AutoCompleteTextView
    private lateinit var etCaregiverName: TextInputEditText
    private lateinit var etCaregiverPhone: TextInputEditText
    private lateinit var etDelegateMessage: TextInputEditText

    private val petNames =
        mutableListOf<String>()

    private val petIds =
        mutableListOf<String>()

    private var selectedPetId =
        ""

    private var selectedPetName =
        ""


    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_delegate
        )


        // =====================================================
        // FIREBASE
        // =====================================================

        auth =
            FirebaseAuth.getInstance()

        db =
            FirebaseFirestore.getInstance()


        // =====================================================
        // LOGIN CHECK
        // =====================================================

        if (auth.currentUser == null) {

            val intent =
                Intent(
                    this,
                    MainActivity::class.java
                )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(
                intent
            )

            finish()

            return
        }


        // =====================================================
        // CONNECT XML
        // =====================================================

        actDelegatePlan =
            findViewById(
                R.id.actDelegatePlan
            )

        etCaregiverName =
            findViewById(
                R.id.etCaregiverName
            )

        etCaregiverPhone =
            findViewById(
                R.id.etCaregiverPhone
            )

        etDelegateMessage =
            findViewById(
                R.id.etDelegateMessage
            )


        val btnGenerateMessage =
            findViewById<MaterialButton>(
                R.id.btnGenerateMessage
            )

        val btnSendSms =
            findViewById<MaterialButton>(
                R.id.btnSendSms
            )

        val bottomNavigation =
            findViewById<BottomNavigationView>(
                R.id.bottomNavigation
            )


        // =====================================================
        // SCENARIO CAREGIVER
        // =====================================================

        if (
            etCaregiverName.text
                .toString()
                .trim()
                .isEmpty()
        ) {

            etCaregiverName.setText(
                "Daniel"
            )
        }


        // =====================================================
        // LOAD PETS
        // =====================================================

        loadPets()


        // =====================================================
        // SELECT PET
        // =====================================================

        actDelegatePlan.setOnItemClickListener {
                _,
                _,
                position,
                _ ->

            if (
                position >= 0 &&
                position < petIds.size
            ) {

                selectedPetId =
                    petIds[position]

                selectedPetName =
                    petNames[position]

                generateChecklistMessage()
            }
        }


        // =====================================================
        // REFRESH MESSAGE
        // =====================================================

        btnGenerateMessage.setOnClickListener {

            if (
                selectedPetId.isBlank()
            ) {

                Toast.makeText(
                    this,
                    "Please select a pet checklist first",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                generateChecklistMessage()
            }
        }


        // =====================================================
        // SEND SMS
        // =====================================================

        btnSendSms.setOnClickListener {

            sendSms()
        }


        // =====================================================
        // BOTTOM NAVIGATION
        // =====================================================

        bottomNavigation.selectedItemId =
            R.id.nav_delegate


        bottomNavigation.setOnItemSelectedListener { item ->

            when (
                item.itemId
            ) {


                // HOME

                R.id.nav_home -> {

                    startActivity(
                        Intent(
                            this,
                            HomeActivity::class.java
                        )
                    )

                    finish()

                    true
                }


                // CHECKLIST

                R.id.nav_checklist -> {

                    startActivity(
                        Intent(
                            this,
                            TasksActivity::class.java
                        )
                    )

                    finish()

                    true
                }


                // ROUTINE

                R.id.nav_routine -> {

                    startActivity(
                        Intent(
                            this,
                            RoutineActivity::class.java
                        )
                    )

                    finish()

                    true
                }


                // CURRENT PAGE

                R.id.nav_delegate -> {

                    true
                }


                // MAP

                R.id.nav_map -> {

                    startActivity(
                        Intent(
                            this,
                            MapActivity::class.java
                        )
                    )

                    finish()

                    true
                }


                else -> false
            }
        }
    }


    // =========================================================
    // LOAD USER PETS
    // =========================================================

    private fun loadPets() {

        val userId =
            auth.currentUser
                ?.uid
                ?: return


        db.collection(
            "pets"
        )
            .whereEqualTo(
                "userId",
                userId
            )
            .get()
            .addOnSuccessListener { documents ->


                petNames.clear()
                petIds.clear()


                for (
                document in documents
                ) {

                    val petName =
                        document.getString(
                            "name"
                        ) ?: "Pet"


                    val petId =
                        document.getString(
                            "id"
                        ) ?: document.id


                    petNames.add(
                        petName
                    )

                    petIds.add(
                        petId
                    )
                }


                // =================================================
                // NO PETS
                // =================================================

                if (
                    petNames.isEmpty()
                ) {

                    actDelegatePlan.setText(
                        "No pets available",
                        false
                    )


                    etDelegateMessage.setText(
                        "Add a pet and care routine before creating a delegated checklist."
                    )


                    return@addOnSuccessListener
                }


                // =================================================
                // DROPDOWN NAMES
                // =================================================

                val checklistNames =
                    petNames.map { name ->

                        "$name's daily care checklist"
                    }


                val adapter =
                    ArrayAdapter(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        checklistNames
                    )


                actDelegatePlan.setAdapter(
                    adapter
                )


                // =================================================
                // PREFER MAX FOR SCENARIO
                // =================================================

                val maxIndex =
                    petNames.indexOfFirst { name ->

                        name.equals(
                            "Max",
                            ignoreCase = true
                        )
                    }


                val selectedIndex =
                    if (
                        maxIndex >= 0
                    ) {

                        maxIndex

                    } else {

                        0
                    }


                selectedPetName =
                    petNames[selectedIndex]

                selectedPetId =
                    petIds[selectedIndex]


                actDelegatePlan.setText(
                    checklistNames[selectedIndex],
                    false
                )


                // =================================================
                // CREATE MESSAGE
                // =================================================

                generateChecklistMessage()
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Unable to load pets: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    // =========================================================
    // GENERATE MESSAGE
    // =========================================================

    private fun generateChecklistMessage() {

        val userId =
            auth.currentUser
                ?.uid
                ?: return


        if (
            selectedPetId.isBlank()
        ) {

            return
        }


        db.collection(
            "routines"
        )
            .whereEqualTo(
                "userId",
                userId
            )
            .get()
            .addOnSuccessListener { documents ->


                val taskItems =
                    mutableListOf<Pair<String, String>>()



                for (
                document in documents
                ) {


                    val petId =
                        document.getString(
                            "petId"
                        ).orEmpty()


                    if (
                        petId != selectedPetId
                    ) {

                        continue
                    }


                    // =============================================
                    // ROUTINE DATA
                    // =============================================

                    val taskName =
                        document.getString(
                            "taskName"
                        )
                            ?.trim()
                            .orEmpty()
                            .ifEmpty {

                                "Care Task"
                            }


                    val category =
                        document.getString(
                            "category"
                        )
                            ?.trim()
                            .orEmpty()


                    val time =
                        document.getString(
                            "time"
                        )
                            ?.trim()
                            .orEmpty()


                    val frequency =
                        document.getString(
                            "frequency"
                        )
                            ?.trim()
                            .orEmpty()


                    val instructions =
                        document.getString(
                            "instructions"
                        )
                            ?.trim()
                            .orEmpty()


                    // =============================================
                    // ONE MESSAGE LINE
                    // =============================================

                    val line =
                        buildString {

                            append(
                                "• "
                            )

                            append(
                                taskName
                            )


                            if (
                                category.isNotEmpty()
                            ) {

                                append(
                                    " — $category"
                                )
                            }


                            if (
                                time.isNotEmpty()
                            ) {

                                append(
                                    " — $time"
                                )
                            }


                            if (
                                frequency.isNotEmpty()
                            ) {

                                append(
                                    " ($frequency)"
                                )
                            }


                            if (
                                instructions.isNotEmpty()
                            ) {

                                append(
                                    "\n  $instructions"
                                )
                            }
                        }


                    taskItems.add(
                        time to line
                    )
                }


                // =================================================
                // SORT BY TIME
                // =================================================

                taskItems.sortBy { item ->

                    item.first
                }


                // =================================================
                // CAREGIVER
                // =================================================

                val caregiverName =
                    etCaregiverName.text
                        ?.toString()
                        ?.trim()
                        .orEmpty()
                        .ifEmpty {

                            "Daniel"
                        }


                // =================================================
                // FINAL SMS MESSAGE
                // =================================================

                val message =
                    buildString {


                        append(
                            "PetCare - $selectedPetName's Care Checklist"
                        )


                        append(
                            "\n\n"
                        )


                        append(
                            "Hi $caregiverName,"
                        )


                        append(
                            "\n"
                        )


                        append(
                            "Here is $selectedPetName's care plan while I'm away:"
                        )


                        append(
                            "\n\n"
                        )


                        if (
                            taskItems.isEmpty()
                        ) {

                            append(
                                "No care routines have been added yet."
                            )

                        } else {

                            taskItems.forEach { item ->

                                append(
                                    item.second
                                )

                                append(
                                    "\n"
                                )
                            }
                        }


                        append(
                            "\nPlease make sure $selectedPetName has fresh water and contact me if anything is unclear."
                        )


                        append(
                            "\n\nSent from PetCare."
                        )
                    }


                etDelegateMessage.setText(
                    message
                )
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Unable to create checklist: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    // =========================================================
    // SEND VIA DEFAULT SMS APP
    // =========================================================

    private fun sendSms() {


        // =====================================================
        // VALUES
        // =====================================================

        val caregiverName =
            etCaregiverName.text
                ?.toString()
                ?.trim()
                .orEmpty()


        val phone =
            etCaregiverPhone.text
                ?.toString()
                ?.trim()
                .orEmpty()


        val message =
            etDelegateMessage.text
                ?.toString()
                ?.trim()
                .orEmpty()


        // =====================================================
        // VALIDATION - PET
        // =====================================================

        if (
            selectedPetId.isBlank()
        ) {

            Toast.makeText(
                this,
                "Please select a pet first",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // =====================================================
        // VALIDATION - CAREGIVER NAME
        // =====================================================

        if (
            caregiverName.isEmpty()
        ) {

            etCaregiverName.error =
                "Caregiver name is required"

            etCaregiverName.requestFocus()

            return
        }


        // =====================================================
        // VALIDATION - PHONE
        // =====================================================

        if (
            phone.isEmpty()
        ) {

            etCaregiverPhone.error =
                "Phone number is required"

            etCaregiverPhone.requestFocus()

            return
        }


        val cleanPhone =
            phone.filter { character ->

                character.isDigit() ||
                        character == '+'
            }


        val digitCount =
            cleanPhone.count { character ->

                character.isDigit()
            }


        if (
            digitCount < 7
        ) {

            etCaregiverPhone.error =
                "Enter a valid phone number"

            etCaregiverPhone.requestFocus()

            return
        }


        // =====================================================
        // VALIDATION - MESSAGE
        // =====================================================

        if (
            message.isEmpty()
        ) {

            etDelegateMessage.error =
                "Message cannot be empty"

            etDelegateMessage.requestFocus()

            return
        }


        // =====================================================
        // PRIMARY SMS INTENT
        // =====================================================

        val smsIntent =
            Intent(
                Intent.ACTION_SENDTO
            ).apply {

                data =
                    Uri.parse(
                        "smsto:$cleanPhone"
                    )

                putExtra(
                    "sms_body",
                    message
                )
            }


        // =====================================================
        // TRY TO OPEN DEFAULT SMS APP
        // =====================================================

        try {

            startActivity(
                smsIntent
            )

        } catch (
            firstException: ActivityNotFoundException
        ) {


            // =================================================
            // FALLBACK FOR SOME XIAOMI / ANDROID DEVICES
            // =================================================

            val fallbackIntent =
                Intent(
                    Intent.ACTION_VIEW
                ).apply {

                    data =
                        Uri.parse(
                            "sms:$cleanPhone"
                        )

                    putExtra(
                        "address",
                        cleanPhone
                    )

                    putExtra(
                        "sms_body",
                        message
                    )
                }


            try {

                startActivity(
                    fallbackIntent
                )

            } catch (
                secondException: Exception
            ) {

                Toast.makeText(
                    this,
                    "No SMS application is available. Please install or enable a Messages app.",
                    Toast.LENGTH_LONG
                ).show()
            }


        } catch (
            exception: Exception
        ) {

            Toast.makeText(
                this,
                "Unable to open the messaging app.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}