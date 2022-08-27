package com.seoul42.relief_post_office.ward

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
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.seoul42.relief_post_office.databinding.WardProfileBinding
import com.seoul42.relief_post_office.model.UserDTO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * 피보호자 프로필 설정 클래스
 * 변경될 수 있는 정보
 *  1. 주소 및 상세주소
 *  2. 프로필 사진
 */
class WardProfileActivity  : AppCompatActivity() {

    private val auth : FirebaseAuth by lazy {
        Firebase.auth
    }
    private val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }
    private val binding by lazy {
        WardProfileBinding.inflate(layoutInflater)
    }

    private val userId = auth.uid.toString()

    // 데이터베이스 참조 변수
    private val userDB = Firebase.database.reference.child("user")

    // firebase storage 에 사진을 저장하고자 할 경로를 지정한 변수
    // 해당 경로를 통해 사진을 저장하거나 가져올 수 있음
    private val imagesRef = storage.reference.child("profile/$userId.jpg")

    private lateinit var userDTO: UserDTO

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)
        setPreProcessed()
        setAddress()
        setPhoto()
        setSave()
    }

    /**
     * 현재 피보호자의 정보를 미리 설정
     */
    private fun setPreProcessed() {
        userDTO = intent.getSerializableExtra("userDTO") as UserDTO

        binding.wardProfileBirth.hint = userDTO.birth
        binding.wardProfileName.setText(userDTO.name)
        binding.wardProfileDetailAddress.setText(userDTO.detailAddress)
        binding.wardProfileAddress.text = if (userDTO.buildingName.isEmpty()) {
            "(${userDTO.zoneCode})\n${userDTO.roadAddress}"
        } else {
            "(${userDTO.zoneCode})\n${userDTO.roadAddress}\n${userDTO.buildingName}"
        }
        if (userDTO.gender) {
            binding.wardProfileMale.isChecked = true
        } else {
            binding.wardProfileFemale.isChecked = true
        }
        Glide.with(this)
            .load(userDTO.photoUri)
            .circleCrop()
            .into(binding.wardProfilePhoto)
    }

    private fun setAddress() {
        binding.wardProfileWebView.setBackgroundColor(Color.TRANSPARENT);
        binding.wardProfileAddress.setOnClickListener {
            showKakaoAddressWebView()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun setPhoto() {
        // 보호자의 프로필 사진을 정상적으로 선택될 시 사진 업로드를 돕는 변수
        val getFromAlbumResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data ?: return@registerForActivityResult
                setPhotoUpload(uri)
            }
        }

        binding.wardProfilePhoto.setOnClickListener {
            val intent = Intent("android.intent.action.GET_CONTENT")
            intent.type = "image/*"
            getFromAlbumResultLauncher.launch(intent)
        }
    }

    /**
     *  피보호자가 변경하고자 할 정보를 수정하는 작업을 수행
     */
    private fun setSave() {
        binding.wardProfileSave.cornerRadius = 30
        binding.wardProfileSave.setOnClickListener {
            if (allCheck()) {
                completeJoin()
            } else {
                Toast.makeText(this, "정보를 완벽하게 기입해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     *  모든 정보가 기입되었는지를 확인하는 메서드
     */
    private fun allCheck() : Boolean {
        userDTO.detailAddress = binding.wardProfileDetailAddress.text.toString()

        if (userDTO.name.isEmpty() || userDTO.birth.isEmpty() || userDTO.token.isEmpty()
            || binding.wardProfileAddress.text.isEmpty()
            || binding.wardProfileDetailAddress.text.isEmpty()
            || userDTO.photoUri.isEmpty())
            return false
        return true
    }

    private fun setInsert() {
        binding.wardProfileProgressbar.visibility = View.VISIBLE
        binding.wardProfileTransformText.text = "프로필 변경중..."

        // 프로필 정보 변경시 화면 선택이 안되도록 설정
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

        // 수정된 정보를 반영
        userDB.child(userId).setValue(userDTO)
    }

    private fun completeJoin() {
        setInsert()
        Handler().postDelayed({
            val returnIntent = Intent()

            returnIntent.putExtra("userDTO", userDTO)
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
        }, 2500)
    }

    /**
     *  Kakao 도로명 주소 검색 API 를 활용
     */
    private fun showKakaoAddressWebView() {
        binding.wardProfileWebView.settings.apply {
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
        }
        binding.wardProfileWebView.apply {
            // index.html 에서 Leaf
            addJavascriptInterface(WebViewData(), "Leaf")
            webViewClient = client
            webChromeClient = chromeClient
            // hosting 주소
            loadUrl("http://relief-339ce.web.app/index.html")
        }
    }

    private val client: WebViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            return false
        }
        override fun onReceivedSslError(
            view: WebView?,
            handler: SslErrorHandler?,
            error: SslError?
        ) {
            handler?.proceed()
        }
    }

    /**
     *  실제로 입력한 주소 정보는 getAddress 메서드의 매개변수로 받아옴
     *  받아온 주소를 반영하도록 돕는 클래스
     */
    private inner class WebViewData {
        @JavascriptInterface
        fun getAddress(
            zone: String,
            road: String,
            building: String
        ) {
            CoroutineScope(Dispatchers.Default).launch {
                withContext(CoroutineScope(Dispatchers.Main).coroutineContext) {
                    setAddressInWeb(zone, road, building)
                }
            }
        }

        private fun setAddressInWeb(
            zone: String,
            road: String,
            building: String
        ) {
            userDTO.zoneCode = zone
            userDTO.roadAddress = road
            userDTO.buildingName = building
            binding.wardProfileAddress.text = if (userDTO.buildingName.isEmpty()) {
                "(${userDTO.zoneCode})\n${userDTO.roadAddress}"
            } else {
                "(${userDTO.zoneCode})\n${userDTO.roadAddress}\n${userDTO.buildingName}"
            }
        }
    }

    //  주소를 선택할 수 있는 새로운 창을 띄우도록 돕는 변수
    private val chromeClient = object : WebChromeClient() {
        /**
         *  주소를 선택할 수 있는 다이얼로그를 띄우는 메서드
         */
        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?
        ): Boolean {
            val newWebView = WebView(this@WardProfileActivity)
            val dialog = Dialog(this@WardProfileActivity)
            val params = dialog.window!!.attributes

            newWebView.settings.javaScriptEnabled = true
            dialog.setContentView(newWebView)
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window!!.attributes = params
            dialog.show()

            newWebView.webChromeClient = object : WebChromeClient() {
                override fun onJsAlert(
                    view: WebView,
                    url: String,
                    message: String,
                    result: JsResult
                ): Boolean {
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

    /**
     * 사진의 회전 상태를 정상적으로 변환해주도록 하는 메서드
     */
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

    /**
     *  변경된 이미지의 uri 을 받아와 사진 업로드를 수행하는 메서드
     */
    private fun setPhotoUpload(uri : Uri) {
        val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
        val orientation = getOrientationOfImage(uri).toFloat()
        val newBitmap = getRotatedBitmap(bitmap, orientation)

        binding.wardProfileProgressbar.visibility = View.VISIBLE
        binding.wardProfileTransformText.text = "이미지 업로드중..."

        // 사진 업로드 시 화면 선택이 안되도록 설정
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

        imagesRef.putFile(uri).addOnSuccessListener {
            imagesRef.downloadUrl.addOnCompleteListener{ task ->
                updatePhoto(task)
            }
        }
    }

    private fun updatePhoto(task : Task<Uri>) {
        if (task.isSuccessful) {
            userDTO.photoUri = task.result.toString()
            Glide.with(this)
                .load(userDTO.photoUri)
                .circleCrop()
                .into(binding.wardProfilePhoto)
        }
        setUploadFinish()
    }

    private fun setUploadFinish() {
        binding.wardProfileProgressbar.visibility = View.INVISIBLE
        binding.wardProfileTransformText.text = ""
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}