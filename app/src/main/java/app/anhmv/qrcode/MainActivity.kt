package app.anhmv.qrcode

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import app.anhmv.qrcode.databinding.ActivityMainBinding
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView


class MainActivity : AppCompatActivity(), DecoratedBarcodeView.TorchListener {

    private lateinit var binding: ActivityMainBinding

    private lateinit var captureManager: CustomCaptureManager

    private var isTurnOn = false

    private var isScan = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!hasFlash()) {
            binding.btnFlight.visibility = View.GONE
        }

        captureManager = CustomCaptureManager(this, binding.zxingBarcodeScanner, this::onDetected)
        captureManager.apply {
            setFrontCamera()
            setShowMissingCameraPermissionDialog(true)
            startScan()
        }

        handleEvents()
    }

    private fun onDetected(barcodeResult: BarcodeResult?) {
        isScan = false
        Toast.makeText(this, barcodeResult?.text ?: "not found!", Toast.LENGTH_LONG).show()
    }

    private fun handleEvents() {
        binding.btnFlight.setOnClickListener {
            isTurnOn = !isTurnOn
            switchFlight(isTurnOn)
        }

        binding.btnRetry.setOnClickListener {
            if (!isScan) {
                captureManager.onResume()
                isScan = true
            }
        }
    }

    private fun switchFlight(value: Boolean) {
        if (value) {
            binding.zxingBarcodeScanner.setTorchOn()
        } else {
            binding.zxingBarcodeScanner.setTorchOff()
        }
    }

    private fun hasFlash(): Boolean {
        return applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }

    override fun onResume() {
        super.onResume()
        captureManager.onResume()
    }

    override fun onPause() {
        super.onPause()
        captureManager.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        captureManager.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return binding.zxingBarcodeScanner.onKeyDown(keyCode, event)
                || super.onKeyDown(keyCode, event)
    }

    override fun onTorchOn() {
        binding.btnFlight.text = "On"
    }

    override fun onTorchOff() {
        binding.btnFlight.text = "Off"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        captureManager.onRequestPermissionResult(requestCode, permissions, grantResults)
    }
}
