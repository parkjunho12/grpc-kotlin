package io.grpc.examples.helloworld.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat

object PhoneUtil {

    private val NUM_REGEX = "[^0-9]".toRegex()

    fun getSimPNumber(context: Context): String {
        var myPNumber = ""
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        when (telephonyManager.simState) {
            TelephonyManager.SIM_STATE_ABSENT -> {
                return myPNumber
            }
            else -> {
                try {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_SMS
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_PHONE_NUMBERS
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_PHONE_STATE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return myPNumber
                    }
                    myPNumber = telephonyManager.line1Number
                    return if (myPNumber == null) {
                        myPNumber = ""
                        myPNumber
                    } else {
                        myPNumber = myPNumber.replace("+82", "0")
                        if (myPNumber.length != 11) {
                            return myPNumber
                        } else {
                            myPNumber = myPNumber.substring(0, 3) + "-" + myPNumber.substring(3, 7) + "-" + myPNumber.substring(7, myPNumber.lastIndex + 1)
                        }
                        myPNumber
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    return myPNumber
                }
            }
        }
    }
}