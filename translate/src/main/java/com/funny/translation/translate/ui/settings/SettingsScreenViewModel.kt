package com.funny.translation.translate.ui.settings

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.funny.translation.helper.readAssets
import com.funny.translation.translate.FunnyApplication
import com.funny.translation.translate.bean.OpenSourceLibraryInfo
import com.funny.translation.translate.utils.SortResultUtils
import com.funny.translation.translate.utils.TranslationEngineName
import com.funny.translation.translate.utils.localDataGson
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class SettingsScreenViewModel : ViewModel() {
    companion object {
        private const val TAG = "SettingsScreenVM"
    }

    private lateinit var openSourceLibraryList: List<OpenSourceLibraryInfo>
    suspend fun loadOpenSourceLibInfo(): List<OpenSourceLibraryInfo> =
        if (this@SettingsScreenViewModel::openSourceLibraryList.isInitialized) openSourceLibraryList
        else
            withContext(Dispatchers.IO) {
                val json = FunnyApplication.ctx.readAssets("open_source_libraries.json")
                val type = object : TypeToken<List<OpenSourceLibraryInfo>>() {}.type
                openSourceLibraryList = Gson().fromJson(json, type)
                openSourceLibraryList
            }

    val localEngineNamesState : List<TranslationEngineName> by lazy {
        runBlocking {
            return@runBlocking SortResultUtils.getLocalEngineNames()
        }
    }

}