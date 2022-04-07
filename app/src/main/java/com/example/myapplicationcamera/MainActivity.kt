package com.example.myapplicationcamera

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.PersistableBundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.FileProvider
import com.example.myapplicationcamera.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var bitmapView: Bitmap? = null

    private val cameraLauncher = registerForActivityResult(Contract()) {
        it?.let { outPut ->
            if(outPut.confirmed) {
                setPic()
            } else {
                Log.d("MainActivity cameraLauncher", "cameraLauncher is not confirmed")
            }
        } ?: Log.d("MainActivity cameraLauncher", "cameraLauncher bad result")
    }
    private var currentPhotoPath: String = ""

    private fun setPic() {
        // Get the dimensions of the View
        val targetW: Int = binding.imageView.width
        val targetH: Int = binding.imageView.height

        val bmOptions = BitmapFactory.Options().apply {
            // Get the dimensions of the bitmap
            inJustDecodeBounds = true

            BitmapFactory.decodeFile(currentPhotoPath, this)

            val photoW: Int = outWidth
            val photoH: Int = outHeight

            // Determine how much to scale down the image
            val scaleFactor: Int = Math.max(1, Math.min(photoW / targetW, photoH / targetH))

            // Decode the image file into a Bitmap sized to fill the View
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
        }

        BitmapFactory.decodeFile(currentPhotoPath, bmOptions)?.also { bitmap: Bitmap ->
            bitmapView = bitmap
            binding.imageView.setImageBitmap(bitmap)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }

        savedInstanceState?.also {
            binding.imageView.setImageBitmap(it.getParcelable("bitmapView"))
        }

        binding.button.setOnClickListener {
            createPhotoFile()?.also { photoFile->
                cameraLauncher.launch(photoFile)
            } ?: Log.d("MainActivity createPhotoFile", "createPhotoFile is null")
        }

        binding.buttonDF.setOnClickListener {
            if (currentPhotoPath.isNotEmpty()){
                val file = File(currentPhotoPath)
                if(file.exists()){
                    if(file.delete()) {
                        Log.d("MainActivity buttonDF.setOnClickListener", "file.delete")
                    }
                }
            }
        }
    }

    private fun createPhotoFile() : File? {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return  try {
            // Create an image file name
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            return File.createTempFile(
                "JPEG_${timeStamp}_", /* prefix */
                ".jpg", /* suffix */
                storageDir /* directory */
            ).apply {
                // Save a file: path for use with ACTION_VIEW intents
                currentPhotoPath = absolutePath
                binding.textView.text = currentPhotoPath
            }
        } catch (ex: IOException) {
            null
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putParcelable("bitmapView", bitmapView)
    }
}

data class Output(
    val confirmed: Boolean
)

class Contract : ActivityResultContract<File, Output>() {

    override fun parseResult(resultCode: Int, data: Intent?): Output? {
        val confirmed = resultCode == RESULT_OK
        return Output(confirmed)
    }

    override fun createIntent(context: Context, photoFile: File): Intent {

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // Continue only if the File was successfully created
        photoFile.also {
            val photoURI: Uri = FileProvider.getUriForFile(
                context,
                "com.example.android.fileprovider",
                it
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        }

        return intent
    }

}

