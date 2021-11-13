/*
 * Copyright 2021 The Android Open Source Project
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

package com.example.background

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkContinuation
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.background.workers.CleanupWorker
import com.example.background.workers.SaveImageToGalleryWorker
import com.example.background.workers.UploadWorker
import com.example.background.workers.filters.BlurEffectFilterWorker
import com.example.background.workers.filters.GrayScaleFilterWorker
import com.example.background.workers.filters.WaterColorFilterWorker

/**
 * Builds and holds WorkContinuation based on supplied filters.
 */
@SuppressLint("EnqueueWork")
class ImageOperations(
    context: Context,
    imageUri: Uri,         //本地图片路径
    waterColor: Boolean = false,
    grayScale: Boolean = false,
    blur: Boolean = false,
    save: Boolean = false   //保存位置
) {

    private val imageInputData = workDataOf(Constants.KEY_IMAGE_URI to imageUri.toString())
    val continuation: WorkContinuation

    init {
        /**
         * 创建一个唯一的工作队列，唯一工作队列里面的任务不能重复添加
         */
        continuation = WorkManager.getInstance(context)
            .beginUniqueWork(
                Constants.IMAGE_MANIPULATION_WORK_NAME,   //工作,名称
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequest.from(CleanupWorker::class.java)
            ).thenMaybe<WaterColorFilterWorker>(waterColor)
            .thenMaybe<GrayScaleFilterWorker>(grayScale)
            .thenMaybe<BlurEffectFilterWorker>(blur)
            .then(  //成功完成任务
                if (save) { //保存本地
                    workRequest<SaveImageToGalleryWorker>(tag = Constants.TAG_OUTPUT)

                } else /* 上传 */ {
                    workRequest<UploadWorker>(tag = Constants.TAG_OUTPUT)
                }
            )
    }

    /**
     *如果apply为true则将 ListenableWorker应用于WorkContinuation 。
     */
    private inline fun <reified T : ListenableWorker> WorkContinuation.thenMaybe(
        apply: Boolean
    ): WorkContinuation {
        return if (apply) {
            then(workRequest<T>())
        } else {
            this
        }
    }

    /**
     * 使用给定的 inputData 和标签（如果已设置）创建OneTimeWorkRequest
     */
    private inline fun <reified T : ListenableWorker> workRequest(
        inputData: Data = imageInputData,
        tag: String? = null
    ) =
        OneTimeWorkRequestBuilder<T>().apply {   //一次性任务
            setInputData(inputData)
            setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            if (!tag.isNullOrEmpty()) {
                addTag(tag)
            }
        }.build()
}
