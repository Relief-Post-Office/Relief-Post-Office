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
import android.util.Log
import android.view.*
import android.webkit.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.databinding.GuardianProfileBinding
import com.seoul42.relief_post_office.model.UserDTO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class GuardianProfileActivity : AppCompatActivity() {

    private val auth : FirebaseAuth by lazy {
        Firebase.auth
    }
    private val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }
    private val binding by lazy {
        GuardianProfileBinding.inflate(layoutInflater)
    }

    private lateinit var userDTO: UserDTO

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        /* 미리 저장된 정보들을 반영 */
        setPreProcessed()
        /* 주소, 사진, 저장버튼에 대한 리스너 처리 */
        setAddress()
        setPhoto()
        setSave()
    }


    private fun setPreProcessed() {
        userDTO = intent.getSerializableExtra("userDTO") as UserDTO

        binding.guardianProfileBirth.hint = userDTO.birth
        binding.guardianProfileName.setText(userDTO.name)
        binding.guardianProfileDetailAddress.setText(userDTO.detailAddress)
        binding.guardianProfileAddress.text = if (userDTO.buildingName.isEmpty()) {
            "(${userDTO.zoneCode})\n${userDTO.roadAddress}"
        } else {
            "(${userDTO.zoneCode})\n${userDTO.roadAddress}\n${userDTO.buildingName}"
        }
        if (userDTO.gender == true) {
            binding.guardianProfileMale.isChecked = true
        } else {
            binding.guardianProfileFemale.isChecked = true
        }
        Glide.with(this)
            .load(userDTO.photoUri)
            .circleCrop()
            .into(binding.guardianProfilePhoto)
    }

    private fun setAddress() {
        binding.guardianProfileWebView.setBackgroundColor(Color.TRANSPARENT);
        binding.guardianProfileAddress.setOnClickListener {
            showKakaoAddressWebView()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun setPhoto() {
        val userId = auth.uid.toString()
        val imagesRef = storage.reference
            .child("profile/$userId.jpg")
        val getFromAlbumResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data

                setUpload()
                if (uri != null) {
                    val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
                    val orientation = getOrientationOfImage(uri).toFloat()
                    val newBitmap = getRotatedBitmap(bitmap, orientation)

                    imagesRef.putFile(uri).addOnSuccessListener {
                        imagesRef.downloadUrl.addOnCompleteListener{ task ->
                            if (task.isSuccessful) {
                                userDTO.photoUri = task.result.toString()
                                Glide.with(this)
                                    .load(userDTO.photoUri)
                                    .circleCrop()
                                    .into(binding.guardianProfilePhoto)
                            }
                            setUploadFinish()
                        }
                    }
                } else setUploadFinish()
            }
        }

        binding.guardianProfilePhoto.setOnClickListener {
            val intent = Intent("android.intent.action.GET_CONTENT")
            intent.type = "image/*"
            getFromAlbumResultLauncher.launch(intent)
        }
    }

    private fun setSave() {
        binding.guardianProfileSave.cornerRadius = 30
        binding.guardianProfileSave.setOnClickListener {
            if (allCheck()) {
                completeJoin()
            } else {
                Toast.makeText(this, "정보를 완벽하게 기입해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /* Start save assistant */
    private fun allCheck() : Boolean {
        userDTO.detailAddress = binding.guardianProfileDetailAddress.text.toString()

        if (userDTO.name.isEmpty() || userDTO.birth.isEmpty() || userDTO.token.isEmpty()
            || binding.guardianProfileAddress.text.isEmpty()
            || binding.guardianProfileDetailAddress.text.isEmpty()
            || userDTO.photoUri.isEmpty())
            return false
        return true
    }

    private fun setInsert() {
        binding.guardianProfileProgressbar.visibility = View.VISIBLE
        binding.guardianProfileTransformText.text = "프로필 변경중..."
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun insertUser() {
        val userId = auth.uid.toString()
        val userDB = Firebase.database.reference.child("user").child(userId)

        userDB.setValue(userDTO)
    }

    private fun completeJoin() {
        setInsert()
        insertUser()
        Handler().postDelayed({
            val returnIntent = Intent()

            returnIntent.putExtra("userDTO", userDTO)
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
        }, 2500)
    }
    /* End save assistant */

    /* Start address assistant */
    private fun showKakaoAddressWebView() {
        binding.guardianProfileWebView.settings.apply {
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
        }
        binding.guardianProfileWebView.apply {
            /* index.html 에서 Leaf */
            addJavascriptInterface(WebViewData(), "Leaf")
            webViewClient = client
            webChromeClient = chromeClient
            /* hosting 주소 */
            loadUrl("http://relief-339ce.web.app/index.html")
        }
    }

    private val client: WebViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            return false
        }
        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
            handler?.proceed()
        }
    }

    private inner class WebViewData {
        @JavascriptInterface
        fun getAddress(zone: String, road: String, building: String) {
            CoroutineScope(Dispatchers.Default).launch {
                withContext(CoroutineScope(Dispatchers.Main).coroutineContext) {
                    userDTO.zoneCode = zone
                    userDTO.roadAddress = road
                    userDTO.buildingName = building
                    binding.guardianProfileAddress.text = if (userDTO.buildingName.isEmpty()) {
                        "(${userDTO.zoneCode})\n${userDTO.roadAddress}"
                    } else {
                        "(${userDTO.zoneCode})\n${userDTO.roadAddress}\n${userDTO.buildingName}"
                    }
                }
            }
        }
    }

    private val chromeClient = object : WebChromeClient() {

        override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
            val newWebView = WebView(this@GuardianProfileActivity)
            val dialog = Dialog(this@GuardianProfileActivity)
            val params = dialog.window!!.attributes

            newWebView.settings.javaScriptEnabled = true
            dialog.setContentView(newWebView)
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window!!.attributes = params
            dialog.show()

            newWebView.webChromeClient = object : WebChromeClient() {
                override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
                    super.onJsAlert(view, url, message, result)
                    return true
                }
                override fun onCloseWindow(window: WebView?) {
                    dialog.dismiss()
                }
            }
            (resultMsg!!.obj as WebView.WebViewTransport).webView = newWebView
            resultMsg.sendToTarget()

            return true
        }
    }
    /* End address assistant */

    /* Start photo assistant */
    @RequiresApi(Build.VERSION_CODES.N)
    private fun getOrientationOfImage(uri: Uri): Int {
        val inputStream = contentResolver.openInputStream(uri)
        val exif: ExifInterface? = try {
            ExifInterface(inputStream!!)
        } catch (e: IOException) {
            e.printStackTrace()
            return -1
        }
        inputStream.close()

        val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        if (orientation != -1) {
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> return 90
                ExifInterface.ORIENTATION_ROTATE_180 -> return 180
                ExifInterface.ORIENTATION_ROTATE_270 -> return 270
            }
        }
        return 0
    }

    @Throws(Exception::class)
    private fun getRotatedBitmap(bitmap: Bitmap?, degrees: Float): Bitmap? {
        if (bitmap == null) return null
        if (degrees == 0F) return bitmap

        val m = Matrix()
        m.setRotate(degrees, bitmap.width.toFloat() / 2, bitmap.height.toFloat() / 2)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    }

    private fun setUpload() {
        binding.guardianProfileProgressbar.visibility = View.VISIBLE
        binding.guardianProfileTransformText.text = "이미지 업로드중..."
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun setUploadFinish() {
        binding.guardianProfileProgressbar.visibility = View.INVISIBLE
        binding.guardianProfileTransformText.text = ""
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
    /* End photo assistant */
}