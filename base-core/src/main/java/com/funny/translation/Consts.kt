package com.funny.translation

object Consts {

    const val KEY_EMAIL = "email"
    const val KEY_USER_UID = "uid"
    const val KEY_ENTER_TO_TRANSLATE = "enter_to_trans"

    //错误常量
    const val ERROR_UNKNOWN = "未知错误！翻译失败！"
    const val ERROR_JSON = "Json数据解析错误！"
    const val ERROR_UNSUPPORT_LANGUAGE = "暂不支持的语言翻译形式"
    const val ERROR_DATED_ENGINE = "当前方法已过期！请反馈！"
    const val ERROR_POST = "发送POST请求异常！请检查网络连接！"
    const val ERROR_IO = "IO流错误！"
    const val ERROR_ILLEGAL_DATA = "数据不合法！"
    const val ERROR_ONLY_CHINESE_SUPPORT = "当前翻译模式仅支持中文！"
    const val ERROR_NO_BV_OR_AV = "未检测到有效的Bv号或Av号"

    const val MODE_EACH_LINE = 1

    // 百度翻译常量
    // 为避免不必要的麻烦，开源部分不包含此部分，请您谅解
    // 您可以访问 https://api.fanyi.baidu.com/ 免费注册该项服务
    private val DEFAULT_BAIDU_APP_ID: String = ""
    private val DEFAULT_BAIDU_SECURITY_KEY: String = ""
    const val DEFAULT_BAIDU_SLEEP_TIME = 1000L

    var BAIDU_APP_ID = DEFAULT_BAIDU_APP_ID
    var BAIDU_SECURITY_KEY = DEFAULT_BAIDU_SECURITY_KEY
    var BAIDU_SLEEP_TIME = DEFAULT_BAIDU_SLEEP_TIME

    const val KEY_SOURCE_LANGUAGE = "sourceLanguage"
    const val KEY_TARGET_LANGUAGE = "targetLanguage"
    const val KEY_APP_CHANNEL = "app_channel"

    const val KEY_HIDE_NAVIGATION_BAR = "hide_nav_bar"
    const val KEY_HIDE_STATUS_BAR = "hide_status_bar"
    const val KEY_SHOW_FLOAT_WINDOW = "show_float_window"
    const val KEY_SPRING_THEME: String = "spring_theme"
    const val KEY_SORT_RESULT = "sort_result"
    const val KEY_CUSTOM_NAVIGATION = "custom_nav"
    const val KEY_FIRST_OPEN_APP = "first_open_app_v2"
    const val KEY_JWT_TOKEN = "jwt_token"

    const val MAX_SELECT_ENGINES = 5
}