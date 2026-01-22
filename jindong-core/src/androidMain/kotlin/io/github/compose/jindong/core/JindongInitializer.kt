/*
 * Copyright (C) 2026 compose-jindong
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.compose.jindong.core

import android.annotation.SuppressLint
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri

/**
 * Internal ContentProvider for automatically capturing the application context.
 *
 * This provider is registered in the AndroidManifest and is automatically
 * instantiated by the system during app startup, before any user code runs.
 * This allows [HapticManager] to access the context without requiring
 * manual initialization.
 *
 * Users do not need to interact with this class directly.
 */
public class JindongInitializer : ContentProvider() {

  override fun onCreate(): Boolean {
    applicationContext = context?.applicationContext
    return true
  }

  override fun query(
    uri: Uri,
    projection: Array<out String>?,
    selection: String?,
    selectionArgs: Array<out String>?,
    sortOrder: String?,
  ): Cursor? = null

  override fun getType(uri: Uri): String? = null

  override fun insert(uri: Uri, values: ContentValues?): Uri? = null

  override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

  override fun update(
    uri: Uri,
    values: ContentValues?,
    selection: String?,
    selectionArgs: Array<out String>?,
  ): Int = 0

  public companion object {
    @SuppressLint("StaticFieldLeak")
    @Volatile
    internal var applicationContext: Context? = null
      private set
  }
}

/**
 * Android implementation of [getPlatformContext].
 *
 * Returns the application context captured by [JindongInitializer].
 */
internal actual fun getPlatformContext(): Any? = JindongInitializer.applicationContext
