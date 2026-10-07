package com.example.petcare

import android.content.Intent
import android.net.Uri
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.petcare.data.Pet
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var petsContainer: LinearLayout
    private lateinit var emptyPetsState: MaterialCardView

    private lateinit var tvTotalPets: TextView
    private lateinit var tvTodayTasks: TextView
    private lateinit var tvCompletedTasks: TextView

    private lateinit var tvCareStatus: TextView
    private lateinit var tvCareSubtitle: TextView
    private lateinit var todayCareContainer: LinearLayout

    private data class TodayCareItem(
        val taskName: String,
        val petName: String,
        val category: String,
        val time: String,
        val completed: Boolean
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser

        if (currentUser == null) {

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

        val tvGreeting =
            findViewById<TextView>(
                R.id.tvGreeting
            )

        tvTotalPets =
            findViewById(
                R.id.tvTotalPets
            )

        tvTodayTasks =
            findViewById(
                R.id.tvTodayTasks
            )

        tvCompletedTasks =
            findViewById(
                R.id.tvCompletedTasks
            )

        petsContainer =
            findViewById(
                R.id.petsContainer
            )

        emptyPetsState =
            findViewById(
                R.id.emptyPetsState
            )

        tvCareStatus =
            findViewById(
                R.id.tvCareStatus
            )

        tvCareSubtitle =
            findViewById(
                R.id.tvCareSubtitle
            )

        todayCareContainer =
            findViewById(
                R.id.todayCareContainer
            )

        val btnAddPet =
            findViewById<MaterialButton>(
                R.id.btnAddPet
            )

        val btnViewPets =
            findViewById<MaterialButton>(
                R.id.btnViewPets
            )

        val btnViewChecklist =
            findViewById<MaterialButton>(
                R.id.btnViewChecklist
            )

        val btnLogout =
            findViewById<MaterialButton>(
                R.id.btnLogout
            )

        val bottomNavigation =
            findViewById<BottomNavigationView>(
                R.id.bottomNavigation
            )

        tvTotalPets.text = "0"
        tvTodayTasks.text = "0"
        tvCompletedTasks.text = "0"

        db.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->

                val fullName =
                    document.getString(
                        "fullName"
                    ) ?: "Pet Parent"

                tvGreeting.text =
                    "Hello, $fullName 👋"
            }
            .addOnFailureListener {

                tvGreeting.text =
                    "Hello, Pet Parent 👋"
            }

        btnAddPet.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    AddPetActivity::class.java
                )
            )
        }

        btnViewPets.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    PetsActivity::class.java
                )
            )
        }

        btnViewChecklist.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    TasksActivity::class.java
                )
            )
        }

        // =====================================================
        // BOTTOM NAVIGATION
        // =====================================================

        bottomNavigation.selectedItemId =
            R.id.nav_home

        bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_home -> {
                    true
                }

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

        btnLogout.setOnClickListener {

            auth.signOut()

            Toast.makeText(
                this,
                "Logged out successfully",
                Toast.LENGTH_SHORT
            ).show()

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
        }
    }

    override fun onResume() {
        super.onResume()

        loadPets()
        loadTodaySummary()
    }

    private fun loadPets() {

        val userId =
            auth.currentUser?.uid ?: return

        db.collection("pets")
            .whereEqualTo(
                "userId",
                userId
            )
            .get()
            .addOnSuccessListener { documents ->

                petsContainer.removeAllViews()

                tvTotalPets.text =
                    documents.size().toString()

                if (documents.isEmpty) {

                    emptyPetsState.visibility =
                        View.VISIBLE

                    petsContainer.visibility =
                        View.GONE

                    return@addOnSuccessListener
                }

                emptyPetsState.visibility =
                    View.GONE

                petsContainer.visibility =
                    View.VISIBLE

                for (document in documents) {

                    val pet =
                        document.toObject(
                            Pet::class.java
                        )

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

    private fun loadTodaySummary() {

        val userId =
            auth.currentUser?.uid ?: return

        val todayDate =
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            ).format(
                Date()
            )

        val todayDay =
            SimpleDateFormat(
                "EEE",
                Locale.ENGLISH
            ).format(
                Date()
            )

        db.collection("routines")
            .whereEqualTo(
                "userId",
                userId
            )
            .get()
            .addOnSuccessListener { documents ->

                var totalTasks =
                    0

                var completedTasks =
                    0

                val todayItems =
                    mutableListOf<TodayCareItem>()

                for (document in documents) {

                    val repeatDays =
                        document.get(
                            "repeatDays"
                        ) as? List<*>
                            ?: emptyList<String>()

                    if (!repeatDays.contains(todayDay)) {
                        continue
                    }

                    val taskName =
                        document.getString(
                            "taskName"
                        ) ?: "Care Task"

                    val petName =
                        document.getString(
                            "petName"
                        ) ?: "Pet"

                    val category =
                        document.getString(
                            "category"
                        ) ?: "Care"

                    val time =
                        document.getString(
                            "time"
                        ) ?: ""

                    val lastCompletedDate =
                        document.getString(
                            "lastCompletedDate"
                        ) ?: ""

                    val completed =
                        lastCompletedDate ==
                                todayDate

                    totalTasks++

                    if (completed) {
                        completedTasks++
                    }

                    todayItems.add(
                        TodayCareItem(
                            taskName =
                                taskName,

                            petName =
                                petName,

                            category =
                                category,

                            time =
                                time,

                            completed =
                                completed
                        )
                    )
                }

                tvTodayTasks.text =
                    totalTasks.toString()

                tvCompletedTasks.text =
                    completedTasks.toString()

                renderTodayCare(
                    todayItems
                )
            }
            .addOnFailureListener { exception ->

                tvCareStatus.text =
                    "Unable to load today's care"

                tvCareSubtitle.text =
                    "Please check your connection and try again."

                Toast.makeText(
                    this,
                    "Unable to load task summary: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun renderTodayCare(
        items: List<TodayCareItem>
    ) {

        todayCareContainer.removeAllViews()

        if (items.isEmpty()) {

            tvCareStatus.text =
                "No care tasks yet"

            tvCareSubtitle.text =
                "Create a care routine to build today's checklist."

            todayCareContainer.visibility =
                View.GONE

            return
        }

        val completedCount =
            items.count {
                it.completed
            }

        val remainingCount =
            items.size -
                    completedCount

        tvCareStatus.text =
            "$completedCount of ${items.size} tasks completed"

        tvCareSubtitle.text =
            when {

                remainingCount == 0 ->
                    "All done for today. Great job caring for your pets!"

                remainingCount == 1 ->
                    "1 care task remaining today."

                else ->
                    "$remainingCount care tasks remaining today."
            }

        todayCareContainer.visibility =
            View.VISIBLE

        for (item in items) {

            addTodayCareItem(
                item
            )
        }
    }

    private fun addTodayCareItem(
        item: TodayCareItem
    ) {

        val card =
            MaterialCardView(
                this
            )

        val cardParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        cardParams.topMargin =
            dp(
                10
            )

        card.layoutParams =
            cardParams

        card.radius =
            dp(
                14
            ).toFloat()

        card.cardElevation =
            dp(
                1
            ).toFloat()

        card.strokeWidth =
            dp(
                1
            )

        card.strokeColor =
            ContextCompat.getColor(
                this,
                if (item.completed) {
                    R.color.pet_success
                } else {
                    R.color.pet_primary
                }
            )

        card.setCardBackgroundColor(
            ContextCompat.getColor(
                this,
                R.color.pet_surface
            )
        )

        card.isClickable =
            true

        card.isFocusable =
            true

        val row =
            LinearLayout(
                this
            )

        row.orientation =
            LinearLayout.HORIZONTAL

        row.gravity =
            Gravity.CENTER_VERTICAL

        row.setPadding(
            dp(
                14
            ),
            dp(
                13
            ),
            dp(
                14
            ),
            dp(
                13
            )
        )

        val statusIcon =
            TextView(
                this
            )

        statusIcon.text =
            if (item.completed) {
                "✓"
            } else {
                "○"
            }

        statusIcon.textSize =
            23f

        statusIcon.setTypeface(
            null,
            Typeface.BOLD
        )

        statusIcon.gravity =
            Gravity.CENTER

        statusIcon.setTextColor(
            ContextCompat.getColor(
                this,
                if (item.completed) {
                    R.color.pet_success
                } else {
                    R.color.pet_primary
                }
            )
        )

        statusIcon.layoutParams =
            LinearLayout.LayoutParams(
                dp(
                    34
                ),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        val detailContainer =
            LinearLayout(
                this
            )

        detailContainer.orientation =
            LinearLayout.VERTICAL

        val detailParams =
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

        detailParams.marginStart =
            dp(
                8
            )

        detailContainer.layoutParams =
            detailParams

        val taskName =
            TextView(
                this
            )

        taskName.text =
            item.taskName

        taskName.textSize =
            15f

        taskName.setTypeface(
            null,
            Typeface.BOLD
        )

        taskName.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.pet_text
            )
        )

        val petAndTime =
            TextView(
                this
            )

        petAndTime.text =
            if (item.time.isEmpty()) {
                item.petName
            } else {
                "${item.petName} • ${item.time}"
            }

        petAndTime.textSize =
            13f

        petAndTime.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.pet_text_secondary
            )
        )

        val status =
            TextView(
                this
            )

        status.text =
            "${item.category} • ${
                if (item.completed) {
                    "Completed"
                } else {
                    "Pending"
                }
            }"

        status.textSize =
            12f

        status.setTextColor(
            ContextCompat.getColor(
                this,
                if (item.completed) {
                    R.color.pet_success
                } else {
                    R.color.pet_primary
                }
            )
        )

        detailContainer.addView(
            taskName
        )

        detailContainer.addView(
            petAndTime
        )

        detailContainer.addView(
            status
        )

        row.addView(
            statusIcon
        )

        row.addView(
            detailContainer
        )

        card.addView(
            row
        )

        card.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    TasksActivity::class.java
                )
            )
        }

        todayCareContainer.addView(
            card
        )
    }

    private fun addPetCard(
        pet: Pet
    ) {

        val petView =
            layoutInflater.inflate(
                R.layout.item_pet,
                petsContainer,
                false
            )

        val ivPetPhoto =
            petView.findViewById<ImageView>(
                R.id.ivPetPhoto
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

        tvPetName.text =
            pet.name

        tvPetBreed.text =
            if (pet.breed.isNotEmpty()) {
                pet.breed
            } else {
                pet.species
            }

        tvPetInfo.text =
            "${pet.species} • Age ${pet.age} • ${pet.weight} kg"

        fun setFallbackPhoto() {

            when {

                pet.name.equals(
                    "Max",
                    ignoreCase =
                        true
                ) -> {

                    ivPetPhoto.setImageResource(
                        R.drawable.max
                    )
                }

                pet.name.equals(
                    "Luna",
                    ignoreCase =
                        true
                ) -> {

                    ivPetPhoto.setImageResource(
                        R.drawable.cat
                    )
                }

                pet.species.equals(
                    "Cat",
                    ignoreCase =
                        true
                ) -> {

                    ivPetPhoto.setImageResource(
                        R.drawable.cat
                    )
                }

                else -> {

                    ivPetPhoto.setImageResource(
                        R.drawable.ic_pets_24
                    )
                }
            }
        }

        if (pet.photoUrl.isNotBlank()) {

            try {

                ivPetPhoto.setImageURI(
                    Uri.parse(
                        pet.photoUrl
                    )
                )

                if (ivPetPhoto.drawable == null) {

                    setFallbackPhoto()
                }

            } catch (_: Exception) {

                setFallbackPhoto()
            }

        } else {

            setFallbackPhoto()
        }

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

            startActivity(
                intent
            )
        }

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

            startActivity(
                intent
            )
        }

        btnDelete.setOnClickListener {

            showDeleteDialog(
                pet
            )
        }

        petsContainer.addView(
            petView
        )
    }

    private fun showDeleteDialog(
        pet: Pet
    ) {

        AlertDialog.Builder(
            this
        )
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

    private fun deletePet(
        pet: Pet
    ) {

        db.collection(
            "pets"
        )
            .document(
                pet.id
            )
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

    private fun dp(
        value: Int
    ): Int {

        return (
                value *
                        resources.displayMetrics.density
                ).toInt()
    }
}