package com.coreproc.kotlin.kotlinbase.ui.main

import android.app.Activity
import android.content.Intent
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.coreproc.kotlin.kotlinbase.App
import com.coreproc.kotlin.kotlinbase.R
import com.coreproc.kotlin.kotlinbase.databinding.ActivityMainBinding
import com.coreproc.kotlin.kotlinbase.extensions.showShortToast
import com.coreproc.kotlin.kotlinbase.misc.AppPreferences
import com.coreproc.kotlin.kotlinbase.ui.base.BaseActivity
import com.coreproc.kotlin.kotlinbase.utils.SensorDetectorService
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import net.ozaydin.serkan.easy_csv.EasyCsv
import net.ozaydin.serkan.easy_csv.FileCallback
import net.ozaydin.serkan.easy_csv.Utility.PermissionUtility
import net.ozaydin.serkan.easy_csv.Utility.Utils
import java.io.*
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class MainActivity : BaseActivity() {

    companion object {
        private const val LOG_STARTED = "Started service!"
        private const val LOG_STOP = "Started stop!"
        private const val LOG_SAVED = "Saved: "
        private const val LOG_SAVING = "Saving..."
        private const val LOG_ERROR_SAVING = "ERROR SAVING: "

    }

    private var viewModel: MainViewModel? = null

    private lateinit var activityMainBinding: ActivityMainBinding

    private lateinit var intentService: Intent

    override fun getLayoutResource(): Int = R.layout.activity_main

    override fun initialize() {

        viewModel = initViewModel(MainViewModel::class.java)
        viewModel!!.success.observe(this) { /* onSuccess(it) */ }

        intentService = Intent(applicationContext, SensorDetectorService::class.java)
        activityMainBinding = ActivityMainBinding.bind(getChildActivityView())

        activityMainBinding.startButton.setOnClickListener {
            AppPreferences.saveSensorData("")
            startService(intentService)
            updateLog(LOG_STARTED)
        }

        activityMainBinding.stopButton.setOnClickListener {
            stopService(intentService)
            stopAndExportToCSV()
            updateLog(LOG_STOP)
        }

        activityMainBinding.dataTextview.text = "No Data\n"

    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel!!.success.removeObservers(this)
        AppPreferences.saveSensorData("")
    }

    private fun updateLog(log: String) {
        activityMainBinding.dataTextview.text =
            "${activityMainBinding.dataTextview.text}$log\n"
    }

    private fun stopAndExportToCSV() {
        val timestamp = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
            .withZone(ZoneOffset.systemDefault())
            .format(Instant.now())

        Handler(Looper.myLooper()!!).postDelayed({
            updateLog(LOG_SAVING)

            class ModifiedEasyCSV: EasyCsv(this) {

                private lateinit var file: File
                private lateinit var outputStream: OutputStream

                override fun createCsvFile(
                    fileName: String?,
                    headerList: MutableList<String>?,
                    data: MutableList<String>?,
                    permissionRequestCode: Int,
                    fileCallback: FileCallback?
                ) {
                    if (PermissionUtility.askPermissionForActivity(
                            this@MainActivity,
                            "android.permission.WRITE_EXTERNAL_STORAGE",
                            permissionRequestCode
                        )
                    ) {
                        val newFileName = fileName!!.replace(" ", "_").replace(":", "_")
                        val fileDir = App.instance!!.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                        if (fileDir == null || !fileDir.exists()){
                            fileDir!!.mkdirs()
                        }

                        file = File(
                            fileDir,
                            "$newFileName.csv"
                        )
                        try {
                            file.createNewFile()
                        } catch (var8: IOException) {
                            fileCallback!!.onFail(var8.message)
                        }
                        val headerListWithComma = Utils.separatorReplace(
                            "&", "*", headerList
                        )
                        val dataListWithComma = Utils.separatorReplace(
                            "&", "*", data
                        )
                        file = writeDataToFile(
                            file,
                            containAllData(headerListWithComma, dataListWithComma), fileCallback!!
                        )
                        fileCallback.onSuccess(file)
                    } else {
                        fileCallback!!.onFail("Write Permission Error")
                    }
                }


                private fun writeDataToFile(
                    file: File,
                    dataList: List<String>,
                    fileCallback: FileCallback
                ): File {
                    return if (file.exists()) {
                        try {
                            outputStream = FileOutputStream(file)
                        } catch (var6: FileNotFoundException) {
                            fileCallback.onFail(var6.message)
                        }
                        val finalFo = this.outputStream
                        val headerArray: Array<String?> = dataList.toTypedArray() as Array<String?>
                        Observable.fromArray(*headerArray).subscribe(object : Observer<Any?> {
                            override fun onSubscribe(d: Disposable) {}

                            override fun onError(e: Throwable) {
                                fileCallback.onFail(e.message)
                            }

                            override fun onComplete() {
                                try {
                                    finalFo.close()
                                } catch (var2: IOException) {
                                    var2.printStackTrace()
                                }
                            }

                            override fun onNext(t: Any) {
                                val dataWithLineBreak = t as String?
                                try {
                                    finalFo.write(dataWithLineBreak!!.toByteArray())
                                } catch (var4: IOException) {
                                    fileCallback.onFail(var4.message)
                                }
                            }
                        })
                        file
                    } else {
                        fileCallback.onFail("Couldn't create CSV file")
                        file
                    }
                }

                private fun containAllData(
                    headerList: List<String>,
                    dataList: List<String>
                ): List<String> {
                    val stringList = mutableListOf<String>()
                    var var4: Iterator<*> = headerList.iterator()
                    var value: String?
                    while (var4.hasNext()) {
                        value = var4.next() as String
                        stringList.add(value)
                    }
                    var4 = dataList.iterator()
                    while (var4.hasNext()) {
                        value = var4.next()
                        stringList.add(value)
                    }
                    return stringList
                }
            }

            val modifiedEasyCSV = ModifiedEasyCSV()
            val headerList = arrayListOf<String>()
            headerList.add("TIMESTAMP&SENSOR&X&Y&Z*")

            val dataList = arrayListOf<String>()

            // Manipulate Sensor Data
            val sensorData = AppPreferences.getSensorData().split("\n")
            sensorData.forEach {
                if (it.trim().isNotEmpty()) {
                    val splitData = it.split(",")
                    dataList.add("${splitData[0]},${splitData[1]},${splitData[2]},${splitData[3]},${splitData[4]}*")
                }
            }

            modifiedEasyCSV.createCsvFile(timestamp, headerList, dataList, 83475, object: FileCallback {

                override fun onSuccess(p0: File?) {
                    p0?.let {
                        updateLog(LOG_SAVED + "${it.absolutePath}")
                    }
                }

                override fun onFail(p0: String?) {
                    p0?.let {
                        updateLog("$LOG_ERROR_SAVING$p0")
                    }
                }
            });

        }, 1000)


    }

}
