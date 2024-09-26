package com.example.tigerspikeapp.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.tigerspikeapp.databinding.ActivityLoginBinding
import com.example.tigerspikeapp.db.UserInfo
import com.example.tigerspikeapp.service.FirebaseInstance
import com.example.tigerspikeapp.ui.MainActivity.Companion.COARSE_LOCATION
import com.example.tigerspikeapp.ui.MainActivity.Companion.FINE_LOCATION
import com.example.tigerspikeapp.ui.MainActivity.Companion.LOCATION_PERMISSION_REQUEST_CODE
import com.example.tigerspikeapp.utils.LoginResult

class LoginActivity : AppCompatActivity(), FirebaseInstance.ILogin {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        getLocationPermission()

        checkUserLoginInfo()

        FirebaseInstance.getInstance().setILogin(this@LoginActivity)

        binding.btnLogin.setOnClickListener {
            var username = binding.edtUsername.text.toString().trim()
            var password = binding.edtPassword.text.toString().trim()
            if (username.isEmpty() || password.isEmpty()) {
                binding.pbLoading.visibility = View.GONE
                Toast.makeText(this@LoginActivity,
                    "Please fill in all information!", Toast.LENGTH_SHORT).show()
            } else {
                binding.pbLoading.visibility = View.VISIBLE
                FirebaseInstance.getInstance().login(username, password)
            }
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
        }
    }

    /**
    * Request locate permission before login app
    * */
    private fun getLocationPermission() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (ContextCompat.checkSelfPermission(this.applicationContext, FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            if (ContextCompat.checkSelfPermission(this.applicationContext, COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) {
                MainActivity.mLocationPermissionsGranted = true
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        } else {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        MainActivity.mLocationPermissionsGranted = false
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty()) {
                for (grantResult in grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        MainActivity.mLocationPermissionsGranted = false
                        return
                    }
                }
                MainActivity.mLocationPermissionsGranted = true
            }
        }
    }

    /**
     * Check state login of user
     * */
    private fun checkUserLoginInfo() {
        var userInfo = UserInfo.getUserInfo(this@LoginActivity)
        // If already account move to MainActivity, else show login form
        if (userInfo != null && userInfo.username.isNotEmpty() && userInfo.password.isNotEmpty()) {
            binding.lnLogin.visibility = View.GONE
            binding.lnNext.visibility = View.VISIBLE

            binding.tvInfo.text = "Logging with ${userInfo.username}..."

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        } else {
            binding.lnLogin.visibility = View.VISIBLE
            binding.lnNext.visibility = View.GONE
        }
    }

    /**
     * Login response when check login info in Firebase db
     * */
    override fun login(username: String, password: String, loginResult: LoginResult) {
        binding.pbLoading.visibility = View.GONE

        if (loginResult == LoginResult.ACCOUNT_NOT_EXIST) {
            Toast.makeText(this@LoginActivity, "Account doesn't exist!", Toast.LENGTH_SHORT).show()
        }
        if (loginResult == LoginResult.WRONG_PASS) {
            Toast.makeText(this@LoginActivity, "Wrong password!", Toast.LENGTH_SHORT).show()
        }
        if (loginResult == LoginResult.SUCCESS) {
            // Login success, move to MainActivity, save UserInfo
            UserInfo.saveUserInfo(this@LoginActivity, username, password)
            finish()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }
}