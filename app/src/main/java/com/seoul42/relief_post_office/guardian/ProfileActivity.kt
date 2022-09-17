package com.seoul42.relief_post_office.guardian

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.seoul42.relief_post_office.databinding.ProfileBinding
import com.seoul42.relief_post_office.model.UserDTO

/**
 * 단순히 보호자와 연결된 피보호자 정보를 확인하도록 하는 클래스
 */
class ProfileActivity : AppCompatActivity() {

    private val binding by lazy {
        ProfileBinding.inflate(layoutInflater)
    }

    private lateinit var userDTO: UserDTO

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setPreProcessed()
        setBackButton()
    }

    /**
     * 미리 저장된 정보들을 반영
     */
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