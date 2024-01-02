package tk.manucloud.manue_kotlin

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import com.felhr.usbserial.UsbSerialInterface.UsbReadCallback
import io.github.controlwear.virtual.joystick.android.JoystickView
import tk.manucloud.manue_kotlin.databinding.ActivityMainBinding
import java.security.KeyStore.TrustedCertificateEntry
import java.util.*


class MainActivity : AppCompatActivity() {

// ------------------------------VAL-------------------------------------- //
    lateinit var m_usbManager : UsbManager
    var m_device : UsbDevice? = null
    var m_serial : UsbSerialDevice? = null
    var m_connection : UsbDeviceConnection? = null

    val ACTION_USB_PERMISSION = "tk.manucloud.manue_kotlin.USB_PERMISSION"
    var on = true
    private lateinit var binding: ActivityMainBinding

// ------------------------------onCreate-------------------------------------- //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        m_usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)

        registerReceiver(broadcastReceiver, filter)


        binding.btnConnect.setOnClickListener {
            startUsbConnecting()
        }
        binding.btnDisconnect.setOnClickListener {
            disconnect()
        }
        // To check a switch
        binding.proModeSwitch.isChecked = false

// To listen for a switch's checked/unchecked state changes
        binding.proModeSwitch.setOnCheckedChangeListener { buttonView, on ->
            if(!on) {
                sendData("0,0,proOn")

            }else {
                sendData("0,0,proOff")

            }
        }


        m_serial?.read {Toast.makeText(this, it.toString(), Toast.LENGTH_SHORT)}
// ------------------------------JOYSTICK--------------------------------- //
        val joystick = findViewById<JoystickView>(R.id.joystickView)
        joystick.setOnMoveListener { angle, strength ->
            sendData("$angle,$strength,0")

        }
// -----------------------------END JOYSTICK------------------------------ //

    }








    private val mCallback = UsbReadCallback {

    }
// -----------------------------USB Serial Connection------------------------------ //

    private fun startUsbConnecting() {
        val usbDevices: HashMap<String, UsbDevice>? = m_usbManager.deviceList
        if (!usbDevices?.isEmpty()!!) {
            var keep = true
            usbDevices.forEach{ entry ->
                m_device = entry.value
                val deviceVendorId: Int? = m_device?.vendorId
                Toast.makeText(this, "vendorId: "+deviceVendorId, Toast.LENGTH_SHORT)
                if (deviceVendorId == 6790) {
                    val intent: PendingIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), FLAG_MUTABLE)
                    m_usbManager.requestPermission(m_device, intent)
                    keep = false
                    Log.i("serial", "connection successful")
                } else {
                    m_connection = null
                    m_device = null
                    Log.i("serial", "unable to connect")
                }
                if (!keep) {
                    return
                }
            }
        } else {
            Log.i("serial", "no usb device connected")
        }
    }

    private fun sendData(input: String) {

        try {
            val msg: String
            val data: ByteArray
            if (false) {
                val sb = StringBuilder()
                TextUtil.toHexString(sb, TextUtil.fromHexString(input))
                msg = sb.toString()
                data = TextUtil.fromHexString(msg)
            } else {
                msg = input
                data = (input).toByteArray()
            }
            val spn = SpannableStringBuilder(
                """
              $msg
              
              """.trimIndent()
            )
            spn.setSpan(
                ForegroundColorSpan(resources.getColor(R.color.colorSendText)),
                0,
                spn.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            m_serial?.write(data)
            Log.i("serial", data.contentToString())
            Toast.makeText(this, data.contentToString(), Toast.LENGTH_SHORT)
        }catch (e: Exception) {
            Log.i("error", e.toString())
        }

    }

    private fun disconnect() {
        m_serial?.close()
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action!! == ACTION_USB_PERMISSION) {
                Toast.makeText(context, "usb permission", Toast.LENGTH_SHORT)
                val granted: Boolean = intent.extras!!.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)
                if (granted) {
                    Toast.makeText(context, "granted", Toast.LENGTH_SHORT)
                    m_connection = m_usbManager.openDevice(m_device)
                    m_serial = UsbSerialDevice.createUsbSerialDevice(m_device, m_connection)
                    if (m_serial != null) {
                        if (m_serial!!.open()) {
                            m_serial!!.setBaudRate(115200)
                            m_serial!!.setDataBits(UsbSerialInterface.DATA_BITS_8)
                            m_serial!!.setStopBits(UsbSerialInterface.STOP_BITS_1)
                            m_serial!!.setParity(UsbSerialInterface.PARITY_NONE)
                            m_serial!!.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
                            Toast.makeText(context, getString(R.string.conected), Toast.LENGTH_SHORT)
                        } else {
                            Log.i("Serial", "port not open")
                            Toast.makeText(context, "port not open", Toast.LENGTH_SHORT)
                        }
                    } else {
                        Log.i("Serial", "port is null")
                        Toast.makeText(context, "port is null", Toast.LENGTH_SHORT)
                    }
                } else {
                    Log.i("Serial","permission not granted")
                    Toast.makeText(context, "permission not granted", Toast.LENGTH_SHORT)
                }
            } else if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                startUsbConnecting()
            } else if (intent.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                disconnect()
            }
        }

    }



}