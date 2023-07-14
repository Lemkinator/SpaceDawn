package com.raghav.spacedawnv2.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.raghav.spacedawnv2.domain.model.LaunchDetail
import com.raghav.spacedawnv2.domain.util.ReminderScheduler
import com.raghav.spacedawnv2.domain.util.ReminderState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Schedules Reminders/Alarms by using Android's
 * [AlarmManager](https://developer.android.com/reference/android/app/AlarmManager)
 * also see [Simplifying AlarmManager: Understanding Alarm Scheduling in Android](https://avidraghav.hashnode.dev/simplifying-alarmmanager-understanding-alarm-scheduling-in-android)
 * @param context Application Context
 */
class AndroidReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : ReminderScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun setReminder(launchDetail: LaunchDetail): ReminderState {
        // Apps targeting Android version 12 (Version Code - S, Api Level 31)
        // or higher need to request for SCHEDULE_EXACT_ALARM permission explicitly whereas
        // Apps targeting any Android version below 12 don't need to do ask for this permission

        // Apps targeting Android version 13 (Version Code - Tiramisu, Api Level 33)
        // or higher need to request for POST_NOTIFICATIONS permission explicitly whereas
        // Apps targeting any Android version below 13 don't need to do ask for this permission

        // 1. For 13 and above both SCHEDULE_EXACT_ALARM and POST_NOTIFICATIONS permission needs to be checked
        // 2. For 12, 12L SCHEDULE_EXACT_ALARM needs to checked
        // 3. For below 12 no neither needs to be checked

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return when {
                alarmManager.canScheduleExactAlarms() && areNotificationsEnabled(context) -> {
                    setAlarm(launchDetail)
                    ReminderState.SetSuccessfully
                }

                alarmManager.canScheduleExactAlarms() && !areNotificationsEnabled(context) -> {
                    ReminderState.PermissionsState(
                        reminderPermission = true,
                        notificationPermission = false
                    )
                }

                !alarmManager.canScheduleExactAlarms() && areNotificationsEnabled(context) -> {
                    ReminderState.PermissionsState(
                        reminderPermission = false,
                        notificationPermission = true
                    )
                }

                !alarmManager.canScheduleExactAlarms() && !areNotificationsEnabled(context) -> {
                    ReminderState.PermissionsState(
                        reminderPermission = false,
                        notificationPermission = false
                    )
                }

                else -> {
                    ReminderState.PermissionsState(
                        reminderPermission = false,
                        notificationPermission = false
                    )
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return if (alarmManager.canScheduleExactAlarms()) {
                setAlarm(launchDetail)
                ReminderState.SetSuccessfully
            } else {
                ReminderState.PermissionsState(
                    reminderPermission = false,
                    notificationPermission = true
                )
            }
        } else {
            setAlarm(launchDetail)
            return ReminderState.SetSuccessfully
        }
    }

    override fun cancelReminder(id: String) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            Intent(context, ReminderBroadcastReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(
            pendingIntent
        )
    }

    private fun setAlarm(launchDetail: LaunchDetail) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra("key", launchDetail.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            launchDetail.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 10_000,
            pendingIntent
        )
    }

    @SuppressLint("InlinedApi")
    private fun areNotificationsEnabled(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
