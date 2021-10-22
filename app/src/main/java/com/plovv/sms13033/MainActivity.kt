package com.plovv.sms13033

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.SmsManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.plovv.sms13033.databinding.ActivityMainBinding
import android.content.IntentFilter
import android.content.Intent
import android.content.BroadcastReceiver
import android.app.PendingIntent


class MainActivity : AppCompatActivity() {

    companion object {
        private const val SMS_PERMISSIONS_REQUEST = 1
        private const val SENT = "SMS13033_SMS_SENT"
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        initDataModel()
        initInputViews()
        initEditButtons()
        initSendButton()
    }

    override fun onPostResume() {
        super.onPostResume()

        // check for sms permission and request if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), SMS_PERMISSIONS_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            SMS_PERMISSIONS_REQUEST -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, continue
                } else {
                    // permission denied
                    this.finish()
                }

                return
            }
        }

    }

    private fun initDataModel() {
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE)

        val userName = sharedPref?.getString(getString(R.string.user_name_stored_key), "") ?: ""
        val userAddress = sharedPref?.getString(getString(R.string.user_address_stored_key), "") ?: ""

        val data = DataModel(userName, userAddress)

        binding.dataModel = data
    }

    private fun initInputViews() {
        binding.userNameInput.setOnEditorActionListener { input, actionId, _ ->
            if(actionId == EditorInfo.IME_ACTION_DONE){
                val name = input.text.toString().trim()

                saveName(name)
                input.isEnabled = false

                true
            }

            false
        }

        // set programmatically to enable line wrap + action done on enter
        binding.userAddressInput.maxLines = 2
        binding.userAddressInput.setHorizontallyScrolling(false)

        binding.userAddressInput.setOnEditorActionListener { input, actionId, _ ->
            if(actionId == EditorInfo.IME_ACTION_DONE){
                val address = input.text.toString().trim()

                saveAddress(address)
                input.isEnabled = false

                true
            }

            false
        }

        // set the inputs to enabled/disabled on start
        binding.userNameInput.isEnabled = binding.dataModel?.fullNameEnabled?: false
        binding.userAddressInput.isEnabled = binding.dataModel?.addressEnabled?: false
    }

    private fun initEditButtons() {
        binding.userNameEditBtn.setOnClickListener{
            binding.userNameInput.isEnabled = true
            binding.userNameInput.requestFocus()
            showKeyboard()
        }

        binding.userAddressEditBtn.setOnClickListener {
            binding.userAddressInput.isEnabled = true
            binding.userAddressInput.requestFocus()
            showKeyboard()
        }
    }

    private fun initSendButton() {
        binding.sendSmsBtn.setOnClickListener {
            onSend()
        }
    }

    private fun onSend() {
        val name = binding.dataModel?.fullName?: ""
        val address = binding.dataModel?.address?: ""

        when {
            (name+address).equals("", false) -> {
                Toast.makeText(applicationContext, R.string.message_no_info, Toast.LENGTH_SHORT).show()
            }
            name.equals("", false) -> {
                Toast.makeText(applicationContext, R.string.message_no_name, Toast.LENGTH_SHORT).show()
            }
            address.equals("", false) -> {
                Toast.makeText(applicationContext, R.string.message_no_address, Toast.LENGTH_SHORT).show()
            }
            else -> {
                sendSMS(
                    when (binding.choicesGroup.checkedRadioButtonId){
                        R.id.choice_1 -> 1
                        R.id.choice_2 -> 2
                        R.id.choice_3 -> 3
                        R.id.choice_4 -> 4
                        R.id.choice_5 -> 5
                        R.id.choice_6 -> 6
                        else -> -1
                    },
                    name,
                    address
                )
            }
        }
    }

    private fun sendSMS(choice: Int, name: String, address: String) {
        val number = getString(R.string.receivers_number)
        val message = "$choice $name $address"

        val smsManager = SmsManager.getDefault()
        val sentIntent: PendingIntent = PendingIntent.getBroadcast(this, 0, Intent(SENT), 0)

        registerSmsBroadcastReceiver()
        smsManager.sendTextMessage(number, null, message, sentIntent, null)
    }

    private fun registerSmsBroadcastReceiver() {
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(arg0: Context, arg1: Intent) {
                when (resultCode) {
                    RESULT_OK -> Toast.makeText(baseContext, R.string.sms_sent_msg, Toast.LENGTH_SHORT).show()
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE ->
                        Toast.makeText(baseContext, R.string.sms_generic_fail, Toast.LENGTH_SHORT).show()
                    SmsManager.RESULT_ERROR_NO_SERVICE ->
                        Toast.makeText(baseContext, R.string.sms_no_srv, Toast.LENGTH_SHORT).show()
                    SmsManager.RESULT_ERROR_NULL_PDU ->
                        Toast.makeText(baseContext, R.string.sms_null_pdu, Toast.LENGTH_SHORT).show()
                    SmsManager.RESULT_ERROR_RADIO_OFF ->
                        Toast.makeText(baseContext, R.string.sms_radio_off, Toast.LENGTH_SHORT).show()
                    else -> { }
                }
            }
        }, IntentFilter(SENT))
    }

    private fun saveName(name : String) {
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return

        with (sharedPref.edit()) {
            putString(getString(R.string.user_name_stored_key), name)
            binding.dataModel?.fullName = name
            apply()
        }
    }

    private fun saveAddress(address : String) {
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return

        with (sharedPref.edit()) {
            putString(getString(R.string.user_address_stored_key), address)
            binding.dataModel?.address = address
            apply()
        }
    }

    private fun showKeyboard() {
        val inputMethodManager: InputMethodManager = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

}