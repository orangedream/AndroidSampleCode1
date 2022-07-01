package tw.iotec.androidsamplecode1

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.AlarmClock
import android.widget.Button
import android.widget.TextView

class SecondActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        val msg = intent.getStringExtra(AlarmClock.EXTRA_MESSAGE) ?: "No message"
        val tvMsg = findViewById<TextView>(R.id.tv_msg)
        tvMsg.text = msg

        findViewById<Button>(R.id.btn_main_activity).setOnClickListener {
            finish()
        }


        findViewById<Button>(R.id.btn_main_activity_v2).setOnClickListener {
            finish()
            overridePendingTransition(
                // 指定轉換的動畫
                R.anim.slide_in_left,   // 新 Activity 進入動畫
                R.anim.slide_out_right	 // 原 Activity 離開的動畫
            )

        }

        findViewById<Button>(R.id.btn_main_activity_v3).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            overridePendingTransition(
                // 指定轉換的動畫
                R.anim.slide_in_left,   // 新 Activity 進入動畫
                R.anim.slide_out_right	 // 原 Activity 離開的動畫
            )

        }

    }
}