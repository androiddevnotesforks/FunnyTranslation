package com.funny.trans.login

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import com.funny.data_saver.core.LocalDataSaver
import com.funny.trans.login.ui.LoginNavigation
import com.funny.trans.login.ui.LoginPage
import com.funny.translation.AppConfig
import com.funny.translation.Consts
import com.funny.translation.helper.BiometricUtils
import com.funny.translation.helper.DataSaverUtils
import com.funny.translation.theme.TransTheme
import com.smarx.notchlib.NotchScreenManager

class LoginActivity : AppCompatActivity() {
    private lateinit var callback: OnBackPressedCallback

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 状态栏沉浸
        WindowCompat.setDecorFitsSystemWindows(window, false)
        NotchScreenManager.getInstance().setDisplayInNotch(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            BiometricUtils.init()
        }

        callback = object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        }

        onBackPressedDispatcher.addCallback(this, callback)

        setContent {
            CompositionLocalProvider(LocalDataSaver provides DataSaverUtils) {
                TransTheme {
                    LoginNavigation(onLoginSuccess = {
                        Log.d(TAG, "登录成功: 用户: $it")
                        DataSaverUtils.saveData(Consts.KEY_JWT_TOKEN, it.jwt_token)
                        if(it.isValid()) AppConfig.userInfo.value = it
                        setResult(RESULT_OK, Intent())
                        finish()
                    })
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}