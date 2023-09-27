package com.example.blueteethtest

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Thread.sleep
import java.util.UUID

private const val TAG = "MY_APP_DEBUG_TAG"

const val STATE_NONE = 0;
const val STATE_LISTEN = 1;
const val STATE_CONNECTING = 2;
const val STATE_CONNECTED = 3;
// ... (Add other message types here as needed.)

class MyBluetoothService(
    // handler that gets info from Bluetooth service
    private val handler: Handler
) {
    val MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
    var bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    var mState = STATE_NONE
    var mNewState = STATE_NONE
    var acceptThread: AcceptThread? = null
    var connectThread: ConnectThread? = null
    var connectedThread: ConnectedThread? = null

    @Synchronized
    fun getState(): Int {
        return mState
    }

    @Synchronized
    private fun updateUserInterfaceTitle() {
        mState = getState()
        Log.d(TAG, "updateUserInterfaceTitle() $mNewState -> $mState")
        mNewState = mState
    }

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
        sleep(100)
        updateUserInterfaceTitle()
    }

    @Synchronized
    fun connect(device: BluetoothDevice?) {
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
        sleep(100)
        updateUserInterfaceTitle()

    }

    private fun manageMyConnectedSocket(socket: BluetoothSocket?) {
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

        sleep(100)
        updateUserInterfaceTitle()
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

    private fun connectionFailed() {
        mState = BluetoothChatService.STATE_NONE
        // Update UI title
        updateUserInterfaceTitle()

        // Start the service over to restart listening mode
        start()
    }

    private fun connectionLost() {
        updateUserInterfaceTitle()
        start()
    }

    @SuppressLint("MissingPermission")
    inner class AcceptThread : Thread() {

        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter?.listenUsingRfcommWithServiceRecord("BluetoothChatSecure", MY_UUID)
        }

        init {
            mState = STATE_LISTEN
        }

        override fun run() {
            // Keep listening until exception occurs or a socket is returned.
            var shouldLoop = true
            Log.d(
                TAG, "Socket Type: " + "BEGIN mAcceptThread" + this
            )
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                }
                socket?.also {
                    manageMyConnectedSocket(it)
                    mmServerSocket?.close()
                    shouldLoop = false
                }
            }
            Log.i(TAG, "END mAcceptThread")
        }

        // Closes the connect socket and causes the thread to finish.
        fun cancel() {
            Log.e(TAG, "cancel" + this)
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }

    }

    @SuppressLint("MissingPermission")
    inner class ConnectThread(device: BluetoothDevice?) : Thread() {

        private val mmSocket = device?.createRfcommSocketToServiceRecord(MY_UUID)

        //            device.createInsecureRfcommSocketToServiceRecord(MyApplication.MY_UUID)
        init {
            mState = STATE_CONNECTING;
        }

        public override fun run() {
            Log.i(TAG, "BEGIN mConnectThread")
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter?.cancelDiscovery()
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
            manageMyConnectedSocket(mmSocket)
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
                val readMsg = handler.obtainMessage(
                    Constants.MESSAGE_READ, numBytes!!, -1, mmBuffer
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
                // Send a failure message back to the activity.
//                val writeErrorMsg = handler.obtainMessage(MESSAGE_TOAST)
//                val bundle = Bundle().apply {
//                    putString("toast", "Couldn't send data to the other device")
//                }
//                writeErrorMsg.data = bundle
//                handler.sendMessage(writeErrorMsg)
                return
            }

            // Share the sent message with the UI activity.
//            val writtenMsg = handler.obtainMessage(
//                MESSAGE_WRITE, -1, -1, mmBuffer
//            )
//            writtenMsg.sendToTarget()
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

}