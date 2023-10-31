package com.example.blueteethtest

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.BOND_BONDED
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.greenrobot.eventbus.EventBus

class FindBluetoothActivity : AppCompatActivity(),
    BluetoothDeviceAdapter.OnDeviceItemClickListener {
    var adapter: BluetoothDeviceAdapter? = null
    val btAdapter by lazy {
        val btManager =
            this@FindBluetoothActivity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        btManager.adapter
    }
    var mManager: MyBluetoothManager? = null
    lateinit var rvDevice:RecyclerView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_bluetooth)
        rvDevice = findViewById<RecyclerView>(R.id.rv_device)
        rvDevice.layoutManager =
            object : LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false) {}
        mManager = MyBluetoothManager(this@FindBluetoothActivity)
        adapter = mManager?.deviceAdapter
        adapter?.onItemClickListener = this
        mManager?.setDiscoverable()
        rvDevice.adapter = adapter
        initObserver()
        mManager?.startSearch(this@FindBluetoothActivity)
    }


    fun initObserver() {
        mManager?.mSearchLiveData?.observe(
            this
        ) { value ->
            when (value.loadingStatus) {
                Resource.NEXT -> {
                    adapter?.notifyItemInserted(value.data)
                }
                Resource.SUCCESS -> toast("扫描完成")
            }
        }
        mManager?.mBondLiveData?.observe(this) { value ->
            when (value.loadingStatus) {
                Resource.LOADING -> toast("配对中")
                Resource.SUCCESS -> toast("配对成功")
                Resource.ERROR -> toast("配对失败")
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onDeviceItemClick(address: String) {
        val remoteDevice = btAdapter.getRemoteDevice(address)
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
        mManager?.makePair(address)
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
        unregisterReceiver(mManager?.mReceiver)
    }
}