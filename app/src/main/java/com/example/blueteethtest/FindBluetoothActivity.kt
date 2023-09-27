package com.example.blueteethtest

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.BOND_BONDED
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.greenrobot.eventbus.EventBus

class FindBluetoothActivity : AppCompatActivity(),
    BluetoothDeviceAdapter.OnDeviceItemClickListener {
    lateinit var adapter: BluetoothDeviceAdapter
    val btAdapter by lazy {
        val btManager =
            this@FindBluetoothActivity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        btManager.adapter
    }
    val manager: MyBluetoothManager = MyBluetoothManager()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_bluetooth)
        setDiscoverable()
        adapter = manager.bluetoothDeviceAdapter
        var rvDevice = findViewById<RecyclerView>(R.id.rv_device)
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
        manager.setAdapter(btAdapter)
        manager.startSearch(this@FindBluetoothActivity)
    }

    fun setDiscoverable() {
        val discoverableIntent: Intent =
            Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            }
        startActivity(discoverableIntent)
    }

    @SuppressLint("MissingPermission")
    override fun onDeviceItemClick(device: BluetoothDeviceInfo) {
        val remoteDevice = btAdapter.getRemoteDevice(device.address)
        if (remoteDevice.bondState == BOND_BONDED) {
            Log.d(
                "MyBluetoothManager",
                "${remoteDevice.name} 已经配对了"
            )
            val intent = Intent()
            EventBus.getDefault().post(object : DeviceEvent(remoteDevice) {})
            setResult(RESULT_OK, intent)
            finish()
            return
        }
        manager.makePair(device.address, bondListener)
    }


    val bondListener: MakePairBluetoothListener = object : MakePairBluetoothListener {
        override fun whilePair(device: BluetoothDevice) {
            Log.d(
                "MyBluetoothManager",
                "正在绑定"
            )

        }

        override fun pairingSuccess(device: BluetoothDevice) {
            Log.d(
                "MyBluetoothManager",
                "绑定成功"
            )
            val intent = Intent()
            EventBus.getDefault().post(object : DeviceEvent(device) {})
            setResult(RESULT_OK, intent)
            finish()
        }

        override fun cancelPair(device: BluetoothDevice) {
            Log.d(
                "MyBluetoothManager",
                "绑定失败"
            )
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(manager.mReceiver)
    }
}