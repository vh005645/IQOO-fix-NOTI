package com.fcmkeeper.iqoo

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.text.format.DateFormat
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {

    private companion object {
        const val OK = 1
        const val BAD = 0
        const val WAIT = -1
        val GREEN = Color.parseColor("#2E7D32")
        val BLUE = Color.parseColor("#1565C0")
        val RED = Color.parseColor("#C62828")
        val ORANGE = Color.parseColor("#EF6C00")
        val GRAY = Color.parseColor("#757575")
    }

    private lateinit var connectTime: TextView
    private lateinit var lastDiscLabel: TextView
    private lateinit var lastDiscView: TextView
    private lateinit var rowGms: TextView
    private lateinit var rowNotif: TextView
    private lateinit var rowBattery: TextView
    private lateinit var rowService: TextView
    private lateinit var rowBind: TextView
    private lateinit var rowNet: TextView
    private lateinit var rowFcm: TextView
    private lateinit var info: TextView
    private lateinit var serviceSwitch: Switch
    private lateinit var bindSwitch: Switch

    private var updatingUi = false
    private var tickCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            updateHeader()
            if (tickCount++ % 3 == 0) refresh()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        connectTime = TextView(this).apply {
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
        }
        lastDiscLabel = TextView(this).apply {
            text = "Mất kết nối gần nhất"
            textSize = 13f
            setTextColor(GRAY)
            setPadding(0, pad / 4, 0, 0)
        }
        lastDiscView = TextView(this).apply {
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, pad)
        }
        root.addView(connectTime)
        root.addView(lastDiscLabel)
        root.addView(lastDiscView)

        fun row(): TextView {
            val t = TextView(this)
            t.textSize = 16f
            t.setPadding(0, pad / 4, 0, pad / 4)
            root.addView(t)
            return t
        }
        rowGms = row()
        rowNotif = row()
        rowBattery = row()
        rowService = row()
        rowBind = row()
        rowNet = row()
        rowFcm = row()

        info = TextView(this)
        info.textSize = 13f
        info.setTextColor(GRAY)
        info.setPadding(0, pad / 2, 0, pad / 2)
        root.addView(info)

        serviceSwitch = Switch(this)
        serviceSwitch.text = "Dịch vụ giữ kết nối"
        serviceSwitch.textSize = 17f
        serviceSwitch.setPadding(0, pad / 2, 0, pad / 2)
        serviceSwitch.setOnCheckedChangeListener { _, checked ->
            if (!updatingUi) {
                setServiceEnabled(checked)
                refresh()
            }
        }
        root.addView(serviceSwitch)

        bindSwitch = Switch(this)
        bindSwitch.text = "Giữ liên kết với GMS (chống đóng băng)"
        bindSwitch.textSize = 17f
        bindSwitch.setPadding(0, pad / 2, 0, pad)
        bindSwitch.setOnCheckedChangeListener { _, checked ->
            if (!updatingUi) {
                setBindingEnabled(checked)
                refresh()
            }
        }
        root.addView(bindSwitch)

        fun button(text: String, action: () -> Unit) {
            val b = Button(this)
            b.text = text
            b.setOnClickListener {
                action()
                refresh()
            }
            root.addView(b)
        }
        button("Cấp quyền thông báo") { requestNotificationPermission() }
        button("Bỏ tối ưu pin cho app này") { requestIgnoreBattery() }
        button("Mở cài đặt chạy nền / tự khởi động") { openRomSettings() }
        button("Mở cài đặt Google Play Services") { openAppInfo(GmsHelper.GMS) }
        button("Kiểm tra kết nối ngay") {
            val app = applicationContext
            Thread { Tick.run(app) }.start()
            Toast.makeText(this, "Đang kiểm tra…", Toast.LENGTH_SHORT).show()
        }

        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            addView(root)
        })
    }

    override fun onResume() {
        super.onResume()
        tickCount = 0
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    override fun onPause() {
        handler.removeCallbacks(ticker)
        super.onPause()
    }

    private fun setRow(tv: TextView, level: Int, text: String) {
        val icon = when (level) {
            OK -> "✅"
            BAD -> "❌"
            else -> "❔"
        }
        tv.text = "$icon  $text"
        tv.setTextColor(
            when (level) {
                OK -> GREEN
                BAD -> RED
                else -> ORANGE
            }
        )
    }

    private fun refresh() {
        val enabled = Prefs.isEnabled(this)
        val bindOn = Prefs.bindEnabled(this)

        updatingUi = true
        serviceSwitch.isChecked = enabled
        bindSwitch.isChecked = bindOn
        updatingUi = false

        val gms = GmsHelper.isInstalled(this)
        val gmsSystem = GmsHelper.isSystemApp(this)
        val notif = NotificationManagerCompat.from(this).areNotificationsEnabled()
        val pm = getSystemService(PowerManager::class.java)
        val battery = pm.isIgnoringBatteryOptimizations(packageName)
        val net = Adaptive.hasNetwork(this)

        setRow(
            rowGms, if (gms) OK else BAD,
            if (gms) "Google Play Services: " + (if (gmsSystem) "app hệ thống" else "app cài thêm")
            else "Google Play Services: chưa cài"
        )
        setRow(rowNotif, if (notif) OK else BAD, "Quyền thông báo")
        setRow(rowBattery, if (battery) OK else BAD, "Bỏ tối ưu pin")
        setRow(rowService, if (enabled) OK else BAD, "Dịch vụ giữ kết nối")

        val bs = GmsBinder.state
        when {
            !bindOn -> setRow(rowBind, BAD, "Liên kết GMS: tắt")
            bs == "bound" -> setRow(rowBind, OK, "Liên kết GMS: đã liên kết")
            bs == "failed" -> setRow(rowBind, BAD, "Liên kết GMS: không liên kết được")
            bs == "connecting" -> setRow(rowBind, WAIT, "Liên kết GMS: đang kết nối")
            else -> setRow(rowBind, WAIT, "Liên kết GMS: chưa chạy")
        }

        setRow(rowNet, if (net) OK else BAD, "Mạng")

        val conn = Prefs.connState(this)
        val code = Prefs.statusCode(this)
        val err = Prefs.statusError(this)
        when (conn) {
            1 -> setRow(rowFcm, OK, "Kết nối FCM: đang kết nối" + (code?.let { " (mã $it)" } ?: ""))
            0 -> setRow(
                rowFcm, BAD,
                "Kết nối FCM: mất kết nối" + (code?.let { " (mã $it)" } ?: (err?.let { " ($it)" } ?: ""))
            )
            else -> setRow(rowFcm, WAIT, "Kết nối FCM: chưa đọc được" + (err?.let { " ($it)" } ?: ""))
        }

        val fmt = "HH:mm:ss dd/MM"
        val lastRe = Prefs.lastReconnect(this)
        val last = Prefs.lastBeat(this)
        val next = Prefs.nextBeat(this)
        val every = Prefs.intervalMin(this)
        info.text = "Reconnect gần nhất: " + (if (lastRe == 0L) "chưa có" else DateFormat.format(fmt, lastRe)) +
            "\nHeartbeat gần nhất: " + (if (last == 0L) "chưa có" else DateFormat.format(fmt, last)) +
            "\nHeartbeat tiếp theo: " +
            (if (!enabled || next == 0L) "—" else DateFormat.format(fmt, next).toString() +
                (if (every > 0) " (mỗi ~$every phút)" else ""))

        updateHeader()
    }

    /** Top block: connect time (large) and the most recent disconnect (status code 3). */
    private fun updateHeader() {
        val now = System.currentTimeMillis()
        val enabled = Prefs.isEnabled(this)
        val conn = Prefs.connState(this)
        val since = Prefs.connectedSince(this)

        when {
            enabled && conn == 1 && since > 0L -> {
                connectTime.text = "Connect time: " + formatDuration(now - since)
                connectTime.setTextColor(BLUE)
            }
            enabled && conn == 0 -> {
                connectTime.text = "Connect time: mất kết nối"
                connectTime.setTextColor(RED)
            }
            else -> {
                connectTime.text = "Connect time: —"
                connectTime.setTextColor(GRAY)
            }
        }

        val last = Prefs.lastDisconnect(this)
        lastDiscView.text = if (last == 0L) "Chưa có"
        else DateFormat.format("HH:mm:ss  •  dd/MM/yyyy", last).toString()
    }

    private fun formatDuration(ms: Long): String {
        val s = (ms / 1000L).coerceAtLeast(0L)
        val d = s / 86400L
        val h = (s % 86400L) / 3600L
        val m = (s % 3600L) / 60L
        val sec = s % 60L
        return (if (d > 0) "$d ngày " else "") + "%02d:%02d:%02d".format(h, m, sec)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private fun requestIgnoreBattery() {
        val pm = getSystemService(PowerManager::class.java)
        if (pm.isIgnoringBatteryOptimizations(packageName)) return
        try {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
            )
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    private fun openRomSettings() {
        if (!RomSettings.openBackgroundSettings(this)) {
            RomSettings.openAppDetails(this, packageName)
            Toast.makeText(this, "Đã mở thông tin app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAppInfo(pkg: String) {
        if (!GmsHelper.isInstalled(this, pkg)) {
            Toast.makeText(this, "Chưa cài $pkg", Toast.LENGTH_SHORT).show()
            return
        }
        RomSettings.openAppDetails(this, pkg)
    }

    private fun setServiceEnabled(on: Boolean) {
        if (on) {
            Prefs.setEnabled(this, true)
            try {
                ContextCompat.startForegroundService(this, Intent(this, KeepAliveService::class.java))
            } catch (e: Exception) {
                Prefs.setEnabled(this, false)
                Toast.makeText(this, "Không khởi động được: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Prefs.setEnabled(this, false)
            Prefs.setConnState(this, -1)
            HeartbeatReceiver.cancel(this)
            stopService(Intent(this, KeepAliveService::class.java))
        }
    }

    private fun setBindingEnabled(on: Boolean) {
        Prefs.setBindEnabled(this, on)
        if (on) {
            if (Prefs.isEnabled(this)) GmsBinder.start(this)
        } else {
            GmsBinder.stop(this)
        }
    }
}
