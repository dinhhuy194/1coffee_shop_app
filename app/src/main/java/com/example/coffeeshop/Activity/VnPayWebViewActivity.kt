package com.example.coffeeshop.Activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.coffeeshop.databinding.ActivityVnpayWebviewBinding

/**
 * Activity hiển thị WebView để thanh toán qua VNPAY.
 *
 * Luồng hoạt động:
 * 1. Nhận paymentUrl từ CheckoutActivity qua Intent
 * 2. Load URL lên WebView → người dùng thanh toán trên giao diện VNPAY
 * 3. Lắng nghe URL redirect:
 *    - Nếu URL chứa ReturnUrl → nghĩa là VNPAY đã xử lý xong
 *    - Parse vnp_ResponseCode từ URL
 *    - Hiển thị dialog kết quả
 *    - Trả kết quả về CheckoutActivity qua setResult()
 *
 * LƯU Ý: IPN callback ở backend đã lo việc cập nhật Firestore.
 * Activity này chỉ xử lý UI cho người dùng.
 */
class VnPayWebViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVnpayWebviewBinding

    companion object {
        const val EXTRA_PAYMENT_URL = "payment_url"
        const val EXTRA_ORDER_ID = "order_id"
        const val RESULT_PAYMENT_SUCCESS = "payment_success"
        const val RESULT_TRANSACTION_NO = "transaction_no"
        const val RESULT_BANK_CODE = "bank_code"
        const val RESULT_PAY_DATE = "pay_date"
        const val TAG = "VnPayWebView"

        /**
         * Chuỗi ReturnUrl mà WebView sẽ lắng nghe.
         * Khi VNPAY redirect về URL có chứa chuỗi này, ta biết giao dịch đã hoàn tất.
         * Phải khớp với ReturnUrl trong appsettings.json (phần đầu URL).
         */
        private const val RETURN_URL_PATTERN = "192.168.18.2:5282/api/payment/vnpay-return"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVnpayWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Lấy URL thanh toán từ Intent
        val paymentUrl = intent.getStringExtra(EXTRA_PAYMENT_URL)

        // Kiểm tra URL hợp lệ
        if (paymentUrl.isNullOrEmpty()) {
            Toast.makeText(this, "Lỗi: Không nhận được URL thanh toán", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Thiết lập nút quay lại
        binding.btnBack.setOnClickListener {
            // Hỏi xác nhận trước khi hủy thanh toán
            AlertDialog.Builder(this)
                .setTitle("Hủy thanh toán?")
                .setMessage("Bạn có chắc muốn hủy giao dịch thanh toán này không?")
                .setPositiveButton("Hủy thanh toán") { _, _ ->
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
                .setNegativeButton("Tiếp tục") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        // Thiết lập WebView
        setupWebView(paymentUrl)
    }

    /**
     * Cấu hình WebView và load URL thanh toán VNPAY.
     *
     * @param paymentUrl URL thanh toán nhận từ API Backend
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(paymentUrl: String) {
        binding.vnpayWebView.apply {
            // Bật JavaScript (bắt buộc cho trang VNPAY)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true

            // WebChromeClient: theo dõi tiến trình tải trang → hiện/ẩn ProgressBar
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    if (newProgress == 100) {
                        binding.progressBar.visibility = View.GONE
                    } else {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                }
            }

            // WebViewClient: lắng nghe URL redirect
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url.toString()

                    // LUỒNG 1: Nếu thấy VNPAY trả về link return -> Đọc kết quả và hiện Dialog!
                    if (url.contains("/api/payment/vnpay-return")) {
                        // Gọi hàm xử lý kết quả ở bên dưới (hàm này sẽ tự phân tích URL và tự gọi finish() khi user bấm OK)
                        handlePaymentReturn(url)

                        // Trả về true để báo cho WebView là "Tao xử lý rồi, mày không cần tải trang web này nữa"
                        return true
                    }

                    // LUỒNG 2: [Bổ sung] Chống lỗi màn hình trắng khi user chọn mở App Ngân hàng (MoMo, Sacombank...)
                    if (url.startsWith("intent://")) {
                        try {
                            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                            if (intent != null) {
                                // Cố gắng mở App ngân hàng trên điện thoại
                                view?.context?.startActivity(intent)
                            }
                            return true
                        } catch (e: Exception) {
                            // Nếu máy ảo/điện thoại không cài app ngân hàng đó -> Báo lỗi nhẹ nhàng, không cho crash app
                            Toast.makeText(this@VnPayWebViewActivity, "Bạn chưa cài ứng dụng ngân hàng này!", Toast.LENGTH_SHORT).show()
                            return true
                        }
                    }

                    // Dành cho các link khác thì cứ tải bình thường
                    return super.shouldOverrideUrlLoading(view, request)
                }
            }

            // Load URL thanh toán VNPAY
            loadUrl(paymentUrl)
        }
    }
    /**
     * Xử lý khi VNPAY redirect về ReturnUrl.
     * Parse vnp_ResponseCode từ URL và hiển thị kết quả cho người dùng.
     *
     * @param returnUrl URL chứa kết quả thanh toán từ VNPAY
     */
    private fun handlePaymentReturn(returnUrl: String) {
        try {
            val uri = Uri.parse(returnUrl)

            // Lấy mã kết quả thanh toán từ tham số vnp_ResponseCode
            val responseCode = uri.getQueryParameter("vnp_ResponseCode") ?: ""
            val transactionNo = uri.getQueryParameter("vnp_TransactionNo") ?: ""

            Log.d(TAG, "Mã phản hồi VNPAY: $responseCode, Mã GD: $transactionNo")

            if (responseCode == "00") {
                // ✅ THANH TOÁN THÀNH CÔNG
                val bankCode = uri.getQueryParameter("vnp_BankCode") ?: ""
                val payDate = uri.getQueryParameter("vnp_PayDate") ?: ""

                showResultDialog(
                    isSuccess = true,
                    title = "Thanh toán thành công! ✅",
                    message = "Giao dịch của bạn đã được xử lý thành công.\nMã giao dịch: $transactionNo",
                    transactionNo = transactionNo,
                    bankCode = bankCode,
                    payDate = payDate
                )
            } else {
                // ❌ THANH TOÁN THẤT BẠI
                val errorMessage = getVnPayErrorMessage(responseCode)
                showResultDialog(
                    isSuccess = false,
                    title = "Thanh toán thất bại ❌",
                    message = "Giao dịch không thành công.\nLỗi: $errorMessage\nMã lỗi: $responseCode"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi xử lý ReturnUrl: ${e.message}")
            showResultDialog(
                isSuccess = false,
                title = "Lỗi",
                message = "Đã xảy ra lỗi khi xử lý kết quả thanh toán."
            )
        }
    }

    /**
     * Hiển thị dialog thông báo kết quả thanh toán.
     * Khi đóng dialog, trả kết quả về CheckoutActivity.
     *
     * @param isSuccess true nếu thanh toán thành công
     * @param title     Tiêu đề dialog
     * @param message   Nội dung dialog
     */
    private fun showResultDialog(
        isSuccess: Boolean,
        title: String,
        message: String,
        transactionNo: String = "",
        bankCode: String = "",
        payDate: String = ""
    ) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ ->
                // Trả kết quả + thông tin giao dịch về cho CheckoutActivity
                val resultIntent = Intent().apply {
                    putExtra(RESULT_PAYMENT_SUCCESS, isSuccess)
                    putExtra(RESULT_TRANSACTION_NO, transactionNo)
                    putExtra(RESULT_BANK_CODE, bankCode)
                    putExtra(RESULT_PAY_DATE, payDate)
                }
                setResult(
                    if (isSuccess) Activity.RESULT_OK else Activity.RESULT_CANCELED,
                    resultIntent
                )
                finish()
            }
            .show()
    }

    /**
     * Chuyển đổi mã lỗi VNPAY sang thông báo tiếng Việt cho người dùng.
     *
     * @param responseCode Mã phản hồi từ VNPAY
     * @return Chuỗi mô tả lỗi bằng tiếng Việt
     */
    private fun getVnPayErrorMessage(responseCode: String): String {
        return when (responseCode) {
            "07" -> "Trừ tiền thành công. Giao dịch bị nghi ngờ gian lận."
            "09" -> "Thẻ/Tài khoản chưa đăng ký Internet Banking."
            "10" -> "Xác thực thông tin thẻ/tài khoản không đúng quá 3 lần."
            "11" -> "Hết hạn chờ thanh toán. Vui lòng thử lại."
            "12" -> "Thẻ/Tài khoản bị khóa."
            "13" -> "Nhập sai mật khẩu OTP. Vui lòng thử lại."
            "24" -> "Giao dịch đã bị hủy bởi khách hàng."
            "51" -> "Tài khoản không đủ số dư để thanh toán."
            "65" -> "Tài khoản đã vượt quá hạn mức giao dịch trong ngày."
            "75" -> "Ngân hàng thanh toán đang bảo trì."
            "79" -> "Nhập sai mật khẩu thanh toán quá số lần quy định."
            "99" -> "Lỗi không xác định."
            else -> "Lỗi không xác định (Mã: $responseCode)"
        }
    }

    /**
     * Xử lý nút Back vật lý trên điện thoại.
     * Nếu WebView có thể quay lại trang trước → quay lại.
     * Nếu không → hỏi xác nhận hủy thanh toán.
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.vnpayWebView.canGoBack()) {
            binding.vnpayWebView.goBack()
        } else {
            binding.btnBack.performClick()
        }
    }
}
