package com.funny.translation.codeeditor.ui.runner

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.funny.translation.codeeditor.base.BaseViewModel
import com.funny.translation.codeeditor.vm.ActivityCodeViewModel
import com.funny.translation.debug.Debug
import com.funny.translation.helper.coroutine.Coroutine
import com.funny.translation.js.JsEngine
import com.funny.translation.js.bean.JsBean
import com.funny.translation.js.core.JsTranslateTask
import com.funny.translation.trans.Language
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CodeRunnerViewModel(application: Application) : BaseViewModel(application),
    Debug.DebugTarget {
    private var jsEngine: JsEngine? = null

    val outputDebug = MutableLiveData("")

    fun initJs(activityCodeViewModel: ActivityCodeViewModel,code: String) {
        val jsBean = JsBean(999, code = code)
        //Log.d(TAG, "initJs: code:$code")
        jsEngine = JsEngine(jsBean).apply {
            loadBasicConfigurations(
                onSuccess = {
                    val jsTranslateTask = JsTranslateTask(
                        jsEngine = this,
                        sourceLanguage = activityCodeViewModel.sourceLanguage.value!!.toShort(),
                        targetLanguage = activityCodeViewModel.targetLanguage.value!!.toShort(),
                        sourceString = activityCodeViewModel.sourceString.value!!
                    )
                    Coroutine.async {
                        jsTranslateTask.translate()
                    }
                },
                onError = {

                }
            )
        }
    }

    init {
        Debug.addTarget(this)
    }

    companion object{
        const val TAG = "CodeRunnerVM"
    }

    override val source: String
        get() = "插件"

    override fun appendLog(text: CharSequence) {
        viewModelScope.launch {
            withContext(Dispatchers.Main){
                outputDebug.value = "${outputDebug.value}\n$text"
            }
        }
    }
}