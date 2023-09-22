package com.example.blueteethtest

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class MyBluetoothManager {
    val deviceList = ArrayList<BluetoothDevice?>()
    val mList = ArrayList<BluetoothDeviceInfo>()
    val bluetoothDeviceAdapter = BluetoothDeviceAdapter(MyApplication.instance.applicationContext)
     val mBluetoothDeviceSearchLiveData: ValueKeeperLiveData<Resource<String>> =
        ValueKeeperLiveData()
     val mConnectDeviceLiveData: ValueKeeperLiveData<Resource<BluetoothDeviceInfo>> =
        ValueKeeperLiveData()

    init {
        bluetoothDeviceAdapter.deviceList = mList
    }

    val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(p0: Context?, p1: Intent) {
            val action: String? = p1.action
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    var device: BluetoothDevice?
                    if (Build.VERSION.SDK_INT >= 33) {
                        device = p1.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice::class.java
                        )
                    } else {
                        device = p1.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    if (judge(device)) {
                        return
                    }
                    deviceList.add(device)
                    val info = BluetoothDeviceInfo(device?.name, device?.address)
                    Log.d("MyBluetoothManager","device name ${device?.name} device address ${device?.address}")
                    mList.add(info)
                    mBluetoothDeviceSearchLiveData.postValue(Resource(Resource.LOADING, null, ""))
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    //搜索完成
                    mBluetoothDeviceSearchLiveData.postValue(Resource(Resource.SUCCESS, null, ""))
                }
            }
        }

    }

    fun judge(device: BluetoothDevice?): Boolean {
        if (device == null || deviceList.contains(device)) {
            return true
        }
        // TODO: 可以加判断逻辑
        return false
    }

    fun clear() {
        mList.clear()
        deviceList.clear()
    }
}
