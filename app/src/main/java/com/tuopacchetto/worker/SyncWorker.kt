package com.tuopacchetto.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        // TODO: fetch updates e refresh cache locale + widget
        return Result.success()
    }
}