package com.example.blueteethtest

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import android.widget.Toast

/**
 * @author: J-T
 * @date: 2023/9/27 11:08
 * @description：同于管理蓝牙设备搜索发现 配对
 */
@SuppressLint("MissingPermission")
class MyBluetoothManager(val context: Context) {
    val deviceList = ArrayList<BluetoothDevice>() //蓝牙设备列表

    //    val mList = ArrayList<BluetoothDeviceInfo>()  //自己的列表 可用于展示
    val mSearchLiveData: ValueKeeperLiveData<Resource<Int>> = ValueKeeperLiveData()
    val mBondLiveData: ValueKeeperLiveData<Resource<BluetoothDevice>> = ValueKeeperLiveData()
    val btAdapter =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter // getDefaultAdapter已经被废弃
    var isSearching = false  //是否正在搜索
    var deviceAdapter = BluetoothDeviceAdapter(context)

    init {
        deviceAdapter.deviceList = deviceList
    }

    val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(p0: Context?, p1: Intent) {
            when (p1.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    isSearching = true
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= 33) {
                        p1.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java
                        )
                    } else {
                        p1.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    if (judge(device)) {
                        return
                    }
                    deviceList.add(device!!)
                    mSearchLiveData.postValue(Resource(Resource.NEXT, deviceList.size - 1, ""))
                    Log.d(
                        "MyBluetoothManager",
                        "device name ${device.name} device address ${device.address}"
                    )
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    isSearching = false
                    //搜索完成
                    mSearchLiveData.postValue(Resource(Resource.SUCCESS, null, ""))
                }

                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= 33) {
                        p1.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java
                        )
                    } else {
                        p1.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val state: Int = when (device?.bondState) {
                        BluetoothDevice.BOND_BONDING -> Resource.LOADING
                        BluetoothDevice.BOND_BONDED -> Resource.SUCCESS
                        BluetoothDevice.BOND_NONE -> Resource.ERROR
                        else -> Resource.ERROR
                    }
                    mBondLiveData.postValue(Resource(state, device, ""))
                }
            }
        }

    }

    fun judge(device: BluetoothDevice?): Boolean {
        // TODO: 此处可以对搜索到的BluetoothDevice进行判断是否添加到列表中
        if (device == null || deviceList.contains(device)) {
            return true
        }
        return false
    }


    @SuppressLint("MissingPermission")
    fun startSearch(context: Context): Unit {
        clear()
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        context.registerReceiver(mReceiver, filter)
        btAdapter.startDiscovery()
        Toast.makeText(
            context, "正在搜索蓝牙设备", Toast.LENGTH_SHORT
        ).show()
    }

    @SuppressLint("MissingPermission")
    fun makePair(address: String?) {
        if (isSearching) {
            btAdapter.cancelDiscovery()
            isSearching = false
        }
        btAdapter.getRemoteDevice(address).apply {
            createBond()
        }
    }

    fun getBondedDevices(): Set<BluetoothDevice> {
        return btAdapter.bondedDevices
    }


    fun setDiscoverable() {
        val discoverableIntent: Intent =
            Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            }
        context.startActivity(discoverableIntent)
    }

    private fun clear() {
        deviceList.clear()
        deviceAdapter.notifyDataSetChanged()
    }
}