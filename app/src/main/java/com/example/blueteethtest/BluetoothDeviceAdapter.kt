package com.example.blueteethtest

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class BluetoothDeviceAdapter(
    private val context: Context
) : RecyclerView.Adapter<BluetoothDeviceAdapter.DeviceViewHolder>() {

    var deviceList = ArrayList<BluetoothDeviceInfo>()
    var onItemClickListener: OnDeviceItemClickListener? = null

    override fun getItemCount(): Int {
        return deviceList.size
    }

    fun refreshData(accountList: ArrayList<BluetoothDeviceInfo>) {
        this.deviceList.clear()
        this.deviceList.addAll(accountList)
        notifyDataSetChanged()
    }

    class DeviceViewHolder : RecyclerView.ViewHolder {
        var root: View? = null
        var name: TextView? = null


        constructor(view: View) : super(view) {
            root = view
            name = view.findViewById(R.id.tv_name)
        }
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        var device = deviceList[position]
        holder.name?.text =
            if (device.name == null) "未知设备 ${device.address}" else "${device.name}   ${device.address}"
        holder?.root?.setOnClickListener {
            onItemClickListener?.onDeviceItemClick(device)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view =
            LayoutInflater.from(context).inflate(R.layout.item_device_info, parent, false)

        return DeviceViewHolder(view)
    }

    interface OnDeviceItemClickListener {
        fun onDeviceItemClick(device: BluetoothDeviceInfo)
    }
}