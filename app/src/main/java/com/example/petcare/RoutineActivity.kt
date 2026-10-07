package com.example.petcare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.petcare.data.CareRoutine
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RoutineActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var routinesContainer: LinearLayout
    private lateinit var emptyRoutineState: MaterialCardView
    private lateinit var tvRoutineHeading: TextView

    private var filterPetId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_routine)

        // =====================================================
        // FIREBASE
        // =====================================================

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

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

            startActivity(intent)
            finish()

            return
        }

        // =====================================================
        // CONNECT XML
        // =====================================================

        routinesContainer =
            findViewById(
                R.id.routinesContainer
            )

        emptyRoutineState =
            findViewById(
                R.id.emptyRoutineState
            )

        tvRoutineHeading =
            findViewById(
                R.id.tvRoutineHeading
            )

        val btnAddRoutineTop =
            findViewById<MaterialButton>(
                R.id.btnAddRoutineTop
            )

        val btnAddRoutineBottom =
            findViewById<MaterialButton>(
                R.id.btnAddRoutineBottom
            )

        val bottomNavigation =
            findViewById<BottomNavigationView>(
                R.id.bottomNavigation
            )

        // =====================================================
        // OPTIONAL PET FILTER
        // =====================================================

        filterPetId =
            intent.getStringExtra(
                "PET_ID"
            ) ?: ""

        // =====================================================
        // ADD ROUTINE BUTTONS
        // =====================================================

        btnAddRoutineTop.setOnClickListener {

            openAddRoutine()
        }

        btnAddRoutineBottom.setOnClickListener {

            openAddRoutine()
        }

        // =====================================================
        // BOTTOM NAVIGATION
        //
        // Home | Checklist | Routine | Delegate | Map
        // =====================================================

        bottomNavigation.selectedItemId =
            R.id.nav_routine

        bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

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

                    true
                }

                // DELEGATE
                R.id.nav_delegate -> {

                    startActivity(
                        Intent(
                            this,
                            DelegateActivity::class.java
                        )
                    )

                    finish()

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
    // REFRESH ROUTINES
    // =========================================================

    override fun onResume() {
        super.onResume()

        loadRoutines()
    }

    // =========================================================
    // OPEN ADD ROUTINE
    // =========================================================

    private fun openAddRoutine() {

        val intent =
            Intent(
                this,
                AddRoutineActivity::class.java
            )

        if (filterPetId.isNotEmpty()) {

            intent.putExtra(
                "PET_ID",
                filterPetId
            )
        }

        startActivity(intent)
    }

    // =========================================================
    // LOAD ROUTINES FROM FIRESTORE
    // =========================================================

    private fun loadRoutines() {

        val userId =
            auth.currentUser?.uid ?: return

        db.collection("routines")
            .whereEqualTo(
                "userId",
                userId
            )
            .get()
            .addOnSuccessListener { documents ->

                routinesContainer.removeAllViews()

                val routines =
                    mutableListOf<CareRoutine>()

                for (document in documents) {

                    val routine =
                        document.toObject(
                            CareRoutine::class.java
                        )

                    // Safety:
                    // If an older Firestore document has no id field,
                    // use the Firestore document ID.
                    if (routine.id.isEmpty()) {
                        routine.id = document.id
                    }

                    if (
                        filterPetId.isEmpty() ||
                        routine.petId == filterPetId
                    ) {

                        routines.add(
                            routine
                        )
                    }
                }

                // =================================================
                // EMPTY STATE
                // =================================================

                if (routines.isEmpty()) {

                    emptyRoutineState.visibility =
                        View.VISIBLE

                    routinesContainer.visibility =
                        View.GONE

                } else {

                    // =================================================
                    // ROUTINES AVAILABLE
                    // =================================================

                    emptyRoutineState.visibility =
                        View.GONE

                    routinesContainer.visibility =
                        View.VISIBLE

                    for (routine in routines) {

                        addRoutineCard(
                            routine
                        )
                    }
                }
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Unable to load routines: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // CREATE ROUTINE CARD
    // =========================================================

    private fun addRoutineCard(
        routine: CareRoutine
    ) {

        val routineView =
            layoutInflater.inflate(
                R.layout.item_routine,
                routinesContainer,
                false
            )

        val tvRoutineName =
            routineView.findViewById<TextView>(
                R.id.tvRoutineName
            )

        val tvRoutinePet =
            routineView.findViewById<TextView>(
                R.id.tvRoutinePet
            )

        val tvRoutineTask =
            routineView.findViewById<TextView>(
                R.id.tvRoutineTask
            )

        val tvRoutineInfo =
            routineView.findViewById<TextView>(
                R.id.tvRoutineInfo
            )

        val tvRepeatDays =
            routineView.findViewById<TextView>(
                R.id.tvRepeatDays
            )

        val btnEditRoutine =
            routineView.findViewById<MaterialButton>(
                R.id.btnEditRoutine
            )

        val btnDeleteRoutine =
            routineView.findViewById<MaterialButton>(
                R.id.btnDeleteRoutine
            )

        // =====================================================
        // DISPLAY ROUTINE DATA
        // =====================================================

        tvRoutineName.text =
            routine.routineName

        tvRoutinePet.text =
            "🐾 ${routine.petName}"

        tvRoutineTask.text =
            routine.taskName

        tvRoutineInfo.text =
            "${routine.category} • ${routine.time} • ${routine.frequency}"

        tvRepeatDays.text =
            if (routine.repeatDays.isEmpty()) {

                "No repeat days selected"

            } else {

                "Repeat: ${
                    routine.repeatDays.joinToString(", ")
                }"
            }

        // =====================================================
        // EDIT ROUTINE
        // =====================================================

        btnEditRoutine.setOnClickListener {

            val intent =
                Intent(
                    this,
                    AddRoutineActivity::class.java
                )

            intent.putExtra(
                "ROUTINE_ID",
                routine.id
            )

            startActivity(intent)
        }

        // =====================================================
        // DELETE ROUTINE
        // =====================================================

        btnDeleteRoutine.setOnClickListener {

            showDeleteDialog(
                routine
            )
        }

        // =====================================================
        // ADD CARD TO CONTAINER
        // =====================================================

        routinesContainer.addView(
            routineView
        )
    }

    // =========================================================
    // DELETE CONFIRMATION
    // =========================================================

    private fun showDeleteDialog(
        routine: CareRoutine
    ) {

        AlertDialog.Builder(this)
            .setTitle(
                "Delete routine?"
            )
            .setMessage(
                "Delete ${routine.routineName} for ${routine.petName}?"
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Delete"
            ) { _, _ ->

                deleteRoutine(
                    routine
                )
            }
            .show()
    }

    // =========================================================
    // DELETE ROUTINE FROM FIRESTORE
    // =========================================================

    private fun deleteRoutine(
        routine: CareRoutine
    ) {

        if (routine.id.isEmpty()) {

            Toast.makeText(
                this,
                "Unable to delete routine: missing routine ID",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        db.collection("routines")
            .document(routine.id)
            .delete()
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Routine deleted successfully",
                    Toast.LENGTH_SHORT
                ).show()

                loadRoutines()
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Delete failed: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}