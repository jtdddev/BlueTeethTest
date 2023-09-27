package com.example.blueteethtest

import android.bluetooth.BluetoothDevice

interface MakePairBluetoothListener {
    fun whilePair(device: BluetoothDevice)
    fun pairingSuccess(device: BluetoothDevice)
    fun cancelPair(device: BluetoothDevice)
}