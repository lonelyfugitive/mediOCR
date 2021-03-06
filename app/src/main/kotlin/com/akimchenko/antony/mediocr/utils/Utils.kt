package com.akimchenko.antony.mediocr.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.StateListDrawable
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import java.io.*
import java.text.DateFormat
import java.util.*

object Utils {

    const val READ_WRITE_CAMERA_REQUEST_CODE = 101
    const val CAPTURE_IMAGE_REQUEST_CODE = 102
    const val GALLERY_CHOOSER_REQUEST_CODE = 103
    const val READ_WRITE_GALLERY_REQUEST_CODE = 104
    const val READ_WRITE_INTENT_REQUEST_CODE = 105

    @JvmStatic
    val customLanguageTags: HashMap<String, String> = hashMapOf(
            Pair("aze_cyrl", "Azerbaijani - Cyrillic"),
            Pair("chi_sim", "Chinese - Simplified"),
            Pair("chi_sim_vert", "Chinese - Simplified Vertical"),
            Pair("chi_tra", "Chinese - Traditional"),
            Pair("chi_tra_vert", "Chinese - Traditional Vertical"),
            Pair("dan_frak", "Danish - Fraktur"),
            Pair("equ", "Math / equation detection"),
            Pair("frk", "Frankish"),
            Pair("jpn_vert", "Japanese - Vertical"),
            Pair("kat_old", "Georgian - Old"),
            Pair("kmr", "Kurdish"),
            Pair("kor_vert", "Korean - Vertical"),
            Pair("osd", "Orientation and script detection"),
            Pair("slk_frak", "Slovak - Fraktur"),
            Pair("spa_old", "Spanish Old"),
            Pair("srp_latn", "Serbian - Latin"),
            Pair("uzb_cyrl", "Uzbek - Cyrillic"),
            Pair("deu_frak", "German - Fraktur"),
            Pair("ita_old", "Italian - Old")
    )

    @JvmStatic
    fun getLocalizedLangName(item: String): String = customLanguageTags[item]
            ?: Locale(item).displayLanguage

    @JvmStatic
    fun makeSelector(context: Context, bitmap: Bitmap): StateListDrawable =
            StateListDrawable().apply {
                val resources = context.resources
                this.setExitFadeDuration(60)
                val pressedState = BitmapDrawable(resources, bitmap)
                pressedState.setColorFilter(
                        ContextCompat.getColor(context, R.color.selected_tint),
                        PorterDuff.Mode.SRC_ATOP
                )
                this.addState(intArrayOf(android.R.attr.state_pressed), pressedState)
                this.addState(intArrayOf(), BitmapDrawable(resources, bitmap))
            }

    @JvmStatic
    fun formatDate(date: Long): String = DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault()).format(date)

    @JvmStatic
    fun writeBitmapToFile(bitmap: Bitmap, file: File) {
        try {
            file.createNewFile()
            val fos = FileOutputStream(file)
            val bos = BufferedOutputStream(fos, 2024 * 8)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
            bos.flush()
            bos.close()
            fos.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun isLanguageDownloaded(activity: MainActivity, item: String): Boolean =
            activity.getTesseractDataFolder().listFiles()?.find { it.name == "$item.traineddata" } != null

    @JvmStatic
    fun getInternalDirs(context: Context): Array<File?> {
        /*return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            context.getExternalFilesDirs(null)
        else
            arrayOf(context.getExternalFilesDir(null))*/
        return arrayOf(context.getExternalFilesDir(null))
    }

    @JvmStatic
    fun shareFile(context: Context, file: File) =
            Intent(Intent.ACTION_SEND).apply {
                val fileUri =
                        FileProvider.getUriForFile(context, context.applicationContext.packageName + ".provider", file)
                this.type = context.contentResolver.getType(fileUri)
                this.putExtra(Intent.EXTRA_STREAM, fileUri)
                context.startActivity(Intent.createChooser(this, null))
            }

    @JvmStatic
    fun isCamera2APISupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && AppSettings.useApplicationCamera) {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager?
                    ?: return false
            for (cameraId in manager.cameraIdList) {
                val characteristics: Int? = manager.getCameraCharacteristics(cameraId)
                        .get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                if (characteristics == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                    return true
                }
            }
        }
        return false
    }
}