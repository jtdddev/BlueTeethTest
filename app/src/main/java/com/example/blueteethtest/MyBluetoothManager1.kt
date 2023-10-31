package com.example.blueteethtest

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log

class MyBluetoothManager1() {
//    val deviceList = ArrayList<BluetoothDevice?>()
//    val mList = ArrayList<BluetoothDeviceInfo>()
//    val bluetoothDeviceAdapter = BluetoothDeviceAdapter(MyApplication.instance.applicationContext)
//    val mBluetoothDeviceSearchLiveData: ValueKeeperLiveData<Resource<String>> =
//        ValueKeeperLiveData()
//    val mConnectDeviceLiveData: ValueKeeperLiveData<Resource<BluetoothDeviceInfo>> =
//        ValueKeeperLiveData()
//    lateinit var mBondStateListener: MakePairBluetoothListener
//    var isSearching: Boolean = false
//    lateinit var btAdapter: BluetoothAdapter
//
////    init {
//////        bluetoothDeviceAdapter.deviceList = mList
////    }
//
//    fun setAdapter(a: BluetoothAdapter) {
//        btAdapter = a
//    }
//
//    val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
//        @SuppressLint("MissingPermission")
//        override fun onReceive(p0: Context?, p1: Intent) {
//            val action: String? = p1.action
//            when (action) {
//                BluetoothDevice.ACTION_FOUND -> {
//                    // Discovery has found a device. Get the BluetoothDevice
//                    // object and its info from the Intent.
//                    isSearching = true
//                    var device: BluetoothDevice?
//                    if (Build.VERSION.SDK_INT >= 33) {
//                        device = p1.getParcelableExtra(
//                            BluetoothDevice.EXTRA_DEVICE,
//                            BluetoothDevice::class.java
//                        )
//                    } else {
//                        device = p1.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
//                    }
//                    if (judge(device)) {
//                        return
//                    }
//                    deviceList.add(device)
//                    val info = BluetoothDeviceInfo(device?.name, device?.address)
//                    Log.d(
//                        "MyBluetoothManager",
//                        "device name ${device?.name} device address ${device?.address}"
//                    )
//                    mList.add(info)
//                    mBluetoothDeviceSearchLiveData.postValue(Resource(Resource.LOADING, null, ""))
//                }
//
//                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
//                    isSearching = false
//                    //搜索完成
//                    mBluetoothDeviceSearchLiveData.postValue(Resource(Resource.SUCCESS, null, ""))
//                }
//
//                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
//                    var device: BluetoothDevice?
//                    if (Build.VERSION.SDK_INT >= 33) {
//                        device = p1.getParcelableExtra(
//                            BluetoothDevice.EXTRA_DEVICE,
//                            BluetoothDevice::class.java
//                        )
//                    } else {
//                        device = p1.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
//                    }
//                    when (device?.bondState) {
//                        BluetoothDevice.BOND_BONDING -> mBondStateListener.whilePair(device)
//                        BluetoothDevice.BOND_BONDED -> mBondStateListener.pairingSuccess(device)
//                        BluetoothDevice.BOND_NONE -> mBondStateListener.cancelPair(device)
//                    }
//                }
//            }
//        }
//
//    }
//
//    fun judge(device: BluetoothDevice?): Boolean {
//        if (device == null || deviceList.contains(device)) {
//            return true
//        }
//        return false
//    }
//
//    fun clear() {
//        mList.clear()
//        deviceList.clear()
//    }
//
//    @SuppressLint("MissingPermission")
//    fun startSearch(context: Context): Unit {
//        clear()
//        var filter: IntentFilter = IntentFilter()
//        filter.addAction(BluetoothDevice.ACTION_FOUND)
//        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
//        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
//        context.registerReceiver(mReceiver, filter)
//        btAdapter.startDiscovery()
//        toast("正在搜索中......")
//    }
//
//    @SuppressLint("MissingPermission")
//    fun makePair(address: String?, listener: MakePairBluetoothListener) {
//        if (isSearching) {
//            btAdapter.cancelDiscovery()
//            isSearching = false
//        }
//        mBondStateListener = listener
//        val remoteDevice = btAdapter.getRemoteDevice(address).apply {
//            createBond()
//        }
//    }
}
