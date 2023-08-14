package com.gson

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.google.gson.Gson
import java.math.BigDecimal

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "IzumiSakai"
    }

    private val objectToString: TextView by lazy { findViewById(R.id.object_to_string) }
    private val stringToObject: TextView by lazy { findViewById(R.id.string_to_object) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        objectToString.setOnClickListener {
            fromObjectToString()
        }

        stringToObject.setOnClickListener {
            fromStringToObject()
        }
    }

    private fun fromObjectToString() {
        val gson = Gson()
        val staff = Staff(
            name = "mk_yong",
            age = 35,
            position = arrayOf("Founder", "CTO", "Writer"),
            salary = mapOf("2010" to BigDecimal(10000), "2012" to BigDecimal(12000), "2018" to BigDecimal(14000)),
            skills = listOf("java", "python", "node", "kotlin")
        )
        val str = gson.toJson(staff)
        Log.d(TAG, str)
    }

    private fun fromStringToObject() {
        val str = "{\"name\":\"mkyong\",\"age\":35,\"position\":[\"Founder\",\"CTO\",\"Writer\"],\"skills\":[\"java\",\"python\",\"node\",\"kotlin\"],\"salary\":{\"2018\":14000,\"2012\":12000,\"2010\":10000}}"
        val staff = Gson().fromJson(str, Staff::class.java)
        Log.d(TAG, staff.toString())
    }
}