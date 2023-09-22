package com.example.blueteethtest

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions

fun toast(s: String) {
    Toast.makeText(
        MyApplication.instance.applicationContext,
        s,
        Toast.LENGTH_SHORT
    ).show()
}


class MainActivity : AppCompatActivity() {
    private val REQUEST_ENABLE_BT: Int = 1
    lateinit var perms: Array<String>
    lateinit var open: TextView
    lateinit var search: TextView
    lateinit var close: TextView
    val btAdapter by lazy {
        val btManager =
            this@MainActivity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        btManager.adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        open = findViewById(R.id.tv_open)
        search = findViewById(R.id.tv_search)
        close = findViewById(R.id.tv_close)
        initPermission()
        initListener()
    }

    fun initListener() {
        open.setOnClickListener(object : View.OnClickListener {
            @SuppressLint("MissingPermission")
            override fun onClick(p0: View?) {
                toast("打开蓝牙 权限:${XXPermissions.isGranted(this@MainActivity, perms)}")
                if (XXPermissions.isGranted(
                        this@MainActivity,
                        perms
                    ) && btAdapter?.isEnabled == false
                ) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                }
            }

        })

        search.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                // TODO: 搜索
                toast("搜索蓝牙 权限:${XXPermissions.isGranted(this@MainActivity, perms)}")
                startActivity(Intent(this@MainActivity,FindBluetoothActivity::class.java))
            }

        })

        close.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                // TODO: 关闭蓝牙
                toast("关闭蓝牙 权限:${XXPermissions.isGranted(this@MainActivity, perms)}")
            }

        })
    }


    fun initPermission() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
            //Android 12  需要动态申请的蓝牙权限
            perms = arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            perms = arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        XXPermissions.with(this)
            .permission(perms)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                    if (!all) {
                        toast("获取部分权限成功，但部分权限未正常授予")
                        return
                    }
                    toast("获取权限成功")
                }

                override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                    if (never) {
                        toast("被永久拒绝授权，请手动授予权限")
                        // 如果是被永久拒绝就跳转到应用权限系统设置页面
                        XXPermissions.startPermissionActivity(this@MainActivity, permissions)
                    } else {
                        toast("获取权限失败")
                    }

                }
            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_ENABLE_BT -> toast("我他妈打开蓝牙")
        }
    }
}