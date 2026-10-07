package com.example.petcare

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
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
import com.google.firebase.firestore.SetOptions
import java.util.Calendar
import java.util.Locale

class AddRoutineActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // =========================================================
    // DROPDOWNS
    // =========================================================

    private lateinit var actPet: AutoCompleteTextView
    private lateinit var actCategory: AutoCompleteTextView
    private lateinit var actFrequency: AutoCompleteTextView

    // =========================================================
    // TEXT FIELDS
    // =========================================================

    private lateinit var etRoutineName: TextInputEditText
    private lateinit var etTaskName: TextInputEditText
    private lateinit var etSupplies: TextInputEditText
    private lateinit var etInstructions: TextInputEditText
    private lateinit var etNotes: TextInputEditText

    // =========================================================
    // VALIDATION LAYOUTS
    // =========================================================

    private lateinit var layoutPet: TextInputLayout
    private lateinit var layoutRoutineName: TextInputLayout
    private lateinit var layoutTaskName: TextInputLayout
    private lateinit var layoutCategory: TextInputLayout
    private lateinit var layoutFrequency: TextInputLayout

    // =========================================================
    // BUTTONS
    // =========================================================

    private lateinit var btnBack: MaterialButton
    private lateinit var btnTime: MaterialButton
    private lateinit var btnAddPhoto: MaterialButton
    private lateinit var btnSaveRoutine: MaterialButton

    // =========================================================
    // OTHER UI
    // =========================================================

    private lateinit var tvScreenTitle: TextView
    private lateinit var progressBar: ProgressBar

    // =========================================================
    // REPEAT DAYS
    // =========================================================

    private lateinit var cbSun: CheckBox
    private lateinit var cbMon: CheckBox
    private lateinit var cbTue: CheckBox
    private lateinit var cbWed: CheckBox
    private lateinit var cbThu: CheckBox
    private lateinit var cbFri: CheckBox
    private lateinit var cbSat: CheckBox

    // =========================================================
    // PET DATA
    // =========================================================

    private val pets =
        mutableListOf<Pet>()

    private var selectedPetId = ""
    private var selectedPetName = ""

    // =========================================================
    // ROUTINE DATA
    // =========================================================

    // Example:
    // 09:00 AM
    // 06:00 PM
    private var selectedTime = ""

    private var photoUrl = ""

    // Empty = ADD
    // Has ID = EDIT
    private var routineId = ""

    private var existingCreatedAt =
        System.currentTimeMillis()

    // =========================================================
    // PHOTO PICKER
    // =========================================================

    private val photoPicker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri != null) {

                photoUrl =
                    uri.toString()

                try {

                    contentResolver
                        .takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )

                } catch (_: Exception) {
                    // Ignore permission exception
                }

                btnAddPhoto.text =
                    "PHOTO SELECTED ✓"
            }
        }

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_add_routine
        )

        // =====================================================
        // FIREBASE
        // =====================================================

        auth =
            FirebaseAuth.getInstance()

        db =
            FirebaseFirestore.getInstance()

        // =====================================================
        // CONNECT XML
        // =====================================================

        tvScreenTitle =
            findViewById(
                R.id.tvScreenTitle
            )

        btnBack =
            findViewById(
                R.id.btnBack
            )

        actPet =
            findViewById(
                R.id.actPet
            )

        actCategory =
            findViewById(
                R.id.actCategory
            )

        actFrequency =
            findViewById(
                R.id.actFrequency
            )

        etRoutineName =
            findViewById(
                R.id.etRoutineName
            )

        etTaskName =
            findViewById(
                R.id.etTaskName
            )

        etSupplies =
            findViewById(
                R.id.etSupplies
            )

        etInstructions =
            findViewById(
                R.id.etInstructions
            )

        etNotes =
            findViewById(
                R.id.etNotes
            )

        layoutPet =
            findViewById(
                R.id.layoutPet
            )

        layoutRoutineName =
            findViewById(
                R.id.layoutRoutineName
            )

        layoutTaskName =
            findViewById(
                R.id.layoutTaskName
            )

        layoutCategory =
            findViewById(
                R.id.layoutCategory
            )

        layoutFrequency =
            findViewById(
                R.id.layoutFrequency
            )

        btnTime =
            findViewById(
                R.id.btnTime
            )

        btnAddPhoto =
            findViewById(
                R.id.btnAddPhoto
            )

        btnSaveRoutine =
            findViewById(
                R.id.btnSaveRoutine
            )

        progressBar =
            findViewById(
                R.id.progressBar
            )

        cbSun =
            findViewById(
                R.id.cbSun
            )

        cbMon =
            findViewById(
                R.id.cbMon
            )

        cbTue =
            findViewById(
                R.id.cbTue
            )

        cbWed =
            findViewById(
                R.id.cbWed
            )

        cbThu =
            findViewById(
                R.id.cbThu
            )

        cbFri =
            findViewById(
                R.id.cbFri
            )

        cbSat =
            findViewById(
                R.id.cbSat
            )

        // =====================================================
        // CHECK LOGIN
        // =====================================================

        if (auth.currentUser == null) {

            Toast.makeText(
                this,
                "Please log in first",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }

        // =====================================================
        // SETUP DROPDOWNS
        // =====================================================

        setupCategoryDropdown()

        setupFrequencyDropdown()

        // =====================================================
        // CHECK ADD / EDIT MODE
        // =====================================================

        routineId =
            intent.getStringExtra(
                "ROUTINE_ID"
            ) ?: ""

        if (routineId.isEmpty()) {

            // =================================================
            // ADD MODE
            // =================================================

            tvScreenTitle.text =
                "New Care Routine"

            btnSaveRoutine.text =
                "SAVE ROUTINE"

            val petId =
                intent.getStringExtra(
                    "PET_ID"
                )

            loadPets(
                petId
            )

        } else {

            // =================================================
            // EDIT MODE
            // =================================================

            tvScreenTitle.text =
                "Edit Care Routine"

            btnSaveRoutine.text =
                "UPDATE ROUTINE"

            loadRoutineForEdit()
        }

        // =====================================================
        // BACK
        // =====================================================

        btnBack.setOnClickListener {

            finish()
        }

        // =====================================================
        // TIME
        // =====================================================

        btnTime.setOnClickListener {

            showTimePicker()
        }

        // =====================================================
        // PHOTO
        // =====================================================

        btnAddPhoto.setOnClickListener {

            photoPicker.launch(
                arrayOf(
                    "image/*"
                )
            )
        }

        // =====================================================
        // SAVE / UPDATE
        // =====================================================

        btnSaveRoutine.setOnClickListener {

            saveRoutine()
        }
    }

    // =========================================================
    // CATEGORY DROPDOWN
    // =========================================================

    private fun setupCategoryDropdown() {

        val categories =
            listOf(
                "Feeding",
                "Exercise",
                "Grooming",
                "Medication",
                "Cleaning",
                "Healthcare"
            )

        val adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                categories
            )

        actCategory.setAdapter(
            adapter
        )
    }

    // =========================================================
    // FREQUENCY DROPDOWN
    // =========================================================

    private fun setupFrequencyDropdown() {

        val frequencies =
            listOf(
                "Daily",
                "Weekly",
                "Monthly",
                "Custom"
            )

        val adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                frequencies
            )

        actFrequency.setAdapter(
            adapter
        )

        actFrequency
            .setOnItemClickListener { _, _, position, _ ->

                if (
                    frequencies[position] == "Daily"
                ) {

                    // Daily selected means every day
                    setAllDaysChecked(
                        true
                    )
                }
            }
    }

    // =========================================================
    // LOAD PETS
    // =========================================================

    private fun loadPets(
        petIdToSelect: String? = null
    ) {

        val userId =
            auth.currentUser?.uid
                ?: return

        db.collection("pets")
            .whereEqualTo(
                "userId",
                userId
            )
            .get()
            .addOnSuccessListener { documents ->

                pets.clear()

                for (document in documents) {

                    val pet =
                        document.toObject(
                            Pet::class.java
                        )

                    pets.add(
                        pet
                    )
                }

                if (pets.isEmpty()) {

                    Toast.makeText(
                        this,
                        "Please add a pet first",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnSuccessListener
                }

                val petNames =
                    pets.map { pet ->

                        "${pet.name} (${pet.species})"
                    }

                val adapter =
                    ArrayAdapter(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        petNames
                    )

                actPet.setAdapter(
                    adapter
                )

                // =================================================
                // USER SELECTS PET
                // =================================================

                actPet
                    .setOnItemClickListener { _, _, position, _ ->

                        val pet =
                            pets[position]

                        selectedPetId =
                            pet.id

                        selectedPetName =
                            pet.name

                        layoutPet.error =
                            null
                    }

                // =================================================
                // PRESELECT PET IN EDIT MODE
                // =================================================

                if (
                    !petIdToSelect.isNullOrEmpty()
                ) {

                    val index =
                        pets.indexOfFirst { pet ->

                            pet.id ==
                                    petIdToSelect
                        }

                    if (index >= 0) {

                        val pet =
                            pets[index]

                        selectedPetId =
                            pet.id

                        selectedPetName =
                            pet.name

                        actPet.setText(
                            petNames[index],
                            false
                        )
                    }
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
    // TIME PICKER
    // =========================================================

    private fun showTimePicker() {

        val calendar =
            Calendar.getInstance()

        val hour =
            calendar.get(
                Calendar.HOUR_OF_DAY
            )

        val minute =
            calendar.get(
                Calendar.MINUTE
            )

        val dialog =
            TimePickerDialog(
                this,

                { _, selectedHour, selectedMinute ->

                    // =================================================
                    // AM / PM
                    // =================================================

                    val amPm =
                        if (selectedHour < 12) {

                            "AM"

                        } else {

                            "PM"
                        }

                    // =================================================
                    // CONVERT 24 HOUR TO 12 HOUR
                    // =================================================

                    val hour12 =
                        when {

                            selectedHour == 0 ->
                                12

                            selectedHour > 12 ->
                                selectedHour - 12

                            else ->
                                selectedHour
                        }

                    selectedTime =
                        String.format(
                            Locale.getDefault(),
                            "%02d:%02d %s",
                            hour12,
                            selectedMinute,
                            amPm
                        )

                    btnTime.text =
                        selectedTime
                },

                hour,

                minute,

                // false = 12-hour picker with AM/PM
                false
            )

        dialog.show()
    }

    // =========================================================
    // CONVERT OLD TIME FORMAT TO AM / PM
    // =========================================================

    private fun convertTo12HourFormat(
        time: String
    ): String {

        val value =
            time.trim()

        if (value.isEmpty()) {

            return ""
        }

        // =====================================================
        // ALREADY AM / PM
        // =====================================================

        if (
            value.contains(
                "AM",
                ignoreCase = true
            ) ||
            value.contains(
                "PM",
                ignoreCase = true
            )
        ) {

            return value.uppercase(
                Locale.getDefault()
            )
        }

        // =====================================================
        // OLD FIREBASE FORMAT
        // Example:
        // 07:00
        // 18:30
        // =====================================================

        val parts =
            value.split(":")

        if (parts.size < 2) {

            return value
        }

        val hour24 =
            parts[0].toIntOrNull()
                ?: return value

        val minute =
            parts[1].toIntOrNull()
                ?: return value

        val amPm =
            if (hour24 < 12) {

                "AM"

            } else {

                "PM"
            }

        val hour12 =
            when {

                hour24 == 0 ->
                    12

                hour24 > 12 ->
                    hour24 - 12

                else ->
                    hour24
            }

        return String.format(
            Locale.getDefault(),
            "%02d:%02d %s",
            hour12,
            minute,
            amPm
        )
    }

    // =========================================================
    // GET SELECTED REPEAT DAYS
    // =========================================================

    private fun getSelectedDays():
            List<String> {

        val days =
            mutableListOf<String>()

        if (cbSun.isChecked) {

            days.add("Sun")
        }

        if (cbMon.isChecked) {

            days.add("Mon")
        }

        if (cbTue.isChecked) {

            days.add("Tue")
        }

        if (cbWed.isChecked) {

            days.add("Wed")
        }

        if (cbThu.isChecked) {

            days.add("Thu")
        }

        if (cbFri.isChecked) {

            days.add("Fri")
        }

        if (cbSat.isChecked) {

            days.add("Sat")
        }

        return days
    }

    // =========================================================
    // SELECT ALL DAYS
    // =========================================================

    private fun setAllDaysChecked(
        checked: Boolean
    ) {

        cbSun.isChecked =
            checked

        cbMon.isChecked =
            checked

        cbTue.isChecked =
            checked

        cbWed.isChecked =
            checked

        cbThu.isChecked =
            checked

        cbFri.isChecked =
            checked

        cbSat.isChecked =
            checked
    }

    // =========================================================
    // SAVE / UPDATE ROUTINE
    // =========================================================

    private fun saveRoutine() {

        val userId =
            auth.currentUser?.uid
                ?: return

        val routineName =
            etRoutineName.text
                ?.toString()
                ?.trim()
                .orEmpty()

        val taskName =
            etTaskName.text
                ?.toString()
                ?.trim()
                .orEmpty()

        val category =
            actCategory.text
                .toString()
                .trim()

        val frequency =
            actFrequency.text
                .toString()
                .trim()

        val supplies =
            etSupplies.text
                ?.toString()
                ?.trim()
                .orEmpty()

        val instructions =
            etInstructions.text
                ?.toString()
                ?.trim()
                .orEmpty()

        val notes =
            etNotes.text
                ?.toString()
                ?.trim()
                .orEmpty()

        val repeatDays =
            getSelectedDays()

        // =====================================================
        // CLEAR OLD ERRORS
        // =====================================================

        layoutPet.error =
            null

        layoutRoutineName.error =
            null

        layoutTaskName.error =
            null

        layoutCategory.error =
            null

        layoutFrequency.error =
            null

        var valid =
            true

        // =====================================================
        // PET VALIDATION
        // =====================================================

        if (selectedPetId.isEmpty()) {

            layoutPet.error =
                "Please select a pet"

            valid =
                false
        }

        // =====================================================
        // ROUTINE NAME VALIDATION
        // =====================================================

        if (routineName.isEmpty()) {

            layoutRoutineName.error =
                "Routine name is required"

            valid =
                false
        }

        // =====================================================
        // TASK NAME VALIDATION
        // =====================================================

        if (taskName.isEmpty()) {

            layoutTaskName.error =
                "Task name is required"

            valid =
                false
        }

        // =====================================================
        // CATEGORY VALIDATION
        // =====================================================

        if (category.isEmpty()) {

            layoutCategory.error =
                "Please select a category"

            valid =
                false
        }

        // =====================================================
        // TIME VALIDATION
        // =====================================================

        if (selectedTime.isEmpty()) {

            Toast.makeText(
                this,
                "Please select a time",
                Toast.LENGTH_SHORT
            ).show()

            valid =
                false
        }

        // =====================================================
        // FREQUENCY VALIDATION
        // =====================================================

        if (frequency.isEmpty()) {

            layoutFrequency.error =
                "Please select a frequency"

            valid =
                false
        }

        // =====================================================
        // REPEAT DAYS VALIDATION
        // =====================================================

        if (repeatDays.isEmpty()) {

            Toast.makeText(
                this,
                "Please select at least one repeat day",
                Toast.LENGTH_SHORT
            ).show()

            valid =
                false
        }

        if (!valid) {

            return
        }

        // =====================================================
        // LOADING
        // =====================================================

        progressBar.visibility =
            View.VISIBLE

        btnSaveRoutine.isEnabled =
            false

        // =====================================================
        // FIRESTORE DOCUMENT
        // =====================================================

        val routineDocument =
            if (routineId.isEmpty()) {

                // ADD NEW ROUTINE
                db.collection(
                    "routines"
                ).document()

            } else {

                // UPDATE EXISTING ROUTINE
                db.collection(
                    "routines"
                ).document(
                    routineId
                )
            }

        val finalRoutineId =
            routineDocument.id

        // =====================================================
        // ROUTINE DATA
        // =====================================================

        val routineData =
            hashMapOf<String, Any>(

                "id" to
                        finalRoutineId,

                "userId" to
                        userId,

                "petId" to
                        selectedPetId,

                "petName" to
                        selectedPetName,

                "routineName" to
                        routineName,

                "taskName" to
                        taskName,

                "category" to
                        category,

                // Example: 09:00 AM
                "time" to
                        selectedTime,

                "frequency" to
                        frequency,

                "repeatDays" to
                        repeatDays,

                "suppliesNeeded" to
                        supplies,

                "instructions" to
                        instructions,

                "notes" to
                        notes,

                "photoUrl" to
                        photoUrl,

                "createdAt" to
                        existingCreatedAt,

                "updatedAt" to
                        System.currentTimeMillis()
            )

        // =====================================================
        // SAVE / UPDATE FIRESTORE
        //
        // IMPORTANT:
        // SetOptions.merge() means fields not included above
        // will NOT be deleted.
        //
        // So these remain safe:
        // lastCompletedDate
        // lastCompletedAt
        // =====================================================

        routineDocument
            .set(
                routineData,
                SetOptions.merge()
            )
            .addOnSuccessListener {

                progressBar.visibility =
                    View.GONE

                btnSaveRoutine.isEnabled =
                    true

                val message =
                    if (routineId.isEmpty()) {

                        "Routine saved successfully"

                    } else {

                        "Routine updated successfully"
                    }

                Toast.makeText(
                    this,
                    message,
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            }
            .addOnFailureListener { exception ->

                progressBar.visibility =
                    View.GONE

                btnSaveRoutine.isEnabled =
                    true

                Toast.makeText(
                    this,
                    "Save failed: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // LOAD ROUTINE FOR EDIT
    // =========================================================

    private fun loadRoutineForEdit() {

        progressBar.visibility =
            View.VISIBLE

        db.collection("routines")
            .document(routineId)
            .get()
            .addOnSuccessListener { document ->

                progressBar.visibility =
                    View.GONE

                if (!document.exists()) {

                    Toast.makeText(
                        this,
                        "Routine not found",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()

                    return@addOnSuccessListener
                }

                // =================================================
                // PET
                // =================================================

                val petId =
                    document.getString(
                        "petId"
                    ) ?: ""

                selectedPetId =
                    petId

                selectedPetName =
                    document.getString(
                        "petName"
                    ) ?: ""

                // =================================================
                // ROUTINE NAME
                // =================================================

                etRoutineName.setText(

                    document.getString(
                        "routineName"
                    ) ?: ""
                )

                // =================================================
                // TASK NAME
                // =================================================

                etTaskName.setText(

                    document.getString(
                        "taskName"
                    ) ?: ""
                )

                // =================================================
                // CATEGORY
                // =================================================

                actCategory.setText(

                    document.getString(
                        "category"
                    ) ?: "",

                    false
                )

                // =================================================
                // TIME
                // =================================================

                selectedTime =
                    convertTo12HourFormat(

                        document.getString(
                            "time"
                        ) ?: ""
                    )

                btnTime.text =
                    if (
                        selectedTime.isEmpty()
                    ) {

                        "SELECT TIME"

                    } else {

                        selectedTime
                    }

                // =================================================
                // FREQUENCY
                // =================================================

                actFrequency.setText(

                    document.getString(
                        "frequency"
                    ) ?: "",

                    false
                )

                // =================================================
                // SUPPLIES
                // =================================================

                etSupplies.setText(

                    document.getString(
                        "suppliesNeeded"
                    ) ?: ""
                )

                // =================================================
                // INSTRUCTIONS
                // =================================================

                etInstructions.setText(

                    document.getString(
                        "instructions"
                    ) ?: ""
                )

                // =================================================
                // NOTES
                // =================================================

                etNotes.setText(

                    document.getString(
                        "notes"
                    ) ?: ""
                )

                // =================================================
                // PHOTO
                // =================================================

                photoUrl =
                    document.getString(
                        "photoUrl"
                    ) ?: ""

                if (photoUrl.isNotEmpty()) {

                    btnAddPhoto.text =
                        "PHOTO SELECTED ✓"
                }

                // =================================================
                // CREATED AT
                // =================================================

                existingCreatedAt =
                    document.getLong(
                        "createdAt"
                    )
                        ?: System.currentTimeMillis()

                // =================================================
                // REPEAT DAYS
                // =================================================

                val repeatDays =
                    document.get(
                        "repeatDays"
                    ) as? List<*>
                        ?: emptyList<String>()

                cbSun.isChecked =
                    repeatDays.contains(
                        "Sun"
                    )

                cbMon.isChecked =
                    repeatDays.contains(
                        "Mon"
                    )

                cbTue.isChecked =
                    repeatDays.contains(
                        "Tue"
                    )

                cbWed.isChecked =
                    repeatDays.contains(
                        "Wed"
                    )

                cbThu.isChecked =
                    repeatDays.contains(
                        "Thu"
                    )

                cbFri.isChecked =
                    repeatDays.contains(
                        "Fri"
                    )

                cbSat.isChecked =
                    repeatDays.contains(
                        "Sat"
                    )

                // =================================================
                // LOAD PETS AND PRESELECT CURRENT PET
                // =================================================

                loadPets(
                    petId
                )
            }
            .addOnFailureListener { exception ->

                progressBar.visibility =
                    View.GONE

                Toast.makeText(
                    this,
                    "Unable to load routine: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}