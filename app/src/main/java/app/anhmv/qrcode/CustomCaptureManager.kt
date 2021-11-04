package app.anhmv.qrcode

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.zxing.client.android.BeepManager
import com.google.zxing.client.android.InactivityTimer
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CameraPreview
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.camera.CameraSettings

class CustomCaptureManager(
    private val activity: Activity,
    private val barcodeView: DecoratedBarcodeView,
    private val onDetected: (BarcodeResult?) -> Unit
) {
    private var finishWhenClosed = false

    private val inactivityTimer: InactivityTimer = InactivityTimer(activity) {
        Log.d(TAG, "Finishing due to inactivity")
        finish()
    }

    private val beepManager: BeepManager = BeepManager(activity)

    private val handler: Handler = Handler(Looper.getMainLooper())

    private var showDialogIfMissingCameraPermission = true

    private var missingCameraPermissionDialogMessage = ""

    private var destroyed = false

    private var askedPermission: Boolean = false

    private val callback: BarcodeCallback = BarcodeCallback { result: BarcodeResult? ->
        barcodeView.pause()
        beepManager.playBeepSoundAndVibrate()

        handler.postDelayed({ this@CustomCaptureManager.onDetected(result) }, 300)
    }

    private val stateListener: CameraPreview.StateListener = object : CameraPreview.StateListener {
        override fun previewSized() {}

        override fun previewStarted() {}

        override fun previewStopped() {}

        override fun cameraError(error: Exception?) {
            displayFrameworkBugMessageAndExit(
                activity.getString(R.string.zxing_msg_camera_framework_bug)
            )
        }

        override fun cameraClosed() {
            if (finishWhenClosed) {
                Log.d(TAG, "Camera closed; finishing activity")
                finish()
            }
        }
    }

    init {
        barcodeView.barcodeView.addStateListener(stateListener)
    }

    fun returnResultTimeout() {
        val intent = Intent(Intents.Scan.ACTION)
        intent.putExtra(Intents.Scan.TIMEOUT, true)
        activity.setResult(Activity.RESULT_CANCELED, intent)
        closeAndFinish()
    }

    fun startScan() {
        barcodeView.decodeContinuous(callback)
    }

    fun onResume() {
        openCameraWithPermission()
        inactivityTimer.start()
    }

    fun onPause() {
        inactivityTimer.cancel()
        barcodeView.pauseAndWait()
    }

    fun onDestroy() {
        destroyed = true
        inactivityTimer.cancel()
        handler.removeCallbacksAndMessages(null)
    }

    private fun openCameraWithPermission() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            barcodeView.resume()
        } else if (!askedPermission) {
            ActivityCompat.requestPermissions(
                activity, arrayOf(Manifest.permission.CAMERA),
                cameraPermissionCode
            )
            askedPermission = true
        }
    }

    fun onRequestPermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == cameraPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted
                barcodeView.resume();
            } else {
                setMissingCameraPermissionResult();

                if (showDialogIfMissingCameraPermission) {
                    displayFrameworkBugMessageAndExit(missingCameraPermissionDialogMessage);
                } else {
                    closeAndFinish();
                }
            }
        }
    }

    private fun closeAndFinish() {
        if (barcodeView.barcodeView.isCameraClosed) {
            finish()
        } else {
            finishWhenClosed = true
        }

        barcodeView.pause()
        inactivityTimer.cancel()
    }

    private fun finish() {
        activity.finish()
    }

    fun displayFrameworkBugMessageAndExit(message: String) {
        var title = message

        if (activity.isFinishing || destroyed || finishWhenClosed) {
            return
        }

        if (title.isEmpty()) {
            title = activity.getString(R.string.zxing_msg_camera_framework_bug)
        }

        AlertDialog.Builder(activity).apply {
            setTitle(activity.getString(R.string.zxing_app_name))
            setMessage(title)
            setPositiveButton(R.string.zxing_button_ok) { _, _ -> finish() }
            setOnCancelListener { _ -> finish() }
            show()
        }
    }

    private fun setMissingCameraPermissionResult() {
        val intent = Intent(Intents.Scan.ACTION)
        intent.putExtra(Intents.Scan.MISSING_CAMERA_PERMISSION, true)
        activity.setResult(Activity.RESULT_CANCELED, intent)
    }

    fun setShowMissingCameraPermissionDialog(visible: Boolean) {
        setShowMissingCameraPermissionDialog(visible, "");
    }

    fun setShowMissingCameraPermissionDialog(visible: Boolean, message: String?) {
        showDialogIfMissingCameraPermission = visible;
        missingCameraPermissionDialogMessage = message ?: ""
    }

    fun setFrontCamera() {
        val cameraSettings = CameraSettings()
        cameraSettings.requestedCameraId = 1
        barcodeView.cameraSettings = cameraSettings
    }

    companion object {
        private val TAG = CustomCaptureManager::class.java.simpleName

        private var cameraPermissionCode = 250
        private const val SAVED_ORIENTATION_LOCK = "SAVED_ORIENTATION_LOCK"

        fun getCameraPermissionCode(): Int {
            return cameraPermissionCode
        }

        fun setCameraPermissionCode(code: Int) {
            cameraPermissionCode = code
        }
    }
}
