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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.seoul42.relief_post_office.R
import com.seoul42.relief_post_office.model.UserDTO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import com.seoul42.relief_post_office.util.Ward.Companion.USER

class WardProfileActivity  : AppCompatActivity() {

    private val auth : FirebaseAuth by lazy {
        Firebase.auth
    }
    private val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }
    private val nameEdit: EditText by lazy {
        findViewById<EditText>(R.id.ward_profile_name)
    }
    private val birthText: TextView by lazy {
        findViewById<TextView>(R.id.ward_profile_birth)
    }
    private val genderRadioMale: RadioButton by lazy {
        findViewById<RadioButton>(R.id.ward_profile_male)
    }
    private val genderRadioFemale: RadioButton by lazy {
        findViewById<RadioButton>(R.id.ward_profile_female)
    }
    private val addressText: TextView by lazy {
        findViewById<TextView>(R.id.ward_profile_address)
    }
    private val detailAddressEdit: EditText by lazy {
        findViewById<EditText>(R.id.ward_profile_detail_address)
    }
    private val photoButton: ImageButton by lazy {
        findViewById<ImageButton>(R.id.ward_profile_photo)
    }
    private val saveButton: Button by lazy {
        findViewById<Button>(R.id.ward_profile_save)
    }
    private val progressBar: ProgressBar by lazy {
        findViewById<ProgressBar>(R.id.ward_profile_progressbar)
    }
    private val translateText: TextView by lazy {
        findViewById<TextView>(R.id.ward_profile_transform_text)
    }
    private val webView: WebView by lazy {
        findViewById<WebView>(R.id.ward_profile_webView)
    }

    private var guardian : Boolean = USER.guardian == true
    private var gender : Boolean = USER.gender == true
    private var name : String = USER.name!!
    private var birth : String = USER.birth!!
    private var photoUri : String = USER.photoUri!!
    private var token : String = USER.token!!
    private var tel : String = USER.tel!!
    private var zoneCode : String = USER.zoneCode!!
    private var roadAddress : String = USER.roadAddress!!
    private var buildingName : String = USER.buildingName!!
    private var detailAddress : String = USER.detailAddress!!

    private lateinit var userDTO: UserDTO

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ward_profile)

        /* 미리 저장된 정보들을 반영 */
        setPreProcessed()
        /* 주소, 사진, 저장버튼에 대한 리스너 처리 */
        setAddress()
        setPhoto()
        setSave()
    }


    private fun setPreProcessed() {
        birthText.hint = birth
        nameEdit.setText(name)
        detailAddressEdit.setText(detailAddress)
        addressText.text = if (buildingName.isEmpty()) {
            "($zoneCode)\n$roadAddress"
        } else {
            "($zoneCode)\n$roadAddress\n$buildingName"
        }
        if (USER.gender == true) {
            genderRadioMale.isChecked = true
        } else {
            genderRadioFemale.isChecked = true
        }
        Glide.with(this)
            .load(USER.photoUri)
            .circleCrop()
            .into(photoButton)
    }

    private fun setAddress() {
        webView.setBackgroundColor(Color.TRANSPARENT);
        addressText.setOnClickListener {
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
                                    .into(photoButton)
                            }
                            setUploadFinish()
                        }
                    }
                } else setUploadFinish()
            }
        }

        photoButton.setOnClickListener {
            val intent = Intent("android.intent.action.GET_CONTENT")
            intent.type = "image/*"
            getFromAlbumResultLauncher.launch(intent)
        }
    }

    private fun setSave() {
        saveButton.setOnClickListener {
            if (allCheck()) {
                completeJoin()
            } else {
                Toast.makeText(this, "정보를 완벽하게 기입해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /* Start save assistant */
    private fun allCheck() : Boolean {
        detailAddress = detailAddressEdit.text.toString()

        if (name.isEmpty() || birth.isEmpty() || token.isEmpty()
            || addressText.text.isEmpty() || detailAddressEdit.text.isEmpty()
            || photoUri.isEmpty())
            return false
        return true
    }

    private fun setInsert() {
        progressBar.visibility = View.VISIBLE
        translateText.text = "회원가입 처리중..."
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun insertUser() {
        val userId = auth.uid.toString()
        val userDB = Firebase.database.reference.child("user").child(userId)
        userDTO = UserDTO(photoUri, name, birth, tel, token, zoneCode,
            roadAddress, buildingName, detailAddress, gender, guardian)
        USER = userDTO

        userDB.setValue(userDTO)
    }

    private fun completeJoin() {
        setInsert()
        insertUser()
        Handler().postDelayed({
            finish()
        }, 2500)
    }
    /* End save assistant */

    /* Start address assistant */
    private fun showKakaoAddressWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
        }
        webView.apply {
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
                    addressText.text = if (buildingName.isEmpty()) {
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
        progressBar.visibility = View.VISIBLE
        translateText.text = "이미지 업로드중..."
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun setUploadFinish() {
        progressBar.visibility = View.INVISIBLE
        translateText.text = ""
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
    /* End photo assistant */
}