/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.background.workers

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.background.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Clears temporary files. */
class CleanupWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val targetDirectory = File(applicationContext.filesDir, Constants.OUTPUT_PATH)

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                Log.e(TAG, "doWork()" )
                cleanupDirectory()
               // Thread.sleep(20000)
                Log.e("你愁啥","瞅你咋地")
                Result.success()
            } catch (exception: Exception) {
                Log.e(TAG, "清理时出错", exception)
                Result.failure()
            }
        }
    }

    /** Removes pngs from the app's files directory */
    private fun cleanupDirectory() {
        targetDirectory.apply {
            if (exists()) {  //文件是否纯在
                listFiles()?.forEach { file ->
                    if (file.name.endsWith(".png")) {
                        val deleted = file.delete()
                        Log.i(TAG, "已删除 ${file.name} - $deleted")
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "CleanupWorker"
    }
}
