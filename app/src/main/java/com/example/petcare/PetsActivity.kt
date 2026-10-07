package com.example.petcare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.petcare.data.Pet
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PetsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var petsContainer: LinearLayout
    private lateinit var emptyPetsState: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_pets)

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

        petsContainer =
            findViewById(
                R.id.petsContainer
            )

        emptyPetsState =
            findViewById(
                R.id.emptyPetsState
            )

        val btnAddPetTop =
            findViewById<MaterialButton>(
                R.id.btnAddPetTop
            )

        val btnAddPetBottom =
            findViewById<MaterialButton>(
                R.id.btnAddPetBottom
            )

        val bottomNavigation =
            findViewById<BottomNavigationView>(
                R.id.bottomNavigation
            )

        // =====================================================
        // ADD PET
        // =====================================================

        btnAddPetTop.setOnClickListener {

            openAddPet()
        }

        btnAddPetBottom.setOnClickListener {

            openAddPet()
        }

        // =====================================================
        // FINAL BOTTOM NAVIGATION
        //
        // Pets is a sub-page of Home.
        //
        // Home | Checklist | Routine | Delegate
        // =====================================================

        bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                // HOME
                R.id.nav_home -> {

                    val intent =
                        Intent(
                            this,
                            HomeActivity::class.java
                        )

                    intent.flags =
                        Intent.FLAG_ACTIVITY_CLEAR_TOP

                    startActivity(intent)

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

                else -> false
            }
        }

        /*
         * Pets is opened from:
         *
         * Home → View All Pets
         *
         * Therefore Home remains selected in bottom navigation.
         */

        bottomNavigation.menu
            .findItem(R.id.nav_home)
            .isChecked = true
    }

    // =========================================================
    // REFRESH PET LIST
    // =========================================================

    override fun onResume() {
        super.onResume()

        loadPets()
    }

    // =========================================================
    // OPEN ADD PET
    // =========================================================

    private fun openAddPet() {

        startActivity(
            Intent(
                this,
                AddPetActivity::class.java
            )
        )
    }

    // =========================================================
    // LOAD PETS FROM FIRESTORE
    // =========================================================

    private fun loadPets() {

        val currentUser =
            auth.currentUser ?: return

        db.collection("pets")
            .whereEqualTo(
                "userId",
                currentUser.uid
            )
            .get()
            .addOnSuccessListener { documents ->

                petsContainer.removeAllViews()

                // =================================================
                // NO PETS
                // =================================================

                if (documents.isEmpty) {

                    emptyPetsState.visibility =
                        View.VISIBLE

                    petsContainer.visibility =
                        View.GONE

                    return@addOnSuccessListener
                }

                // =================================================
                // PETS AVAILABLE
                // =================================================

                emptyPetsState.visibility =
                    View.GONE

                petsContainer.visibility =
                    View.VISIBLE

                for (document in documents) {

                    val pet =
                        document.toObject(
                            Pet::class.java
                        )

                    /*
                     * Extra safety:
                     * If old Firestore data does not contain id,
                     * use Firestore document ID.
                     */

                    if (pet.id.isEmpty()) {
                        pet.id = document.id
                    }

                    addPetCard(
                        pet
                    )
                }
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
    // CREATE PET CARD
    // =========================================================

    private fun addPetCard(
        pet: Pet
    ) {

        val petView =
            layoutInflater.inflate(
                R.layout.item_pet,
                petsContainer,
                false
            )

        val tvPetName =
            petView.findViewById<TextView>(
                R.id.tvPetName
            )

        val tvPetBreed =
            petView.findViewById<TextView>(
                R.id.tvPetBreed
            )

        val tvPetInfo =
            petView.findViewById<TextView>(
                R.id.tvPetInfo
            )

        val btnView =
            petView.findViewById<MaterialButton>(
                R.id.btnViewPet
            )

        val btnEdit =
            petView.findViewById<MaterialButton>(
                R.id.btnEditPet
            )

        val btnDelete =
            petView.findViewById<MaterialButton>(
                R.id.btnDeletePet
            )

        // =====================================================
        // DISPLAY PET INFORMATION
        // =====================================================

        tvPetName.text =
            pet.name

        tvPetBreed.text =
            if (pet.breed.isNotEmpty()) {

                pet.breed

            } else {

                pet.species
            }

        tvPetInfo.text =
            buildString {

                append(pet.species)

                append(" • Age ")

                append(pet.age)

                append(" • ")

                append(pet.weight)

                append(" kg")
            }

        // =====================================================
        // VIEW PET
        // =====================================================

        btnView.setOnClickListener {

            val intent =
                Intent(
                    this,
                    PetDetailActivity::class.java
                )

            intent.putExtra(
                "PET_ID",
                pet.id
            )

            startActivity(intent)
        }

        // =====================================================
        // EDIT PET
        // =====================================================

        btnEdit.setOnClickListener {

            val intent =
                Intent(
                    this,
                    AddPetActivity::class.java
                )

            intent.putExtra(
                "PET_ID",
                pet.id
            )

            startActivity(intent)
        }

        // =====================================================
        // DELETE PET
        // =====================================================

        btnDelete.setOnClickListener {

            showDeleteDialog(
                pet
            )
        }

        petsContainer.addView(
            petView
        )
    }

    // =========================================================
    // DELETE CONFIRMATION
    // =========================================================

    private fun showDeleteDialog(
        pet: Pet
    ) {

        AlertDialog.Builder(this)
            .setTitle(
                "Delete ${pet.name}?"
            )
            .setMessage(
                "Are you sure you want to delete this pet profile?"
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Delete"
            ) { _, _ ->

                deletePet(
                    pet
                )
            }
            .show()
    }

    // =========================================================
    // DELETE PET FROM FIRESTORE
    // =========================================================

    private fun deletePet(
        pet: Pet
    ) {

        if (pet.id.isEmpty()) {

            Toast.makeText(
                this,
                "Unable to delete pet: missing pet ID",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        db.collection("pets")
            .document(pet.id)
            .delete()
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "${pet.name} deleted successfully",
                    Toast.LENGTH_SHORT
                ).show()

                loadPets()
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