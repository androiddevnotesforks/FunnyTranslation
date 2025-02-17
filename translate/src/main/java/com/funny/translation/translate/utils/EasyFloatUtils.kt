package com.funny.translation.translate.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.*
import com.funny.translation.AppConfig
import com.funny.translation.TranslateConfig
import com.funny.translation.helper.ClipBoardUtil
import com.funny.translation.helper.VibratorUtils
import com.funny.translation.translate.*
import com.funny.translation.translate.engine.TextTranslationEngines
import com.funny.translation.translate.ui.bean.TranslationConfig
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.enums.SidePattern
import com.lzf.easyfloat.interfaces.OnPermissionResult
import com.lzf.easyfloat.interfaces.OnTouchRangeListener
import com.lzf.easyfloat.permission.PermissionUtils
import com.lzf.easyfloat.utils.DragUtils
import com.lzf.easyfloat.widget.BaseSwitchView
import com.tomlonghurst.roundimageview.RoundImageView
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.min


object EasyFloatUtils {
    const val TAG_FLOAT_BALL = "ball"
    private const val TAG_TRANS_WINDOW = "window"
    private const val TAG = "EasyFloat"
    private var vibrating = false
    var initTransWindow = false
    var initFloatBall = false

    private var translateConfigFlow =
        MutableStateFlow(TranslationConfig("", Language.AUTO, Language.CHINESE))
    private var translateJob: Job? = null

    private var inputTextFlow = MutableStateFlow("")

    fun initScreenSize(activity: Activity) {
        val displayMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        AppConfig.SCREEN_WIDTH = displayMetrics.widthPixels
        AppConfig.SCREEN_HEIGHT = displayMetrics.heightPixels
    }
    
    private fun initTransWindow(view: View){
        view.layoutParams.width = (min(AppConfig.SCREEN_WIDTH, AppConfig.SCREEN_HEIGHT) * 0.9).toInt()

        val edittext = view.findViewById<EditText>(R.id.float_window_input)

        GlobalScope.launch {
            inputTextFlow.collect {
                withContext(Dispatchers.Main){
                    edittext.setText(it)
                    edittext.setSelection(it.length)
                }
            }
        }

        val spinnerSource: Spinner =
            view.findViewById<Spinner?>(R.id.float_window_spinner_source).apply {
                adapter = ArrayAdapter<String>(FunnyApplication.ctx, R.layout.view_spinner_text_item).apply {
                    addAll(enabledLanguages.value.map { it.displayText })
                    setDropDownViewResource(R.layout.view_spinner_dropdown_item)
                }
                setSelection(enabledLanguages.value.indexOf(translateConfigFlow.value.sourceLanguage ?: Language.AUTO))
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        translateConfigFlow.value = translateConfigFlow.value.copy(
                            sourceString = edittext.text.trim().toString(),
                            sourceLanguage = enabledLanguages.value[position]
                        )
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
//                        TODO("Not yet implemented")
                    }
                }
            }

        val spinnerTarget: Spinner =
            view.findViewById<Spinner?>(R.id.float_window_spinner_target).apply {
                adapter = ArrayAdapter<String>(FunnyApplication.ctx, R.layout.view_spinner_text_item).apply {
                    addAll(enabledLanguages.value.map { it.displayText })
                    setDropDownViewResource(R.layout.view_spinner_dropdown_item)
                }
                setSelection(enabledLanguages.value.indexOf(translateConfigFlow.value.targetLanguage ?: Language.CHINESE))
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        translateConfigFlow.value = translateConfigFlow.value.copy(
                            sourceString = edittext.text.trim().toString(),
                            targetLanguage = enabledLanguages.value[position]
                        )
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
//                        TODO("Not yet implemented")
                    }
                }
            }

        GlobalScope.launch {
            enabledLanguages.collect {
                withContext(Dispatchers.Main){
                    spinnerSource.adapter = ArrayAdapter<String>(FunnyApplication.ctx, R.layout.view_spinner_text_item).apply {
                        addAll(enabledLanguages.value.map { it.displayText })
                        setDropDownViewResource(R.layout.view_spinner_dropdown_item)
                    }
                    spinnerSource.setSelection(enabledLanguages.value.indexOf(translateConfigFlow.value.sourceLanguage ?: Language.AUTO))

                    spinnerTarget.adapter = ArrayAdapter<String>(FunnyApplication.ctx, R.layout.view_spinner_text_item).apply {
                        addAll(enabledLanguages.value.map { it.displayText })
                        setDropDownViewResource(R.layout.view_spinner_dropdown_item)
                    }
                    spinnerTarget.setSelection(enabledLanguages.value.indexOf(translateConfigFlow.value.targetLanguage ?: Language.CHINESE))
                }
            }
        }

        val rotateAnimation = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 300
        }

        view.findViewById<ImageButton?>(R.id.float_window_exchange_button).apply {
            setOnClickListener {
                val temp = spinnerSource.selectedItemPosition
                spinnerSource.setSelection(spinnerTarget.selectedItemPosition, true)
                spinnerTarget.setSelection(temp, true)
                startAnimation(rotateAnimation)
            }
        }

        val resultText: TextView = view.findViewById(R.id.float_window_text)
        val speakBtn = view.findViewById<ImageButton>(R.id.float_window_speak_btn).apply {
            setOnClickListener {
                val txt = resultText.text
                if (txt.isNotEmpty()){
                    AudioPlayer.play(txt.toString(), findLanguageById(spinnerTarget.selectedItemPosition)){
                        Toast.makeText(context, "朗读错误", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        view.findViewById<ImageButton?>(R.id.float_window_close).apply {
            setOnClickListener {
                EasyFloat.hide(TAG_TRANS_WINDOW)
            }
        }

        translateJob = GlobalScope.launch(Dispatchers.IO) {
            translateConfigFlow.collect {
                kotlin.runCatching {
                    if (it.sourceString!=null && it.sourceString!="") {
                        val sourceLanguage = enabledLanguages.value[spinnerSource.selectedItemPosition]
                        val targetLanguage = enabledLanguages.value[spinnerTarget.selectedItemPosition]
                        val task = TranslateUtils.createTask(
                            TextTranslationEngines.BaiduNormal,
                            it.sourceString!!,
                            sourceLanguage,
                            targetLanguage
                        )

                        // 设置全局的翻译参数
                        with(TranslateConfig){
                            this.sourceLanguage = task.sourceLanguage
                            this.targetLanguage = task.targetLanguage
                            this.sourceString   = task.sourceString
                        }

                        withContext(Dispatchers.Main) {
                            resultText.text = "正在翻译……"
                        }
                        task.translate()
                        withContext(Dispatchers.Main) {
                            resultText.text = task.result.basicResult.trans
                            if(speakBtn.visibility != View.VISIBLE){
                                speakBtn.visibility = View.VISIBLE
                            }
                        }
                    }
                }.onFailure {
                    withContext(Dispatchers.Main) {
                        it.printStackTrace()
                        resultText.text = FunnyApplication.ctx.resources.getString(R.string.trans_error).format(it)
                    }
                }
            }
        }

        view.findViewById<TextView?>(R.id.float_window_translate).apply {
            setOnLongClickListener {
                val clipboardText = ClipBoardUtil.read(context).trim()
                Log.d(TAG, "clipboardText: $clipboardText")
                if (clipboardText != "") {
                    VibratorUtils.vibrate(100)
                    edittext.setText(clipboardText)
                    translateConfigFlow.value =
                        translateConfigFlow.value.copy(sourceString = clipboardText)
                }
                true
            }
            setOnClickListener {
                val inputText = edittext.text.trim()
                if (inputText.isNotEmpty()) {
                    translateConfigFlow.value =
                        translateConfigFlow.value.copy(sourceString = inputText.toString())
                }
            }
        }

        view.findViewById<ImageButton>(R.id.float_window_open_app_btn).apply {
            setOnClickListener {
                Intent().apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse("funny://translation/translate?text=${edittext.text}&sourceId=${spinnerSource.selectedItemPosition}&targetId=${spinnerTarget.selectedItemPosition}")
                    flags = Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK
                }.let {
                    context.startActivity(it)
                }
            }
        }
    }

     fun showTransWindow(){
        if(!initTransWindow){
            EasyFloat.with(FunnyApplication.ctx)
                .setTag(TAG_TRANS_WINDOW)
                .setLayout(R.layout.layout_float_window){ view ->
                    initTransWindow(view)
                }
                .hasEditText(true)
                .setShowPattern(ShowPattern.ALL_TIME)
                .setSidePattern(SidePattern.DEFAULT)
                .setImmersionStatusBar(true)
                .setGravity(Gravity.CENTER_HORIZONTAL or Gravity.TOP, 0, 100)
                .show()
            initTransWindow = true
        }else{
            EasyFloat.show(TAG_TRANS_WINDOW)
        }
    }

    fun resetFloatBallPlace(activity: Activity){
        initScreenSize(activity)
        EasyFloat.updateFloat(TAG_FLOAT_BALL, AppConfig.SCREEN_WIDTH - 200, AppConfig.SCREEN_HEIGHT * 2 / 3)
    }

    fun startTranslate(sourceString: String, sourceLanguage: Language, targetLanguage: Language){
        inputTextFlow.value = sourceString
        translateConfigFlow.value = translateConfigFlow.value.copy(sourceString, sourceLanguage, targetLanguage)
    }

    @SuppressLint("MissingPermission")
    fun setVibrator(inRange: Boolean) {
        val vibrator = VibratorUtils.vibrator
        if (!vibrator.hasVibrator() || (inRange && vibrating)) return
        vibrating = inRange
        if (inRange) VibratorUtils.vibrate(100)
        else vibrator.cancel()
    }

    private fun _showFloatBall(){
        if(initFloatBall){
            EasyFloat.show(TAG_FLOAT_BALL)
        }else {
            EasyFloat.with(FunnyApplication.ctx)
                .setTag(TAG_FLOAT_BALL)
                .setLayout(R.layout.layout_float_ball) { view ->
                    view.findViewById<RoundImageView>(R.id.float_ball_image).apply {
                        setOnClickListener {
                            showTransWindow()
                        }
                    }
                }
                .setShowPattern(ShowPattern.ALL_TIME)
                .setSidePattern(SidePattern.RESULT_HORIZONTAL)
                .setImmersionStatusBar(true)
                .setGravity(Gravity.END or Gravity.BOTTOM, -20, -200)
                .registerCallback {
                    drag { _, motionEvent ->
                        DragUtils.registerDragClose(motionEvent, object : OnTouchRangeListener {
                            override fun touchInRange(inRange: Boolean, view: BaseSwitchView) {
                                setVibrator(inRange)
                                view.findViewById<TextView>(com.lzf.easyfloat.R.id.tv_delete).text =
                                    if (inRange) "松手删除" else "删除浮窗"

                                view.findViewById<ImageView>(com.lzf.easyfloat.R.id.iv_delete)
                                    .setImageResource(
                                        if (inRange) com.lzf.easyfloat.R.drawable.icon_delete_selected
                                        else com.lzf.easyfloat.R.drawable.icon_delete_normal
                                    )
                            }

                            override fun touchUpInRange() {
                                EasyFloat.dismiss(TAG_FLOAT_BALL)
                                initFloatBall = false
                            }
                        }, showPattern = ShowPattern.ALL_TIME)
                    }
                }
                .show()
            initFloatBall = true
        }
    }

    fun showFloatBall(activity : Activity){
        if(!PermissionUtils.checkPermission(FunnyApplication.ctx)) {
            AlertDialog.Builder(activity)
                .setMessage("使用浮窗功能，需要您授权悬浮窗权限。")
                .setPositiveButton("去开启") { _, _ ->
                    PermissionUtils.requestPermission(activity, object : OnPermissionResult {
                        override fun permissionResult(isOpen: Boolean) {
                            showFloatBall(activity)
                        }
                    })
                }
                .setNegativeButton("取消") { _, _ -> }
                .show()
        }else{
            _showFloatBall()
        }
    }

    fun hideAllFloatWindow(){
        EasyFloat.hide(TAG_TRANS_WINDOW)
        EasyFloat.hide(TAG_FLOAT_BALL)
    }

    fun dismissAll(){
        EasyFloat.dismiss(TAG_TRANS_WINDOW)
        EasyFloat.dismiss(TAG_FLOAT_BALL)
        translateJob?.cancel()
    }

    fun isShowingFloatBall() = EasyFloat.isShow(TAG_FLOAT_BALL)
}