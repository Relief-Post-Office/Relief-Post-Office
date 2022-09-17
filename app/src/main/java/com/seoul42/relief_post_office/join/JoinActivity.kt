package com.seoul42.relief_post_office.join

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
import android.os.*
import android.view.*
import android.webkit.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.seoul42.relief_post_office.guardian.GuardianBackgroundActivity
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.databinding.JoinBinding
import com.seoul42.relief_post_office.model.UserDTO
import com.seoul42.relief_post_office.ward.WardActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * 회원가입을 처리하도록 돕는 클래스
 * 회원가입을 기입할 정보
 *  - 피보호자 및 보호자 선택
 *  - 이름
 *  - 주소 및 상세주소
 *  - 생년월일
 *  - 성별
 *  - 사진
 */
class JoinActivity : AppCompatActivity() {

    private val auth : FirebaseAuth by lazy {
        Firebase.auth
    }
    private val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }
    private val binding by lazy {
        JoinBinding.inflate(layoutInflater)
    }

    private val userId = auth.uid.toString()

    // 데이터베이스 참조
    private val userDB = Firebase.database.reference.child("user")

    // firebase storage 에 사진을 저장하고자 할 경로를 지정한 변수
    // 해당 경로를 통해 사진을 저장하거나 가져올 수 있음
    private val imagesRef = storage.reference.child("profile/$userId.jpg")

    // 회원가입에 기입할 정보들
    private var guardian : Boolean = false
    private var gender : Boolean = false
    private var name : String = ""
    private var birth : String = ""
    private var photoUri : String = ""
    private var token : String = ""
    private var tel : String = ""
    private var zoneCode : String = ""
    private var roadAddress : String = ""
    private var buildingName : String = ""
    private var detailAddress : String = ""

    // 유저의 모든 정보를 객체에 담도록 함
    private lateinit var userDTO: UserDTO

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        getUserInfo()
        setUserInfo()
    }

    private fun getUserInfo() {
        getTel()
        getToken()
    }

    private fun setUserInfo() {
        setBirth()
        setAddress()
        setPhoto()
        setSave()
    }

    private fun getTel() {
        tel = intent.getStringExtra("tel").toString()
    }

    /**
     * FCM(Firebase Cloud Messaging) 이 가능하도록 돕는 토큰 정보를 받아오는 메서드
     * 토큰을 통해 유저간 FCM 전송이 가능
     */
    private fun getToken() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) token = task.result.toString()
            }
    }

    private fun setBirth() {
        binding.joinBirth.setOnClickListener {
            val birthDialog = BirthDialog(this)

            birthDialog.show(birth)
            birthDialog.setOnSaveClickedListener { content ->
                birth = content
                binding.joinBirth.text = birth
            }
        }
    }

    private fun setAddress() {
        binding.joinWebView.setBackgroundColor(Color.TRANSPARENT);
        binding.joinAddress.setOnClickListener {
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

        binding.joinPhoto.setOnClickListener {
            val intent = Intent("android.intent.action.GET_CONTENT")
            intent.type = "image/*"
            getFromAlbumResultLauncher.launch(intent)
        }
    }

    /**
     * 모든 정보가 기입될 시 유저의 정보가 저장되도록 처리하는 메서드
     */
    private fun setSave() {
        binding.joinSave.cornerRadius = 30
        binding.joinSave.setOnClickListener {
            if (allCheck()) {
                completeJoin()
            } else {
                Toast.makeText(this, "정보를 완벽하게 기입해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 모든 정보가 기입되었는지를 확인하는 메서드
     */
    private fun allCheck() : Boolean {
        // 보호자 및 피보호자 라디오 버튼은 선택이 되지 않을 경우 false 반환으로 처리
        guardian = when(binding.joinGuardianGroup.checkedRadioButtonId) {
            binding.joinGuardian.id -> true
            binding.joinWard.id -> false
            else -> return false
        }
        // 성별에 대한 라디오 버튼은 선택이 되지 않을 경우 false 반환으로 처리
        gender = when(binding.joinGenderGroup.checkedRadioButtonId) {
            binding.joinMale.id -> true
            binding.joinFemale.id -> false
            else -> return false
        }
        name = binding.joinName.text.toString()
        detailAddress = binding.joinDetailAddress.text.toString()

        if (name.isEmpty() || birth.isEmpty() || token.isEmpty()
            || binding.joinAddress.text.isEmpty()
            || binding.joinDetailAddress.text.isEmpty()
            || photoUri.isEmpty())
            return false
        return true
    }

    private fun insertUser() {
        val userId = auth.uid.toString()

        binding.joinProgressbar.visibility = View.VISIBLE
        binding.joinTransformText.text = "회원가입 처리중..."

        // 정보 삽입시 화면 선택이 안되도록 설정
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

        userDTO = UserDTO(photoUri, name, birth, tel, token, zoneCode,
            roadAddress, buildingName, detailAddress, gender, guardian)
        // 최종 정보를 반영
        userDB.child(userId).setValue(userDTO)
    }

    /**
     * 1. 기입된 모든 정보들을 데이터베이스에 삽입
     * 2. 보호자는 보호자 메인 화면, 피보호자는 피보호자 메인 화면으로 이동
     */
    private fun completeJoin() {
        insertUser()
        moveActivity()
    }

    /**
     * 보호자 플래그에 따라 화면 이동을 처리
     */
    private fun moveActivity() {
        val guardianIntent = Intent(this, GuardianBackgroundActivity::class.java)
        val wardIntent = Intent(this, WardActivity::class.java)

        Handler().postDelayed({
            ActivityCompat.finishAffinity(this)
            if (userDTO.guardian) {
                guardianIntent.putExtra("userDTO", userDTO)
                startActivity(guardianIntent)
            } else {
                wardIntent.putExtra("userDTO", userDTO)
                startActivity(wardIntent)
            }
        }, 2000)
    }

    /**
     * Kakao 도로명 주소 검색 API 를 활용하였음
     */
    private fun showKakaoAddressWebView() {
        binding.joinWebView.settings.apply {
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
        }
        binding.joinWebView.apply {
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
     * 실제로 입력한 주소 정보는 getAddress 메서드의 매개변수로 받아옴
     * 받아온 주소를 반영하도록 돕는 클래스
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
            zoneCode = zone
            roadAddress = road
            buildingName = building
            binding.joinAddress.text = if (buildingName.isEmpty()) {
                "($zoneCode)\n$roadAddress"
            } else {
                "($zoneCode)\n$roadAddress\n$buildingName"
            }
        }
    }

    //  주소를 선택할 수 있는 새로운 창을 띄우도록 돕는 변수
    private val chromeClient = object : WebChromeClient() {
        /**
         * 주소를 선택할 수 있는 다이얼로그를 띄우는 메서드
         */
        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?
        ): Boolean {
            val newWebView = WebView(this@JoinActivity)
            val dialog = Dialog(this@JoinActivity)
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
     * 변경된 이미지의 uri 을 받아와 사진 업로드를 수행하는 메서드
     */
    private fun setPhotoUpload(uri : Uri) {
        val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
        val orientation = getOrientationOfImage(uri).toFloat()
        val newBitmap = getRotatedBitmap(bitmap, orientation)

        binding.joinProgressbar.visibility = View.VISIBLE
        binding.joinTransformText.text = "이미지 업로드중..."

        // 사진 업로드 시 화면 선택이 안되도록 설정
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

        imagesRef.putFile(uri).addOnSuccessListener {
            imagesRef.downloadUrl.addOnCompleteListener{ task ->
                uploadPhoto(task)
            }
        }
    }

    private fun uploadPhoto(task : Task<Uri>) {
        if (task.isSuccessful) {
            photoUri = task.result.toString()
            Glide.with(this)
                .load(photoUri)
                .circleCrop()
                .into(binding.joinPhoto)
        }
        setUploadFinish()
    }

    private fun setUploadFinish() {
        binding.joinProgressbar.visibility = View.INVISIBLE
        binding.joinTransformText.text = ""
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
}