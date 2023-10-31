package com.example.blueteethtest

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import com.example.blueteethtest.Constants.MESSAGE_READ
import com.example.blueteethtest.Constants.MESSAGE_STATE_CHANGE
import com.example.blueteethtest.Constants.MESSAGE_TOAST
import com.example.blueteethtest.Constants.MESSAGE_WRITE
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * @author: J-T
 * @date: 2023/9/27 13:54
 * @description：用于蓝牙连接及通信
 */

const val STATE_NONE = 0;
const val STATE_LISTEN = 1;
const val STATE_CONNECTING = 2;
const val STATE_CONNECTED = 3;

class MyBluetoothService(val context: Context, private val mHandler: Handler) {
    private val TAG = "MyBluetoothService"
    val MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
    val btAdapter =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter // getDefaultAdapter已经被废弃
    var mState = STATE_NONE
    var mNewState = STATE_NONE
    var acceptThread: AcceptThread? = null
    var connectThread: ConnectThread? = null
    var connectedThread: ConnectedThread? = null

    @Synchronized
    fun start() {
        Log.d(TAG, "start")
        if (connectThread != null) {
            connectThread?.cancel()
            connectThread = null
        }

        if (connectedThread != null) {
            connectedThread?.cancel()
            connectedThread = null
        }

        if (acceptThread == null) {
            acceptThread = AcceptThread()
            acceptThread?.start()
        }
        updateUserInterfaceTitle()
    }

    @SuppressLint("MissingPermission")
    @Synchronized
    fun connect(device: BluetoothDevice) {
        Log.d(TAG, "connect to: $device")

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (connectThread != null) {
                connectThread?.cancel()
                connectThread = null
            }
        }
        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread?.cancel()
            connectedThread = null
        }

        // Start the thread to connect with the given device
        connectThread = ConnectThread(device)
        connectThread?.start()


        // Send the name of the connected device back to the UI Activity
        val msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME)
        val bundle = Bundle()
        bundle.putString(Constants.DEVICE_NAME, device.name)
        msg.data = bundle
        mHandler.sendMessage(msg)

        updateUserInterfaceTitle()

    }

    @SuppressLint("MissingPermission")
    private fun manageMyConnectedSocket(socket: BluetoothSocket?, device:BluetoothDevice) {
        Log.d(TAG, "manageMyConnectedSocket")
        if (connectThread != null) {
            connectThread?.cancel()
            connectThread = null
        }

        if (connectedThread != null) {
            connectedThread?.cancel()
            connectedThread = null
        }
        if (acceptThread != null) {
            acceptThread?.cancel()
            acceptThread = null
        }
        connectedThread = ConnectedThread(socket)
        connectedThread?.start()


        // Send the name of the connected device back to the UI Activity
        val msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME)
        val bundle = Bundle()
        bundle.putString(Constants.DEVICE_NAME, device.getName())
        msg.data = bundle
        mHandler.sendMessage(msg)
        updateUserInterfaceTitle()
    }


    @Synchronized
    private fun updateUserInterfaceTitle() {
        mState = getState()
        Log.d(TAG, "updateUserInterfaceTitle() $mNewState -> $mState")
        mNewState = mState
        mHandler.obtainMessage(MESSAGE_STATE_CHANGE, mNewState, -1).sendToTarget()
    }

    @Synchronized
    fun getState(): Int {
        return mState
    }

    fun write(out: ByteArray?) {
        // Create temporary object
        var r: ConnectedThread?
        // Synchronize a copy of the ConnectedThread
        synchronized(this) {
            if (mState != STATE_CONNECTED) return
            r = connectedThread
        }
        // Perform the write unsynchronized
        r?.write(out!!)
    }

    fun write(s: String?) {
        if (TextUtils.isEmpty(s)) {
            return
        }
        // Create temporary object
        var r: ConnectedThread?
        // Synchronize a copy of the ConnectedThread
        synchronized(this) {
            if (mState != STATE_CONNECTED) return
            r = connectedThread
        }
        // Perform the write unsynchronized
        r?.write(s!!.toByteArray())
    }


    @SuppressLint("MissingPermission")
    inner class AcceptThread : Thread() {

        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            btAdapter?.listenUsingRfcommWithServiceRecord("Bluetooth", MY_UUID)
        }

        init {
            mState = STATE_LISTEN
        }

        override fun run() {
            // Keep listening until exception occurs or a socket is returned.
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                }
                socket?.also {
                    manageMyConnectedSocket(it,it.remoteDevice)
                    mmServerSocket?.close()
                    shouldLoop = false
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }

    }

    @SuppressLint("MissingPermission")
    inner class ConnectThread(val device: BluetoothDevice) : Thread() {

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(MY_UUID)
        }

        init {
            mState = STATE_CONNECTING;
        }

        public override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            btAdapter?.cancelDiscovery()
            try {
                mmSocket?.connect()
            } catch (e: IOException) {
                try {
                    mmSocket?.close()
                } catch (e2: IOException) {
                    Log.e(
                        TAG, "unable to close() " + " socket during connection failure", e2
                    )
                }
                connectionFailed()
                return
            }
            synchronized(this@MyBluetoothService) {
                connectThread = null
            }
            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            manageMyConnectedSocket(mmSocket, device)
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e("MyBluetoothManager", "Could not close the client socket", e)
            }
        }

    }

    inner class ConnectedThread(private val mmSocket: BluetoothSocket?) : Thread() {

        private val mmInStream: InputStream? = mmSocket?.inputStream
        private val mmOutStream: OutputStream? = mmSocket?.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

        init {
            mState = STATE_CONNECTED
        }

        override fun run() {
            var numBytes: Int? // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                // Read from the InputStream.
                numBytes = try {
                    mmInStream?.read(mmBuffer)
                } catch (e: IOException) {
                    Log.d(TAG, "Input stream was disconnected", e)
                    break
                }

                // Send the obtained bytes to the UI activity.
                val readMsg = mHandler.obtainMessage(
                    MESSAGE_READ, numBytes!!, -1, mmBuffer
                )
                readMsg.sendToTarget()
            }
        }

        // Call this from the main activity to send data to the remote device.
        fun write(bytes: ByteArray) {
            try {
                mmOutStream?.write(bytes)
            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when sending data", e)
                val writeErrorMsg = mHandler.obtainMessage(MESSAGE_TOAST)
                val bundle = Bundle().apply {
                    putString("toast", "Couldn't send data to the other device")
                }
                writeErrorMsg.data = bundle
                mHandler.sendMessage(writeErrorMsg)
                return

            }
            //Share the sent message with the UI activity .
            val writtenMsg = mHandler.obtainMessage(
                MESSAGE_WRITE, -1, -1, bytes
            )
            writtenMsg.sendToTarget()
        }

        // Call this method from the main activity to shut down the connection.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }


    private fun connectionFailed() {

        // Send a failure message back to the Activity
        val msg = mHandler.obtainMessage(MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(Constants.TOAST, "Unable to connect device")
        msg.data = bundle
        mHandler.sendMessage(msg)
        mState = BluetoothChatService.STATE_NONE
        // Update UI title
        updateUserInterfaceTitle()

        // Start the service over to restart listening mode
        start()
    }

    private fun connectionLost() {

        // Send a failure message back to the Activity
        val msg = mHandler.obtainMessage(MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(Constants.TOAST, "Device connection was lost")
        msg.data = bundle
        mHandler.sendMessage(msg)
        updateUserInterfaceTitle()
        start()
    }
}