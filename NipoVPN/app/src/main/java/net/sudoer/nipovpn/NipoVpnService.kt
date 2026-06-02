package net.sudoer.nipovpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File

class NipoVpnService : Service() {

    private var nipoProcess: Process? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startNipoVpn()
        return START_STICKY
    }

    override fun onDestroy() {
        stopNipoVpn()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startNipoVpn() {
        if (nipoProcess != null) {
            Log.i("NipoVPN", "Already running")
            return
        }

        try {
            val configFile = File(filesDir, "config.yaml")
            val logDir = File(filesDir, "logs")
            logDir.mkdirs()
            assets.open("config.yaml").use { input ->
                configFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val binaryFile = findNipoBinary()

            Log.i("NipoVPN", "nativeLibraryDir=${applicationInfo.nativeLibraryDir}")
            Log.i("NipoVPN", "Binary=${binaryFile.absolutePath}")
            Log.i("NipoVPN", "Binary exists=${binaryFile.exists()}")
            Log.i("NipoVPN", "Binary executable=${binaryFile.canExecute()}")
            Log.i("NipoVPN", "Config=${configFile.absolutePath}")

            nipoProcess = ProcessBuilder(
                binaryFile.absolutePath,
                "agent",
                configFile.absolutePath
            )
                .redirectErrorStream(true)
                .start()

            Log.i("NipoVPN", "Process started")

            Thread {
                try {
                    nipoProcess?.inputStream
                        ?.bufferedReader()
                        ?.forEachLine {
                            Log.i("NipoVPN", it)
                        }
                } catch (e: Exception) {
                    Log.e("NipoVPN", "stdout reader failed", e)
                }
            }.start()

        } catch (e: Exception) {
            Log.e("NipoVPN", "Failed to start NipoVPN", e)
        }
    }

    private fun findNipoBinary(): File {
        val direct = File(
            applicationInfo.nativeLibraryDir,
            "libnipovpn_exec.so"
        )

        if (direct.exists()) {
            return direct
        }

        val parent = File(applicationInfo.nativeLibraryDir).parentFile

        parent?.walkTopDown()?.forEach { file ->
            if (file.name == "libnipovpn_exec.so") {
                return file
            }
        }

        throw IllegalStateException(
            "libnipovpn_exec.so not found. nativeLibraryDir=${applicationInfo.nativeLibraryDir}"
        )
    }

    private fun stopNipoVpn() {
        try {
            nipoProcess?.destroy()
            nipoProcess = null
            Log.i("NipoVPN", "Process stopped")
        } catch (e: Exception) {
            Log.e("NipoVPN", "Failed to stop process", e)
        }
    }

    private fun createNotification(): Notification {
        val channelId = "nipovpn"

        val channel = NotificationChannel(
            channelId,
            "NipoVPN",
            NotificationManager.IMPORTANCE_LOW
        )

        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("NipoVPN")
            .setContentText("NipoVPN service is running")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .build()
    }
}