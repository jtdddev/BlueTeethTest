package com.example.blueteethtest

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FindBluetoothActivity : AppCompatActivity(),
    BluetoothDeviceAdapter.OnDeviceItemClickListener {
    lateinit var adapter: BluetoothDeviceAdapter
    val manager: MyBluetoothManager = MyBluetoothManager()
    val btAdapter by lazy {
        val btManager =
            this@FindBluetoothActivity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        btManager.adapter
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_bluetooth)
        adapter = manager.bluetoothDeviceAdapter
        var rvDevice=findViewById<RecyclerView>(R.id.rv_device)
        rvDevice.adapter = manager.bluetoothDeviceAdapter
        manager.bluetoothDeviceAdapter.onItemClickListener = this
        rvDevice.layoutManager =
            object : LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false) {}
        manager.mBluetoothDeviceSearchLiveData.observe(this, object : Observer<Resource<String>> {
            override fun onChanged(value: Resource<String>) {
                when (value.loadingStatus) {
                    Resource.LOADING ->
                        adapter.notifyDataSetChanged()
                    Resource.SUCCESS -> toast("玛德扫描完了")
                }
            }

        })
        startSearch()
    }

    @SuppressLint("MissingPermission")
    fun startSearch(): Unit {
        manager.clear()
        var filter: IntentFilter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(manager.mReceiver, filter)
        btAdapter.startDiscovery()
        toast("正在搜索中......")
    }

    fun itemClick(info: BluetoothDeviceInfo) {

    }

    override fun onDeviceItemClick(device: BluetoothDeviceInfo) {

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(manager.mReceiver)
    }
}