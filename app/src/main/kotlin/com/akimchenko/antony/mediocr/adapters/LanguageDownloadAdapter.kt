package com.akimchenko.antony.mediocr.adapters

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context.DOWNLOAD_SERVICE
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.akimchenko.antony.mediocr.MainActivity
import com.akimchenko.antony.mediocr.R
import com.akimchenko.antony.mediocr.fragments.LanguageFragment
import com.akimchenko.antony.mediocr.utils.AppSettingsComponent
import com.akimchenko.antony.mediocr.utils.NotificationCenter
import com.akimchenko.antony.mediocr.utils.Utils
import org.koin.standalone.KoinComponent
import org.koin.standalone.get
import java.io.File
import java.util.*


class LanguageDownloadAdapter(fragment: LanguageFragment, var items: ArrayList<String>) :
        RecyclerView.Adapter<LanguageDownloadAdapter.ViewHolder>(), NotificationCenter.Observer, KoinComponent {


    val activity: MainActivity? = fragment.activity as MainActivity?
    private val appSettings = get<AppSettingsComponent>()

    fun resume() {
        NotificationCenter.addObserver(this)
        notifyDataSetChanged()
    }

    fun pause() = NotificationCenter.removeObserver(this)

    abstract inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun updateUI(position: Int)
    }

    override fun onNotification(id: Int, `object`: Any?) {
        when (id) {
            NotificationCenter.LANG_DELETED -> {
                if (`object` == appSettings.getSelectedLanguage())
                    appSettings.setSelectedLanguage(null)
                notifyDataSetChanged()
            }
            NotificationCenter.LANG_DOWNLOAD_STATUS_CHANGED -> notifyItemChanged(items.indexOf(`object` as String))
        }
    }

    private inner class AvailableLangViewHolder(itemView: View) : ViewHolder(itemView) {

        val downloadDeleteButton: ImageView = itemView.findViewById(R.id.download_button)
        val title: TextView = itemView.findViewById(R.id.text_view)
        val checkMark: ImageView = itemView.findViewById(R.id.checkmark)
        val progressbar: ProgressBar = itemView.findViewById(R.id.progress_bar)

        init {
            itemView.setOnClickListener {
                activity ?: return@setOnClickListener
                val item = items[adapterPosition] as String? ?: return@setOnClickListener
                if (!Utils.isLanguageDownloaded(activity, item) && item != "eng")
                    download(item, File(activity.getTesseractDataFolder(), "$item.traineddata"), Utils.getLocalizedLangName(item))

                appSettings.setSelectedLanguage(item)
                notifyDataSetChanged()
            }
            downloadDeleteButton.setOnClickListener {
                activity ?: return@setOnClickListener
                val item = items[adapterPosition] as String? ?: return@setOnClickListener
                val file = File(activity.getTesseractDataFolder(), "$item.traineddata")
                if (Utils.isLanguageDownloaded(activity, item)) {
                    AlertDialog.Builder(activity)
                            .setMessage("${activity.getString(R.string.do_you_want_to_delete)} ${Utils.getLocalizedLangName(item)}")
                            .setPositiveButton(activity.getString(R.string.delete)) { dialog, _ ->
                                if (file.exists())
                                    file.delete()
                                NotificationCenter.notify(NotificationCenter.LANG_DELETED, item)
                                dialog.dismiss()
                            }.setNegativeButton(activity.getString(R.string.cancel)) { dialog, _ ->
                                dialog.dismiss()
                            }.create().show()

                } else {
                    download(items[adapterPosition], file, item)
                }
                notifyItemChanged(adapterPosition)
            }
        }

        override fun updateUI(position: Int) {
            activity ?: return
            val item = items[position] as String? ?: return
            if (item != "eng") {
                val isDownloaded = Utils.isLanguageDownloaded(activity, item)
                val isDownloading = activity.downloadIdsLangs.containsValue(item)
                downloadDeleteButton.isClickable = !isDownloading
                downloadDeleteButton.isFocusable = !isDownloading
                if (isDownloading) {
                    downloadDeleteButton.visibility = View.GONE
                    progressbar.visibility = View.VISIBLE
                } else {
                    downloadDeleteButton.visibility = View.VISIBLE
                    progressbar.visibility = View.GONE
                }
                downloadDeleteButton.setImageDrawable(ContextCompat.getDrawable(activity, if (isDownloaded) R.drawable.delete else R.drawable.download))
            } else {
                downloadDeleteButton.visibility = View.GONE
                progressbar.visibility = View.GONE
            }
            title.text = Utils.getLocalizedLangName(item)
            val isSelected = appSettings.getSelectedLanguage() == item
            checkMark.visibility = if (isSelected) View.VISIBLE else View.GONE
            title.setPadding(if (isSelected) 0 else activity.resources.getDimensionPixelSize(R.dimen.default_side_margin), 0, 0, 0)
        }
    }

    private fun download(lang: String, destFile: File, fileName: String) {
        activity ?: return
        val request = DownloadManager.Request(Uri.parse("https://github.com/tesseract-ocr/tessdata/blob/master/$lang.traineddata?raw=true"))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setTitle(fileName)
                .setDestinationUri(Uri.fromFile(destFile))
                .setAllowedOverRoaming(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            request.setAllowedOverMetered(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            request.setRequiresCharging(false)

        val downloadManager = activity.getSystemService(DOWNLOAD_SERVICE) as DownloadManager?
                ?: return
        activity.downloadIdsLangs[downloadManager.enqueue(request)] = lang
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageDownloadAdapter.ViewHolder {
        return AvailableLangViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_language, parent, false))
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: LanguageDownloadAdapter.ViewHolder, position: Int) = holder.updateUI(position)
}