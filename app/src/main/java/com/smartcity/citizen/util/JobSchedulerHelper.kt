package com.smartcity.citizen.util

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import java.util.concurrent.TimeUnit

class JobSchedulerHelper(private val context: Context) {

    fun scheduleCleanupJob() {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val componentName = ComponentName(context, CleanupJobService::class.java)
        val jobInfo = JobInfo.Builder(1, componentName)
            .setPeriodic(TimeUnit.HOURS.toMillis(24))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .build()
        jobScheduler.schedule(jobInfo)
    }

    class CleanupJobService : JobService() {
        override fun onStartJob(params: JobParameters?): Boolean {
            // Perform cleanup (e.g., delete temp files)
            // Return false if job is short
            return false
        }

        override fun onStopJob(params: JobParameters?): Boolean {
            return false
        }
    }
}