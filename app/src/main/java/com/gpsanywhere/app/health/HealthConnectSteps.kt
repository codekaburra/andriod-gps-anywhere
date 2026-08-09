package com.gpsanywhere.app.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Single entry point for all Health Connect step operations.
 *
 * Health Connect permissions are NOT runtime permissions: declaring them in the
 * manifest does nothing until the user approves them in the Health Connect
 * permission screen, launched via [androidx.health.connect.client.PermissionController.createRequestPermissionResultContract].
 * Every write attempted before that throws SecurityException.
 */
object HealthConnectSteps {

    private const val TAG = "HealthConnectSteps"

    /** Average walking cadence used to spread manual steps over a plausible time window. */
    private const val STEPS_PER_MINUTE = 105L

    val PERMISSIONS: Set<String> = setOf(
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    enum class Status { NOT_INSTALLED, UPDATE_REQUIRED, NO_PERMISSION, READY }

    suspend fun status(context: Context): Status {
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_UNAVAILABLE -> return Status.NOT_INSTALLED
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> return Status.UPDATE_REQUIRED
        }
        return try {
            val granted = HealthConnectClient.getOrCreate(context)
                .permissionController.getGrantedPermissions()
            if (granted.containsAll(PERMISSIONS)) Status.READY else Status.NO_PERMISSION
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query Health Connect permissions", e)
            Status.NO_PERMISSION
        }
    }

    /**
     * Write [count] steps ending now, spread backwards over a window matching a
     * normal walking cadence so consumer apps see a plausible record. Use this
     * for one-off manual entry where there is no real elapsed time to attribute.
     */
    suspend fun writeSteps(context: Context, count: Long): Result<Unit> {
        val end = Instant.now()
        val minutes = (count.coerceAtLeast(1L) / STEPS_PER_MINUTE).coerceAtLeast(1L)
        return writeSteps(context, count, end.minusSeconds(minutes * 60), end)
    }

    /**
     * Write [count] steps over the explicit window [[start], [end]]. Callers that
     * emit steps continuously must supply non-overlapping windows: Health Connect
     * rejects [StepsRecord]s from the same app whose time ranges overlap.
     */
    suspend fun writeSteps(
        context: Context,
        count: Long,
        start: Instant,
        end: Instant
    ): Result<Unit> = runCatching {
        require(count > 0) { "step count must be positive" }
        require(end.isAfter(start)) { "record end must be after start" }
        HealthConnectClient.getOrCreate(context).insertRecords(
            listOf(
                StepsRecord(
                    count = count,
                    startTime = start,
                    endTime = end,
                    startZoneOffset = offsetAt(start),
                    endZoneOffset = offsetAt(end)
                )
            )
        )
        Unit
    }.onFailure { Log.w(TAG, "Failed to write $count steps to Health Connect", it) }

    /** Today's total step count as other Health Connect apps see it, or null on failure. */
    suspend fun readTodayTotal(context: Context): Long? = try {
        val zone = ZoneId.systemDefault()
        val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        val result = HealthConnectClient.getOrCreate(context).aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startOfDay, Instant.now())
            )
        )
        result[StepsRecord.COUNT_TOTAL] ?: 0L
    } catch (e: Exception) {
        Log.w(TAG, "Failed to read today's steps from Health Connect", e)
        null
    }

    private fun offsetAt(instant: Instant): ZoneOffset =
        ZoneId.systemDefault().rules.getOffset(instant)
}
