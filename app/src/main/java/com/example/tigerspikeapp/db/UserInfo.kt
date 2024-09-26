package com.example.tigerspikeapp.db

import android.content.Context

/**
 * Save user login info
 * */
class UserInfo {
    var username = ""
    var password = ""

    constructor(username: String, password: String) {
        this.username = username
        this.password = password
    }

    companion object {
        private const val PACKAGE_NAME = "USER_INFO"
        const val USERNAME = "USERNAME"
        const val PASSWORD = "PASSWORD"

        fun saveUserInfo(context: Context, username: String, password: String) {
            var prefs = context.getSharedPreferences(PACKAGE_NAME, Context.MODE_PRIVATE)
            if (prefs != null) {
                var editor = prefs.edit()
                editor.putString(USERNAME, username)
                editor.putString(PASSWORD, password)
                editor.apply()
            } else {
                var editor = context.getSharedPreferences(PACKAGE_NAME, Context.MODE_PRIVATE).edit()
                editor.putString(USERNAME, username)
                editor.putString(PASSWORD, password)
                editor.apply()
            }
        }

        fun getUserInfo(context: Context): UserInfo? {
            var prefs = context.getSharedPreferences(PACKAGE_NAME, Context.MODE_PRIVATE)
            if (prefs != null) {
                var username = prefs.getString(USERNAME, "")
                var password = prefs.getString(PASSWORD, "")
                var userInfo = UserInfo(username.toString(), password.toString())
                return userInfo
            }
            return null
        }

        fun clearUserInfo(context: Context) {
            var prefs = context.getSharedPreferences(PACKAGE_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
        }
    }
}