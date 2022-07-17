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

    private lateinit var userDTO: UserDTO

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        /* 현재 유저의 휴대전화번호 및 토큰 정보를 얻어옴 */
        getTel()
        getToken()
        /* 생년월일, 주소, 사진, 저장버튼에 대한 리스너 처리 */
        setBirth()
        setAddress()
        setPhoto()
        setSave()
    }

    private fun getTel() {
        tel = intent.getStringExtra("tel").toString()
    }

    private fun getToken() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    token = task.result.toString()
                }
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
                                photoUri = task.result.toString()
                                Glide.with(this)
                                    .load(photoUri)
                                    .circleCrop()
                                    .into(binding.joinPhoto)
                            }
                            setUploadFinish()
                        }
                    }
                } else setUploadFinish()
            }
        }

        binding.joinPhoto.setOnClickListener {
            val intent = Intent("android.intent.action.GET_CONTENT")
            intent.type = "image/*"
            getFromAlbumResultLauncher.launch(intent)
        }
    }

    private fun setSave() {
        binding.joinSave.setOnClickListener {
            if (allCheck()) {
                completeJoin()
            } else {
                Toast.makeText(this, "정보를 완벽하게 기입해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /* Start save assistant */
    private fun allCheck() : Boolean {
        guardian = when(binding.joinGuardianGroup.checkedRadioButtonId) {
            binding.joinGuardian.id -> true
            binding.joinWard.id -> false
            else -> return false
        }
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

    private fun setInsert() {
        binding.joinProgressbar.visibility = View.VISIBLE
        binding.joinTransformText.text = "회원가입 처리중..."
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun insertUser() {
        val userId = auth.uid.toString()
        val userDB = Firebase.database.reference.child("user").child(userId)
        userDTO = UserDTO(photoUri, name, birth, tel, token, zoneCode,
            roadAddress, buildingName, detailAddress, gender, guardian)

        userDB.setValue(userDTO)
    }

    private fun completeJoin() {
        setInsert()
        insertUser()
        moveActivity()
    }

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
    /* End save assistant */

    /* Start address assistant */
    private fun showKakaoAddressWebView() {
        binding.joinWebView.settings.apply {
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
        }
        binding.joinWebView.apply {
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
        }
    }

    private val chromeClient = object : WebChromeClient() {

        override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
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
        binding.joinProgressbar.visibility = View.VISIBLE
        binding.joinTransformText.text = "이미지 업로드중..."
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun setUploadFinish() {
        binding.joinProgressbar.visibility = View.INVISIBLE
        binding.joinTransformText.text = ""
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
    /* End photo assistant */
}