package com.example.petcare

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.petcare.data.Pet
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PetDetailActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var ivPetPhotoDetail: ImageView
    private lateinit var tvPetNameDetail: TextView
    private lateinit var tvPetSubtitle: TextView
    private lateinit var tvSpecies: TextView
    private lateinit var tvAge: TextView
    private lateinit var tvWeight: TextView
    private lateinit var tvDiet: TextView
    private lateinit var tvAllergies: TextView
    private lateinit var tvVaccination: TextView
    private lateinit var tvFavouriteToy: TextView
    private lateinit var tvNotes: TextView
    private lateinit var cardNotes: MaterialCardView

    private var petId: String = ""
    private var currentPet: Pet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pet_detail)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        petId = intent.getStringExtra("PET_ID").orEmpty()

        if (petId.isBlank()) {
            Toast.makeText(
                this,
                "Unable to open pet profile",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        val btnBack =
            findViewById<MaterialButton>(
                R.id.btnBack
            )

        val btnEditTop =
            findViewById<MaterialButton>(
                R.id.btnEditTop
            )

        val btnEditPetDetail =
            findViewById<MaterialButton>(
                R.id.btnEditPetDetail
            )

        val btnDeletePetDetail =
            findViewById<MaterialButton>(
                R.id.btnDeletePetDetail
            )

        ivPetPhotoDetail =
            findViewById(
                R.id.ivPetPhotoDetail
            )

        tvPetNameDetail =
            findViewById(
                R.id.tvPetNameDetail
            )

        tvPetSubtitle =
            findViewById(
                R.id.tvPetSubtitle
            )

        tvSpecies =
            findViewById(
                R.id.tvSpecies
            )

        tvAge =
            findViewById(
                R.id.tvAge
            )

        tvWeight =
            findViewById(
                R.id.tvWeight
            )

        tvDiet =
            findViewById(
                R.id.tvDiet
            )

        tvAllergies =
            findViewById(
                R.id.tvAllergies
            )

        tvVaccination =
            findViewById(
                R.id.tvVaccination
            )

        tvFavouriteToy =
            findViewById(
                R.id.tvFavouriteToy
            )

        tvNotes =
            findViewById(
                R.id.tvNotes
            )

        cardNotes =
            findViewById(
                R.id.cardNotes
            )

        btnBack.setOnClickListener {
            finish()
        }

        btnEditTop.setOnClickListener {
            openEditPet()
        }

        btnEditPetDetail.setOnClickListener {
            openEditPet()
        }

        btnDeletePetDetail.setOnClickListener {
            showDeleteDialog()
        }
    }

    override fun onResume() {
        super.onResume()

        if (petId.isNotBlank()) {
            loadPet()
        }
    }

    private fun loadPet() {

        val currentUser =
            auth.currentUser

        if (currentUser == null) {
            Toast.makeText(
                this,
                "Please log in again",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        db.collection("pets")
            .document(petId)
            .get()
            .addOnSuccessListener { document ->

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
                        "Unable to read pet profile",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@addOnSuccessListener
                }

                if (
                    pet.userId.isNotBlank() &&
                    pet.userId != currentUser.uid
                ) {

                    Toast.makeText(
                        this,
                        "You do not have access to this pet",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                    return@addOnSuccessListener
                }

                if (pet.id.isBlank()) {
                    pet.id = document.id
                }

                currentPet = pet

                displayPet(
                    pet
                )
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Unable to load pet: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun displayPet(
        pet: Pet
    ) {

        tvPetNameDetail.text =
            valueOrDefault(
                pet.name,
                "Pet"
            )

        val breedText =
            valueOrDefault(
                pet.breed,
                pet.species
            )

        val speciesText =
            valueOrDefault(
                pet.species,
                "Not recorded"
            )

        tvPetSubtitle.text =
            "$breedText • $speciesText"

        tvSpecies.text =
            speciesText

        tvAge.text =
            if (pet.age > 0) {
                "${pet.age} yrs"
            } else {
                "Not set"
            }

        tvWeight.text =
            if (pet.weight > 0) {
                "${formatWeight(pet.weight)} kg"
            } else {
                "Not set"
            }

        tvDiet.text =
            valueOrDefault(
                pet.dietaryPreference,
                "Not recorded"
            )

        tvAllergies.text =
            valueOrDefault(
                pet.allergies,
                "None recorded"
            )

        tvVaccination.text =
            valueOrDefault(
                pet.vaccinationHistory,
                "Not recorded"
            )

        tvFavouriteToy.text =
            valueOrDefault(
                pet.favouriteToy,
                "Not recorded"
            )

        if (pet.notes.isBlank()) {

            cardNotes.visibility =
                View.GONE

        } else {

            cardNotes.visibility =
                View.VISIBLE

            tvNotes.text =
                pet.notes
        }

        setPetPhoto(
            pet
        )
    }

    private fun setPetPhoto(
        pet: Pet
    ) {

        fun setFallbackPhoto() {

            when {

                pet.name.equals(
                    "Max",
                    ignoreCase = true
                ) -> {
                    ivPetPhotoDetail.setImageResource(
                        R.drawable.max
                    )
                }

                pet.name.equals(
                    "Luna",
                    ignoreCase = true
                ) -> {
                    ivPetPhotoDetail.setImageResource(
                        R.drawable.cat
                    )
                }

                pet.species.equals(
                    "Cat",
                    ignoreCase = true
                ) -> {
                    ivPetPhotoDetail.setImageResource(
                        R.drawable.cat
                    )
                }

                else -> {
                    ivPetPhotoDetail.setImageResource(
                        R.drawable.petcare_hero
                    )
                }
            }
        }

        if (pet.photoUrl.isBlank()) {

            setFallbackPhoto()
            return
        }

        try {

            ivPetPhotoDetail.setImageURI(
                Uri.parse(
                    pet.photoUrl
                )
            )

            if (ivPetPhotoDetail.drawable == null) {
                setFallbackPhoto()
            }

        } catch (_: Exception) {

            setFallbackPhoto()
        }
    }

    private fun openEditPet() {

        if (petId.isBlank()) {
            return
        }

        val intent =
            Intent(
                this,
                AddPetActivity::class.java
            )

        intent.putExtra(
            "PET_ID",
            petId
        )

        startActivity(
            intent
        )
    }

    private fun showDeleteDialog() {

        val pet =
            currentPet
                ?: return

        AlertDialog.Builder(this)
            .setTitle(
                "Delete ${pet.name}?"
            )
            .setMessage(
                "Are you sure you want to delete this pet profile? This action cannot be undone."
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Delete"
            ) { _, _ ->

                deletePet()
            }
            .show()
    }

    private fun deletePet() {

        if (petId.isBlank()) {
            return
        }

        db.collection("pets")
            .document(petId)
            .delete()
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Pet profile deleted successfully",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Delete failed: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun valueOrDefault(
        value: String,
        fallback: String
    ): String {

        return if (value.isBlank()) {
            fallback
        } else {
            value
        }
    }

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
