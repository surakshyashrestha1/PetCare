package com.example.petcare

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

class TasksActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tasksContainer: LinearLayout
    private lateinit var emptyTasksState: MaterialCardView
    private lateinit var tvTodayDate: TextView

    // Shake sensor
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastShakeTime = 0L
    private var isResetDialogShowing = false

    private var todayDate = ""
    private var todayDay = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_tasks)

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

        tasksContainer =
            findViewById(
                R.id.tasksContainer
            )

        emptyTasksState =
            findViewById(
                R.id.emptyTasksState
            )

        tvTodayDate =
            findViewById(
                R.id.tvTodayDate
            )

        val btnAddTask =
            findViewById<MaterialButton>(
                R.id.btnAddTask
            )

        val btnShareTasks =
            findViewById<MaterialButton>(
                R.id.btnShareTasks
            )

        val bottomNavigation =
            findViewById<BottomNavigationView>(
                R.id.bottomNavigation
            )

        // =====================================================
        // TODAY DATE
        // =====================================================

        todayDate =
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            ).format(
                Date()
            )

        todayDay =
            SimpleDateFormat(
                "EEE",
                Locale.ENGLISH
            ).format(
                Date()
            )

        val formattedDate =
            SimpleDateFormat(
                "EEE, dd MMM",
                Locale.getDefault()
            ).format(
                Date()
            )

        tvTodayDate.text =
            "$formattedDate • Today's care plan"

        // =====================================================
        // SHAKE SENSOR
        // =====================================================

        sensorManager =
            getSystemService(
                Context.SENSOR_SERVICE
            ) as SensorManager

        accelerometer =
            sensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER
            )

        // =====================================================
        // ADD CARE ROUTINE
        // =====================================================

        btnAddTask.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    AddRoutineActivity::class.java
                )
            )
        }

        // =====================================================
        // SHARE
        // =====================================================

        btnShareTasks.setOnClickListener {

            shareTodayChecklist()
        }

        // =====================================================
        // BOTTOM NAVIGATION
        // =====================================================

        bottomNavigation.selectedItemId =
            R.id.nav_checklist

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

                // CHECKLIST - CURRENT SCREEN
                R.id.nav_checklist -> {

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
    // ACTIVITY RESUME
    // =========================================================

    override fun onResume() {
        super.onResume()

        loadTodayTasks()

        accelerometer?.let { sensor ->

            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    // =========================================================
    // ACTIVITY PAUSE
    // =========================================================

    override fun onPause() {
        super.onPause()

        sensorManager.unregisterListener(
            this
        )
    }

    // =========================================================
    // LOAD TODAY TASKS
    // =========================================================

    private fun loadTodayTasks() {

        val userId =
            auth.currentUser?.uid
                ?: return

        tasksContainer.removeAllViews()

        db.collection(
            "routines"
        )
            .whereEqualTo(
                "userId",
                userId
            )
            .get()
            .addOnSuccessListener { documents ->

                tasksContainer.removeAllViews()

                var taskCount =
                    0

                for (document in documents) {

                    val repeatDays =
                        document.get(
                            "repeatDays"
                        ) as? List<*>
                            ?: emptyList<String>()

                    // Show only today's routines
                    if (!repeatDays.contains(todayDay)) {

                        continue
                    }

                    val routineId =
                        document.getString(
                            "id"
                        ) ?: document.id

                    val petName =
                        document.getString(
                            "petName"
                        ) ?: "Pet"

                    val taskName =
                        document.getString(
                            "taskName"
                        ) ?: "Care Task"

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

                    val completedToday =
                        lastCompletedDate ==
                                todayDate

                    addTaskCard(
                        routineId =
                            routineId,

                        petName =
                            petName,

                        taskName =
                            taskName,

                        category =
                            category,

                        time =
                            time,

                        completed =
                            completedToday
                    )

                    taskCount++
                }

                if (taskCount == 0) {

                    emptyTasksState.visibility =
                        View.VISIBLE

                    tasksContainer.visibility =
                        View.GONE

                } else {

                    emptyTasksState.visibility =
                        View.GONE

                    tasksContainer.visibility =
                        View.VISIBLE
                }
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Unable to load today's tasks: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // ADD TASK CARD
    // =========================================================

    private fun addTaskCard(
        routineId: String,
        petName: String,
        taskName: String,
        category: String,
        time: String,
        completed: Boolean
    ) {

        val taskView =
            layoutInflater.inflate(
                R.layout.item_task,
                tasksContainer,
                false
            )

        val cbTaskDone =
            taskView.findViewById<CheckBox>(
                R.id.cbTaskDone
            )

        val tvTaskName =
            taskView.findViewById<TextView>(
                R.id.tvTaskName
            )

        val tvTaskPet =
            taskView.findViewById<TextView>(
                R.id.tvTaskPet
            )

        val tvTaskInfo =
            taskView.findViewById<TextView>(
                R.id.tvTaskInfo
            )

        val btnEditTask =
            taskView.findViewById<MaterialButton>(
                R.id.btnEditTask
            )

        val btnDeleteTask =
            taskView.findViewById<MaterialButton>(
                R.id.btnDeleteTask
            )

        // =====================================================
        // SHOW DATA
        // =====================================================

        tvTaskName.text =
            taskName

        tvTaskPet.text =
            petName

        tvTaskInfo.text =
            if (time.isEmpty()) {

                category

            } else {

                "$category • $time"
            }

        // =====================================================
        // CHECKBOX INITIAL STATE
        // =====================================================

        cbTaskDone.setOnCheckedChangeListener(
            null
        )

        cbTaskDone.isChecked =
            completed

        updateTaskAppearance(
            tvTaskName,
            completed
        )

        // =====================================================
        // CHECKBOX COMPLETE / UNCOMPLETE
        // =====================================================

        cbTaskDone.setOnCheckedChangeListener { _, isChecked ->

            updateTaskCompletion(
                routineId =
                    routineId,

                completed =
                    isChecked,

                tvTaskName =
                    tvTaskName
            )
        }

        // =====================================================
        // EDIT BUTTON
        // =====================================================

        btnEditTask.setOnClickListener {

            val intent =
                Intent(
                    this,
                    AddRoutineActivity::class.java
                )

            intent.putExtra(
                "ROUTINE_ID",
                routineId
            )

            startActivity(
                intent
            )
        }

        // =====================================================
        // DELETE BUTTON
        // =====================================================

        btnDeleteTask.setOnClickListener {

            showDeleteDialog(
                routineId =
                    routineId,

                taskName =
                    taskName,

                petName =
                    petName
            )
        }

        // =====================================================
        // GESTURE CONTROLS
        //
        // RIGHT = COMPLETE
        // LEFT  = DELETE
        // =====================================================

        var startX =
            0f

        var startY =
            0f

        val swipeThreshold =
            60 *
                    resources.displayMetrics.density

        val swipeTouchListener =
            View.OnTouchListener { view, event ->

                when (event.actionMasked) {

                    MotionEvent.ACTION_DOWN -> {

                        startX =
                            event.x

                        startY =
                            event.y

                        true
                    }

                    MotionEvent.ACTION_MOVE -> {

                        true
                    }

                    MotionEvent.ACTION_UP -> {

                        val endX =
                            event.x

                        val endY =
                            event.y

                        val differenceX =
                            endX -
                                    startX

                        val differenceY =
                            endY -
                                    startY

                        val isHorizontalSwipe =
                            abs(
                                differenceX
                            ) >
                                    abs(
                                        differenceY
                                    )

                        val distanceEnough =
                            abs(
                                differenceX
                            ) >=
                                    swipeThreshold

                        if (
                            isHorizontalSwipe &&
                            distanceEnough
                        ) {

                            // =============================
                            // SWIPE RIGHT
                            // =============================

                            if (differenceX > 0) {

                                if (!cbTaskDone.isChecked) {

                                    cbTaskDone.isChecked =
                                        true

                                } else {

                                    Toast.makeText(
                                        this@TasksActivity,
                                        "Task is already completed",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                true

                            } else {

                                // =========================
                                // SWIPE LEFT
                                // =========================

                                showDeleteDialog(
                                    routineId =
                                        routineId,

                                    taskName =
                                        taskName,

                                    petName =
                                        petName
                                )

                                true
                            }

                        } else {

                            view.performClick()

                            true
                        }
                    }

                    MotionEvent.ACTION_CANCEL -> {

                        true
                    }

                    else -> {

                        true
                    }
                }
            }

        /*
         * Important:
         *
         * Cardमा TextViews touch events लिन सक्छन्.
         * त्यसैले listener root card मात्र होइन,
         * text area मा पनि attach गरिएको छ.
         *
         * Checkbox/Edit/Delete मा attach गरिएको छैन,
         * ताकि normal click पनि काम गरोस्.
         */

        taskView.setOnTouchListener(
            swipeTouchListener
        )

        tvTaskName.setOnTouchListener(
            swipeTouchListener
        )

        tvTaskPet.setOnTouchListener(
            swipeTouchListener
        )

        tvTaskInfo.setOnTouchListener(
            swipeTouchListener
        )

        // =====================================================
        // ADD CARD TO CONTAINER
        // =====================================================

        tasksContainer.addView(
            taskView
        )
    }

    // =========================================================
    // UPDATE COMPLETION IN FIREBASE
    // =========================================================

    private fun updateTaskCompletion(
        routineId: String,
        completed: Boolean,
        tvTaskName: TextView
    ) {

        val updateData =
            if (completed) {

                mapOf(
                    "lastCompletedDate" to
                            todayDate,

                    "lastCompletedAt" to
                            System.currentTimeMillis()
                )

            } else {

                mapOf(
                    "lastCompletedDate" to
                            "",

                    "lastCompletedAt" to
                            0L
                )
            }

        db.collection(
            "routines"
        )
            .document(
                routineId
            )
            .update(
                updateData
            )
            .addOnSuccessListener {

                updateTaskAppearance(
                    tvTaskName,
                    completed
                )

                Toast.makeText(
                    this,
                    if (completed) {
                        "Task completed ✓"
                    } else {
                        "Task marked as pending"
                    },
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Unable to update task: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()

                // Restore actual Firestore state
                loadTodayTasks()
            }
    }

    // =========================================================
    // TASK APPEARANCE
    // =========================================================

    private fun updateTaskAppearance(
        textView: TextView,
        completed: Boolean
    ) {

        if (completed) {

            textView.paintFlags =
                textView.paintFlags or
                        Paint.STRIKE_THRU_TEXT_FLAG

            textView.alpha =
                0.55f

        } else {

            textView.paintFlags =
                textView.paintFlags and
                        Paint.STRIKE_THRU_TEXT_FLAG.inv()

            textView.alpha =
                1f
        }
    }

    // =========================================================
    // SHAKE SENSOR
    // =========================================================

    override fun onSensorChanged(
        event: SensorEvent?
    ) {

        if (
            event == null ||
            event.sensor.type !=
            Sensor.TYPE_ACCELEROMETER
        ) {

            return
        }

        val x =
            event.values[0]

        val y =
            event.values[1]

        val z =
            event.values[2]

        val gForce =
            sqrt(
                (
                        x * x +
                                y * y +
                                z * z
                        ).toDouble()
            ) /
                    SensorManager.GRAVITY_EARTH

        val currentTime =
            System.currentTimeMillis()

        if (
            gForce > 2.7 &&
            currentTime - lastShakeTime > 1500 &&
            !isResetDialogShowing
        ) {

            lastShakeTime =
                currentTime

            showResetTodayDialog()
        }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {

        // Nothing required
    }

    // =========================================================
    // SHAKE RESET DIALOG
    // =========================================================

    private fun showResetTodayDialog() {

        if (isResetDialogShowing) {

            return
        }

        isResetDialogShowing =
            true

        val dialog =
            AlertDialog.Builder(
                this
            )
                .setTitle(
                    "Reset today's checklist?"
                )
                .setMessage(
                    "Shake detected. All completed tasks for today will be marked as pending."
                )
                .setNegativeButton(
                    "Cancel"
                ) { _, _ ->

                    isResetDialogShowing =
                        false
                }
                .setPositiveButton(
                    "Reset"
                ) { _, _ ->

                    isResetDialogShowing =
                        false

                    resetTodayChecklist()
                }
                .create()

        dialog.setOnCancelListener {

            isResetDialogShowing =
                false
        }

        dialog.setOnDismissListener {

            isResetDialogShowing =
                false
        }

        dialog.show()
    }

    // =========================================================
    // RESET TODAY'S COMPLETED TASKS
    // =========================================================

    private fun resetTodayChecklist() {

        val userId =
            auth.currentUser?.uid
                ?: return

        db.collection(
            "routines"
        )
            .whereEqualTo(
                "userId",
                userId
            )
            .get()
            .addOnSuccessListener { documents ->

                val batch =
                    db.batch()

                var resetCount =
                    0

                for (document in documents) {

                    val repeatDays =
                        document.get(
                            "repeatDays"
                        ) as? List<*>
                            ?: emptyList<String>()

                    if (!repeatDays.contains(todayDay)) {

                        continue
                    }

                    val lastCompletedDate =
                        document.getString(
                            "lastCompletedDate"
                        ) ?: ""

                    if (
                        lastCompletedDate ==
                        todayDate
                    ) {

                        batch.update(
                            document.reference,
                            mapOf(
                                "lastCompletedDate" to
                                        "",

                                "lastCompletedAt" to
                                        0L
                            )
                        )

                        resetCount++
                    }
                }

                if (resetCount == 0) {

                    Toast.makeText(
                        this,
                        "No completed tasks to reset today",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@addOnSuccessListener
                }

                batch.commit()
                    .addOnSuccessListener {

                        Toast.makeText(
                            this,
                            "$resetCount task(s) reset successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        loadTodayTasks()
                    }
                    .addOnFailureListener { exception ->

                        Toast.makeText(
                            this,
                            "Reset failed: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Unable to reset checklist: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // SHARE TODAY CHECKLIST
    // =========================================================

    private fun shareTodayChecklist() {

        val userId =
            auth.currentUser?.uid
                ?: return

        db.collection(
            "routines"
        )
            .whereEqualTo(
                "userId",
                userId
            )
            .get()
            .addOnSuccessListener { documents ->

                val taskLines =
                    mutableListOf<String>()

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

                    val status =
                        if (
                            lastCompletedDate ==
                            todayDate
                        ) {

                            "✓ Done"

                        } else {

                            "Pending"
                        }

                    val line =
                        if (time.isEmpty()) {

                            "• $taskName - $petName - $category - $status"

                        } else {

                            "• $taskName - $petName - $category - $time - $status"
                        }

                    taskLines.add(
                        line
                    )
                }

                if (taskLines.isEmpty()) {

                    Toast.makeText(
                        this,
                        "No care tasks available to share today",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@addOnSuccessListener
                }

                val formattedDate =
                    SimpleDateFormat(
                        "EEE, dd MMM yyyy",
                        Locale.getDefault()
                    ).format(
                        Date()
                    )

                val message =
                    buildString {

                        append(
                            "🐾 PetCare - Today's Checklist"
                        )

                        append(
                            "\n"
                        )

                        append(
                            formattedDate
                        )

                        append(
                            "\n\n"
                        )

                        taskLines.forEach { task ->

                            append(
                                task
                            )

                            append(
                                "\n"
                            )
                        }

                        append(
                            "\n"
                        )

                        append(
                            "Please follow today's pet care plan."
                        )
                    }

                val shareIntent =
                    Intent(
                        Intent.ACTION_SEND
                    ).apply {

                        type =
                            "text/plain"

                        putExtra(
                            Intent.EXTRA_SUBJECT,
                            "PetCare - Today's Checklist"
                        )

                        putExtra(
                            Intent.EXTRA_TEXT,
                            message
                        )
                    }

                startActivity(
                    Intent.createChooser(
                        shareIntent,
                        "Share care checklist"
                    )
                )
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Unable to prepare checklist: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // DELETE CONFIRMATION
    // =========================================================

    private fun showDeleteDialog(
        routineId: String,
        taskName: String,
        petName: String
    ) {

        AlertDialog.Builder(
            this
        )
            .setTitle(
                "Delete task?"
            )
            .setMessage(
                "Delete $taskName for $petName? This will also remove its care routine."
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Delete"
            ) { _, _ ->

                deleteRoutine(
                    routineId
                )
            }
            .show()
    }

    // =========================================================
    // DELETE FROM FIREBASE
    // =========================================================

    private fun deleteRoutine(
        routineId: String
    ) {

        db.collection(
            "routines"
        )
            .document(
                routineId
            )
            .delete()
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Care task deleted successfully",
                    Toast.LENGTH_SHORT
                ).show()

                loadTodayTasks()
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