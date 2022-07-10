package com.seoul42.relief_post_office

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.media.ExifInterface
import android.net.Uri
import android.net.http.SslError
import android.os.*
import android.util.Log
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
import com.seoul42.relief_post_office.model.UserDTO
import com.seoul42.relief_post_office.util.Guardian
import com.seoul42.relief_post_office.util.UserInfo
import com.seoul42.relief_post_office.util.Ward
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
    private val guardianRadioGroup: RadioGroup by lazy {
        findViewById<RadioGroup>(R.id.join_guardian_group)
    }
    private val nameEdit: EditText by lazy {
        findViewById<EditText>(R.id.join_name)
    }
    private val birthText: TextView by lazy {
        findViewById<TextView>(R.id.join_birth)
    }
    private val genderRadioGroup: RadioGroup by lazy {
        findViewById<RadioGroup>(R.id.join_gender_group)
    }
    private val addressText: TextView by lazy {
        findViewById<TextView>(R.id.join_address)
    }
    private val detailAddressEdit: EditText by lazy {
        findViewById<EditText>(R.id.join_detail_address)
    }
    private val photoButton: ImageButton by lazy {
        findViewById<ImageButton>(R.id.join_photo)
    }
    private val saveButton: Button by lazy {
        findViewById<Button>(R.id.join_save)
    }
    private val progressBar: ProgressBar by lazy {
        findViewById<ProgressBar>(R.id.join_progressbar)
    }
    private val translateText: TextView by lazy {
        findViewById<TextView>(R.id.join_transform_text)
    }
    private val webView: WebView by lazy {
        findViewById<WebView>(R.id.join_webView)
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
        setContentView(R.layout.join)

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
        birthText.setOnClickListener {
            val dialog = AlertDialog.Builder(this).create()
            val eDialog : LayoutInflater = LayoutInflater.from(this)
            val mView : View = eDialog.inflate(R.layout.dialog_birth,null)
            val year : NumberPicker = mView.findViewById(R.id.birth_year)
            val month : NumberPicker = mView.findViewById(R.id.birth_month)
            val day : NumberPicker = mView.findViewById(R.id.birth_day)
            val save : Button = mView.findViewById(R.id.birth_save)
            var myYear : Int = 1970
            var myMonth : Int = 1
            var myDay : Int = 1
            val listener = NumberPicker.OnValueChangeListener { numberPicker, _, new ->
                when (numberPicker) {
                    year -> myYear = new
                    month -> myMonth = new
                    day -> myDay = new
                }
            }

            year.wrapSelectorWheel = false
            month.wrapSelectorWheel = false
            day.wrapSelectorWheel = false
            year.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
            month.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
            day.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
            year.minValue = 1900
            month.minValue = 1
            day.minValue = 1
            year.maxValue = 2100
            month.maxValue = 12
            day.maxValue = 31

            if (birth.isEmpty()) {
                year.value = 1970
                month.value = 1
                day.value = 1
                birth = "1970/1/1"
            } else {
                year.value = birth.split("/")[0].toInt()
                month.value = birth.split("/")[1].toInt()
                day.value = birth.split("/")[2].toInt()
                myYear = year.value
                myMonth = month.value
                myDay = day.value
            }

            year.setOnValueChangedListener(listener)
            month.setOnValueChangedListener(listener)
            day.setOnValueChangedListener(listener)

            save.setOnClickListener {
                birth = "$myYear/$myMonth/$myDay"
                birthText.text = birth
                dialog.dismiss()
                dialog.cancel()
            }

            dialog.setView(mView)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.create()
            dialog.show()
        }
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
        guardian = when(guardianRadioGroup.checkedRadioButtonId) {
            R.id.join_guardian -> true
            R.id.join_ward -> false
            else -> return false
        }
        gender = when(genderRadioGroup.checkedRadioButtonId) {
            R.id.join_male -> true
            R.id.join_female -> false
            else -> return false
        }
        name = nameEdit.text.toString()
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
        userDTO = UserDTO(userId, photoUri, name, birth, tel, token, zoneCode,
            roadAddress, buildingName, detailAddress, gender, guardian)

        userDB.setValue(userDTO)
    }

    private fun completeJoin() {
        setInsert()
        insertUser()
        setInfo()
    }

    private fun setInfo() {
        /* 보호자 및 피보호자에 대한 현재 유저 세팅 */
        if (userDTO.guardian == true) Guardian(userDTO)
        else Ward(userDTO)
        UserInfo() /* 모든 유저 정보 세팅 */
        moveActivity()
    }

    private fun moveActivity() {
        Handler().postDelayed({
            ActivityCompat.finishAffinity(this)
            if (userDTO.guardian == true)
                startActivity(Intent(this, GuardianBackgroundActivity::class.java))
            else
                startActivity(Intent(this, WardActivity::class.java))
        }, 3000)
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