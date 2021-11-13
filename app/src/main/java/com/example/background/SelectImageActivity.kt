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

package com.example.background

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Html
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.background.databinding.ActivitySelectBinding
import com.google.android.material.snackbar.Snackbar
import java.util.ArrayList

/**
 *帮助为FilterActivity选择图像并处理权限请求。
 *图像有两个来源： MediaStore和StockImages 。
 */
class SelectImageActivity : AppCompatActivity() {

    private var permissionRequestCount = 0
    private var hasPermissions = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySelectBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }

        with(binding) {
            // 显示库存图片来源。
            credits.text = fromHtml(getString(R.string.credits))
            // 启用链接跟随。
            credits.movementMethod = LinkMovementMethod.getInstance()
        }

        /*
        我们会跟踪我们请求权限的次数。
        如果用户不想两次授予权限 - 显示一个 Snackbar 并且不要在剩余的会话中再次请求权限。
         */
        Log.e("savedInstanceState",savedInstanceState.toString())
        if (savedInstanceState != null) {
            permissionRequestCount = savedInstanceState.getInt(KEY_PERMISSIONS_REQUEST_COUNT, 0)
        }

        requestPermissionsIfNecessary()
        //选着相册
        binding.selectImage.setOnClickListener {
            val chooseIntent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            startActivityForResult(chooseIntent, REQUEST_CODE_IMAGE)
        }
        //选着图片库
        binding.selectStockImage.setOnClickListener {
            startActivity(
                FilterActivity.newIntent(
                    this@SelectImageActivity, StockImages.randomStockImage()
                )
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PERMISSIONS_REQUEST_COUNT, permissionRequestCount)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_CODE_IMAGE -> handleImageRequestResult(data)
                else -> Log.d(TAG, "Unknown request code.")
            }
        } else {
            Log.e(TAG, String.format("Unexpected Result code \"%s\" or missing data.", resultCode))
        }
    }

    /**
     * 请求结果
     * @param requestCode Int
     * @param permissions Array<String>
     * @param grantResults IntArray
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.e( "请求权限码 requestCode : ",requestCode.toString())

        // Check if permissions were granted after a permissions request flow.
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            requestPermissionsIfNecessary() // no-op if permissions are granted already.
        }
    }

    /**
     * 必要时请求权限
     */
    private fun requestPermissionsIfNecessary() {
        // Check to see if we have all the permissions we need.
        // Otherwise request permissions up to MAX_NUMBER_REQUESTED_PERMISSIONS.
        hasPermissions = checkAllPermissions()
        if (!hasPermissions) {
            if (permissionRequestCount < MAX_NUMBER_REQUEST_PERMISSIONS) {
                permissionRequestCount += 1
                ActivityCompat.requestPermissions(
                    this,
                    sPermissions.toTypedArray(),
                    REQUEST_CODE_PERMISSIONS
                )
            } else {
                Snackbar.make(
                    findViewById(R.id.coordinatorLayout),
                    R.string.set_permissions_in_settings,
                    Snackbar.LENGTH_INDEFINITE
                ).show()

                findViewById<View>(R.id.selectImage).isEnabled = false
            }
        }
    }

    /**
     * 处理请求的图片
     * @param data Intent
     */
    private fun handleImageRequestResult(data: Intent) {
        // Get the imageUri the user picked, from the Intent.ACTION_PICK result.
        val imageUri = data.clipData!!.getItemAt(0).uri

        if (imageUri == null) {
            Log.e(TAG, "Invalid input image Uri.")
            return
        }
        startActivity(FilterActivity.newIntent(this, imageUri))
    }

    /**
     * 检查全部权限
     * @return Boolean
     */
    private fun checkAllPermissions(): Boolean {
        var hasPermissions = true
        for (permission in sPermissions) {
            hasPermissions = hasPermissions and (ContextCompat.checkSelfPermission(
                this, permission
            ) == PackageManager.PERMISSION_GRANTED)
        }
        return hasPermissions
    }

    companion object {

        private const val TAG = "SelectImageActivity"
        private const val KEY_PERMISSIONS_REQUEST_COUNT = "KEY_PERMISSIONS_REQUEST_COUNT"

        private const val MAX_NUMBER_REQUEST_PERMISSIONS = 2
        private const val REQUEST_CODE_IMAGE = 100
        private const val REQUEST_CODE_PERMISSIONS = 101

        // A list of permissions the application needs.
        @VisibleForTesting
        val sPermissions: MutableList<String> = object : ArrayList<String>() {
            init {
                add(Manifest.permission.INTERNET)
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        private fun fromHtml(input: String): Spanned {
            return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                Html.fromHtml(input, Html.FROM_HTML_MODE_COMPACT)
            } else {
                // method deprecated at API 24.
                @Suppress("DEPRECATION")
                Html.fromHtml(input)
            }
        }
    }
}
