package com.example.petcare

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.petcare.data.Pet
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddPetActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var petId: String? = null
    private var selectedPhotoUri: String = ""

    private lateinit var tvFormTitle: TextView
    private lateinit var ivPetPhoto: ImageView

    private lateinit var layoutPetName: TextInputLayout
    private lateinit var layoutSpecies: TextInputLayout
    private lateinit var layoutBreed: TextInputLayout
    private lateinit var layoutAge: TextInputLayout
    private lateinit var layoutWeight: TextInputLayout

    private lateinit var etPetName: TextInputEditText
    private lateinit var etSpecies: TextInputEditText
    private lateinit var etBreed: TextInputEditText
    private lateinit var etAge: TextInputEditText
    private lateinit var etWeight: TextInputEditText

    private lateinit var etDiet: TextInputEditText
    private lateinit var etAllergies: TextInputEditText
    private lateinit var etVaccination: TextInputEditText
    private lateinit var etFavouriteToy: TextInputEditText
    private lateinit var etNotes: TextInputEditText

    private lateinit var btnChoosePhoto: MaterialButton
    private lateinit var btnSavePet: MaterialButton

    private lateinit var progressBar: ProgressBar


    // =========================================================
    // PHOTO PICKER
    // =========================================================

    private val photoPicker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->

            if (uri != null) {

                try {

                    contentResolver
                        .takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )

                } catch (_: Exception) {
                }

                selectedPhotoUri =
                    uri.toString()

                try {

                    ivPetPhoto.setImageURI(
                        uri
                    )

                } catch (_: Exception) {

                    ivPetPhoto.setImageResource(
                        R.drawable.petcare_hero
                    )
                }

                btnChoosePhoto.text =
                    "Change Photo"

                Toast.makeText(
                    this,
                    "Pet photo selected",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


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
            R.layout.activity_add_pet
        )


        // =====================================================
        // FIREBASE
        // =====================================================

        auth =
            FirebaseAuth.getInstance()

        db =
            FirebaseFirestore.getInstance()


        if (auth.currentUser == null) {

            Toast.makeText(
                this,
                "Please log in again",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }


        // =====================================================
        // CONNECT XML
        // =====================================================

        tvFormTitle =
            findViewById(
                R.id.tvFormTitle
            )

        ivPetPhoto =
            findViewById(
                R.id.ivPetPhoto
            )


        layoutPetName =
            findViewById(
                R.id.layoutPetName
            )

        layoutSpecies =
            findViewById(
                R.id.layoutSpecies
            )

        layoutBreed =
            findViewById(
                R.id.layoutBreed
            )

        layoutAge =
            findViewById(
                R.id.layoutAge
            )

        layoutWeight =
            findViewById(
                R.id.layoutWeight
            )


        etPetName =
            findViewById(
                R.id.etPetName
            )

        etSpecies =
            findViewById(
                R.id.etSpecies
            )

        etBreed =
            findViewById(
                R.id.etBreed
            )

        etAge =
            findViewById(
                R.id.etAge
            )

        etWeight =
            findViewById(
                R.id.etWeight
            )

        etDiet =
            findViewById(
                R.id.etDiet
            )

        etAllergies =
            findViewById(
                R.id.etAllergies
            )

        etVaccination =
            findViewById(
                R.id.etVaccination
            )

        etFavouriteToy =
            findViewById(
                R.id.etFavouriteToy
            )

        etNotes =
            findViewById(
                R.id.etNotes
            )


        val btnBack =
            findViewById<MaterialButton>(
                R.id.btnBack
            )

        btnChoosePhoto =
            findViewById(
                R.id.btnChoosePhoto
            )

        btnSavePet =
            findViewById(
                R.id.btnSavePet
            )

        progressBar =
            findViewById(
                R.id.petProgressBar
            )


        // =====================================================
        // BACK
        // =====================================================

        btnBack.setOnClickListener {

            finish()
        }


        // =====================================================
        // ADD OR EDIT MODE
        // =====================================================

        petId =
            intent.getStringExtra(
                "PET_ID"
            )


        if (petId.isNullOrBlank()) {

            // ADD MODE

            tvFormTitle.text =
                "Add New Pet"

            btnSavePet.text =
                "Save Pet Profile"

            ivPetPhoto.setImageResource(
                R.drawable.petcare_hero
            )

        } else {

            // EDIT MODE

            tvFormTitle.text =
                "Update Pet"

            btnSavePet.text =
                "Update Pet Profile"

            loadPetData(
                petId!!
            )
        }


        // =====================================================
        // CHOOSE PHOTO
        // =====================================================

        btnChoosePhoto.setOnClickListener {

            photoPicker.launch(
                arrayOf(
                    "image/*"
                )
            )
        }


        // =====================================================
        // SAVE
        // =====================================================

        btnSavePet.setOnClickListener {

            savePet()
        }
    }


    // =========================================================
    // LOAD EXISTING PET
    // =========================================================

    private fun loadPetData(
        id: String
    ) {

        progressBar.visibility =
            View.VISIBLE


        db.collection(
            "pets"
        )
            .document(
                id
            )
            .get()
            .addOnSuccessListener { document ->

                progressBar.visibility =
                    View.GONE


                if (!document.exists()) {

                    Toast.makeText(
                        this,
                        "Pet profile not found",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()

                    return@addOnSuccessListener
                }


                val pet =
                    document.toObject(
                        Pet::class.java
                    )


                if (pet == null) {

                    Toast.makeText(
                        this,
                        "Unable to load pet profile",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@addOnSuccessListener
                }


                val currentUser =
                    auth.currentUser


                if (
                    currentUser == null ||
                    (
                            pet.userId.isNotBlank() &&
                                    pet.userId != currentUser.uid
                            )
                ) {

                    Toast.makeText(
                        this,
                        "You do not have access to this pet",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()

                    return@addOnSuccessListener
                }


                // =============================================
                // PRE-FILL DATA
                // =============================================

                etPetName.setText(
                    pet.name
                )

                etSpecies.setText(
                    pet.species
                )

                etBreed.setText(
                    pet.breed
                )

                etAge.setText(
                    if (pet.age > 0) {
                        pet.age.toString()
                    } else {
                        ""
                    }
                )

                etWeight.setText(
                    if (pet.weight > 0) {
                        formatWeight(
                            pet.weight
                        )
                    } else {
                        ""
                    }
                )

                etDiet.setText(
                    pet.dietaryPreference
                )

                etAllergies.setText(
                    pet.allergies
                )

                etVaccination.setText(
                    pet.vaccinationHistory
                )

                etFavouriteToy.setText(
                    pet.favouriteToy
                )

                etNotes.setText(
                    pet.notes
                )


                selectedPhotoUri =
                    pet.photoUrl


                setPetPhotoPreview(
                    pet
                )
            }
            .addOnFailureListener { exception ->

                progressBar.visibility =
                    View.GONE

                Toast.makeText(
                    this,
                    "Unable to load pet: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    // =========================================================
    // PET PHOTO PREVIEW
    // =========================================================

    private fun setPetPhotoPreview(
        pet: Pet
    ) {

        fun fallbackPhoto() {

            when {

                pet.name.equals(
                    "Max",
                    ignoreCase = true
                ) -> {

                    ivPetPhoto.setImageResource(
                        R.drawable.max
                    )
                }


                pet.name.equals(
                    "Luna",
                    ignoreCase = true
                ) -> {

                    ivPetPhoto.setImageResource(
                        R.drawable.cat
                    )
                }


                pet.species.equals(
                    "Cat",
                    ignoreCase = true
                ) -> {

                    ivPetPhoto.setImageResource(
                        R.drawable.cat
                    )
                }


                else -> {

                    ivPetPhoto.setImageResource(
                        R.drawable.petcare_hero
                    )
                }
            }
        }


        if (pet.photoUrl.isBlank()) {

            fallbackPhoto()

            return
        }


        try {

            ivPetPhoto.setImageURI(
                Uri.parse(
                    pet.photoUrl
                )
            )


            if (
                ivPetPhoto.drawable == null
            ) {

                fallbackPhoto()

            } else {

                btnChoosePhoto.text =
                    "Change Photo"
            }

        } catch (_: Exception) {

            fallbackPhoto()
        }
    }


    // =========================================================
    // SAVE / UPDATE PET
    // =========================================================

    private fun savePet() {


        // =====================================================
        // CLEAR OLD ERRORS
        // =====================================================

        layoutPetName.error =
            null

        layoutSpecies.error =
            null

        layoutBreed.error =
            null

        layoutAge.error =
            null

        layoutWeight.error =
            null


        // =====================================================
        // READ INPUT
        // =====================================================

        val name =
            etPetName.text
                .toString()
                .trim()

        val species =
            etSpecies.text
                .toString()
                .trim()

        val breed =
            etBreed.text
                .toString()
                .trim()

        val ageText =
            etAge.text
                .toString()
                .trim()

        val weightText =
            etWeight.text
                .toString()
                .trim()

        val diet =
            etDiet.text
                .toString()
                .trim()

        val allergies =
            etAllergies.text
                .toString()
                .trim()

        val vaccination =
            etVaccination.text
                .toString()
                .trim()

        val favouriteToy =
            etFavouriteToy.text
                .toString()
                .trim()

        val notes =
            etNotes.text
                .toString()
                .trim()


        // =====================================================
        // VALIDATION
        // =====================================================

        var valid =
            true


        if (name.isEmpty()) {

            layoutPetName.error =
                "Pet name is required"

            valid =
                false
        }


        if (species.isEmpty()) {

            layoutSpecies.error =
                "Species is required"

            valid =
                false
        }


        if (breed.isEmpty()) {

            layoutBreed.error =
                "Breed is required"

            valid =
                false
        }


        val age =
            ageText.toIntOrNull()


        if (
            age == null ||
            age <= 0
        ) {

            layoutAge.error =
                "Enter a valid age"

            valid =
                false
        }


        val weight =
            weightText.toDoubleOrNull()


        if (
            weight == null ||
            weight <= 0
        ) {

            layoutWeight.error =
                "Enter a valid weight"

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
        // USER
        // =====================================================

        val currentUser =
            auth.currentUser


        if (currentUser == null) {

            Toast.makeText(
                this,
                "Please log in again",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        progressBar.visibility =
            View.VISIBLE

        btnSavePet.isEnabled =
            false


        // =====================================================
        // EDIT EXISTING PET
        // =====================================================

        if (!petId.isNullOrBlank()) {

            val updatedPet =
                Pet(
                    id = petId!!,
                    userId = currentUser.uid,
                    name = name,
                    species = species,
                    breed = breed,
                    age = age!!,
                    weight = weight!!,
                    dietaryPreference = diet,
                    allergies = allergies,
                    vaccinationHistory = vaccination,
                    favouriteToy = favouriteToy,
                    notes = notes,
                    photoUrl = selectedPhotoUri
                )


            db.collection(
                "pets"
            )
                .document(
                    petId!!
                )
                .set(
                    updatedPet
                )
                .addOnSuccessListener {

                    progressBar.visibility =
                        View.GONE

                    btnSavePet.isEnabled =
                        true

                    Toast.makeText(
                        this,
                        "Pet profile updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                }
                .addOnFailureListener { exception ->

                    progressBar.visibility =
                        View.GONE

                    btnSavePet.isEnabled =
                        true

                    Toast.makeText(
                        this,
                        "Update failed: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }


        } else {


            // =================================================
            // ADD NEW PET
            // =================================================

            val petDocument =
                db.collection(
                    "pets"
                )
                    .document()


            val newPetId =
                petDocument.id


            val newPet =
                Pet(
                    id = newPetId,
                    userId = currentUser.uid,
                    name = name,
                    species = species,
                    breed = breed,
                    age = age!!,
                    weight = weight!!,
                    dietaryPreference = diet,
                    allergies = allergies,
                    vaccinationHistory = vaccination,
                    favouriteToy = favouriteToy,
                    notes = notes,
                    photoUrl = selectedPhotoUri
                )


            petDocument
                .set(
                    newPet
                )
                .addOnSuccessListener {

                    progressBar.visibility =
                        View.GONE

                    btnSavePet.isEnabled =
                        true

                    Toast.makeText(
                        this,
                        "Pet profile saved successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                }
                .addOnFailureListener { exception ->

                    progressBar.visibility =
                        View.GONE

                    btnSavePet.isEnabled =
                        true

                    Toast.makeText(
                        this,
                        "Save failed: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }


    // =========================================================
    // FORMAT WEIGHT
    // =========================================================

    private fun formatWeight(
        weight: Double
    ): String {

        return if (
            weight % 1.0 == 0.0
        ) {

            weight.toInt()
                .toString()

        } else {

            String.format(
                "%.1f",
                weight
            )
        }
    }
}