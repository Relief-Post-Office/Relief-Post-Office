package com.seoul42.relief_post_office.guardian

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.adapters.TextViewBindingAdapter.setText
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.seoul42.relief_post_office.databinding.GuardianProfileBinding
import com.seoul42.relief_post_office.databinding.ProfileBinding
import com.seoul42.relief_post_office.model.UserDTO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class ProfileActivity : AppCompatActivity() {

    private val binding by lazy {
        ProfileBinding.inflate(layoutInflater)
    }

    private lateinit var userDTO: UserDTO

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        /* 미리 저장된 정보들을 반영 */
        setPreProcessed()
        setBackButton()
    }

    private fun setPreProcessed() {
        userDTO = intent.getSerializableExtra("userDTO") as UserDTO

        binding.profileName.text = userDTO.name
        binding.profileBirth.text = userDTO.birth
        binding.profileDetailAddress.text = userDTO.detailAddress
        binding.profileAddress.text = if (userDTO.buildingName.isEmpty()) {
            "(${userDTO.zoneCode})\n${userDTO.roadAddress}"
        } else {
            "(${userDTO.zoneCode})\n${userDTO.roadAddress}\n${userDTO.buildingName}"
        }
        if (userDTO.gender) {
            binding.profileMale.isChecked = true
            binding.profileMale.isEnabled = true
            binding.profileFemale.isEnabled = false
        } else {
            binding.profileFemale.isChecked = true
            binding.profileMale.isEnabled = false
            binding.profileFemale.isEnabled = true
        }
        Glide.with(this)
            .load(userDTO.photoUri)
            .circleCrop()
            .into(binding.profilePhoto)
    }

    private fun setBackButton() {
        binding.profileBackBtn.setOnClickListener {
            finish()
        }
    }
}