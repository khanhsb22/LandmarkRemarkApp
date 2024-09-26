package com.example.tigerspikeapp.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tigerspikeapp.R
import com.example.tigerspikeapp.databinding.ActivityLoginBinding
import com.example.tigerspikeapp.databinding.ActivityRegisterBinding
import com.example.tigerspikeapp.service.FirebaseInstance
import com.example.tigerspikeapp.utils.RegisterResult

class RegisterActivity : AppCompatActivity(), FirebaseInstance.IRegister {
    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        FirebaseInstance.getInstance().setIRegister(this@RegisterActivity)

        binding.btnRegister.setOnClickListener {
            var username = binding.edtUsername.text.toString().trim()
            var password = binding.edtPassword.text.toString().trim()
            var retypePass = binding.edtRetypePassword.text.toString().trim()
            if (username.isEmpty() || password.isEmpty() || retypePass.isEmpty()) {
                binding.pbLoading.visibility = View.GONE
                Toast.makeText(this@RegisterActivity,
                    "Please fill in all information!", Toast.LENGTH_SHORT).show()
            } else {
                binding.pbLoading.visibility = View.VISIBLE
                FirebaseInstance.getInstance().register(username, password, retypePass)
            }
        }

    }

    /**
     * Register account response when register account on Firebase db
     * */
    override fun register(registerResult: RegisterResult) {
        binding.pbLoading.visibility = View.GONE

        when (registerResult) {
            RegisterResult.RETYPE_PASS_WRONG -> {
                Toast.makeText(this@RegisterActivity,
                    "Retype password wrong, let try again!", Toast.LENGTH_SHORT).show()
            }
            RegisterResult.USERNAME_EXIST -> {
                Toast.makeText(this@RegisterActivity,
                    "Account already exists, please choose another username!", Toast.LENGTH_SHORT).show()
            }
            else -> {
                // Register success
                var alertDialog = AlertDialog.Builder(this@RegisterActivity)
                alertDialog.setTitle("Notify")
                alertDialog.setMessage("Create account success, you can login!")
                alertDialog.setCancelable(false) // Không cho click ở ngoài làm mất dialog
                alertDialog.setPositiveButton("Ok") { dialog, which ->
                    alertDialog.setCancelable(true)
                    finish()
                }
                alertDialog.setNegativeButton("Cancel") {dialog, which ->
                    alertDialog.setCancelable(true)
                    dialog.dismiss()
                }
                alertDialog.show()
            }
        }
    }

    /**
     * Error response when register failed
     * */
    override fun error(ex: Exception) {
        binding.pbLoading.visibility = View.GONE
        Toast.makeText(this@RegisterActivity,
            "Error when create account: ${ex.message.toString()}", Toast.LENGTH_SHORT).show()
    }
}