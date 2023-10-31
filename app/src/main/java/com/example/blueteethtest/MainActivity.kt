package com.example.blueteethtest

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

fun toast(s: String) {
    Toast.makeText(
        MyApplication.instance.applicationContext,
        s,
        Toast.LENGTH_SHORT
    ).show()
}


class MainActivity : AppCompatActivity() {
    lateinit var perms: Array<String> //权限
    var isGranted = false
    lateinit var open: TextView
    lateinit var search: TextView
    lateinit var et_message: EditText
    lateinit var tv_message: TextView
    lateinit var send: TextView
    var mManager: MyBluetoothManager? = null
    var mService: MyBluetoothService? = null
    var mConnectDevice: BluetoothDevice? = null
    var mConnectDeviceName: String = ""

    var mHandler: Handler = Handler(Looper.getMainLooper()) {
        when (it.what) {
            Constants.MESSAGE_STATE_CHANGE -> {
                var s: String = ""
                when (it.arg1) {
                    STATE_CONNECTING -> s = "正在尝试连接"
                    STATE_CONNECTED -> s = "已连接${mConnectDeviceName}"
                    STATE_LISTEN -> s = "未连接"
                    STATE_NONE -> s = "未连接"
                }
                toast(s)
                true
            }

            Constants.MESSAGE_WRITE -> {
                val buffer = (it.obj) as ByteArray
                toast("发送了消息:${String(buffer)}")
                true
            }

            Constants.MESSAGE_READ -> {
                val buffer = (it.obj) as ByteArray
                tv_message.setText(String(buffer, 0, it.arg1))
                true
            }

            Constants.MESSAGE_DEVICE_NAME -> {
                mConnectDeviceName = it.data.getString(Constants.DEVICE_NAME) + ""
                toast("正在连接 ${it.data.getString(Constants.DEVICE_NAME)}")
                true
            }

            Constants.MESSAGE_TOAST -> {
                toast("${it.data.getString(Constants.TOAST)}")
                true
            }

            else -> {
                true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        open = findViewById(R.id.tv_open)
        search = findViewById(R.id.tv_search)
        et_message = findViewById(R.id.et_message)
        tv_message = findViewById(R.id.tv_get_message)
        send = findViewById(R.id.tv_send)
        initPermission()
        initListener()
        EventBus.getDefault().register(this)
    }

    fun initListener() {
        open.setOnClickListener(object : View.OnClickListener {
            @SuppressLint("MissingPermission")
            override fun onClick(p0: View?) {
                initPermission()
            }

        })

        search.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                if (!isGranted) {
                    toast("请先打开蓝牙权限")
                    return
                }
                startActivityForResult(
                    Intent(this@MainActivity, FindBluetoothActivity::class.java),
                    10001
                )
            }

        })

        send.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                if (!TextUtils.isEmpty(et_message.text)) {
                    mService?.write(et_message.text.toString())
                    et_message.setText("")
                }
            }
        })

    }

    fun initPermission() {
        perms = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
            //Android 12  需要动态申请的蓝牙权限
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
        }
        XXPermissions.with(this@MainActivity).permission(perms)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                    isGranted = all
                    if (!all) {
                        Toast.makeText(
                            this@MainActivity,
                            "获取部分权限成功，但部分权限未正常授予",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }
                    Toast.makeText(
                        this@MainActivity, "获取权限成功", Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                    isGranted = false
                    if (never) {
                        Toast.makeText(
                            this@MainActivity, "被永久拒绝授权，请手动授予权限", Toast.LENGTH_SHORT
                        ).show()
                        // 如果是被永久拒绝就跳转到应用权限系统设置页面
                        XXPermissions.startPermissionActivity(this@MainActivity, permissions)
                    } else {
                        Toast.makeText(
                            this@MainActivity, "获取权限失败", Toast.LENGTH_SHORT
                        ).show()
                    }

                }
            })
    }

    override fun onStart() {
        super.onStart()
        if (mService == null) {
            mService = MyBluetoothService(this@MainActivity, mHandler)
        }
        if (mManager == null) {
            mManager = MyBluetoothManager(this@MainActivity)
        }
    }

    override fun onResume() {
        super.onResume()
        if (mService != null && isGranted) {
            if (mService?.mState == STATE_NONE) {
                mService?.start()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe
    fun onConnect(event: DeviceEvent) {
        mConnectDevice = event.device
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            10001 -> {
                if (resultCode == RESULT_OK)
                    mService?.connect(mConnectDevice!!)
            }
        }
    }
}