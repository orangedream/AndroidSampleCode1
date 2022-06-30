package tw.iotec.androidsamplecode1

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.AlarmClock
import android.widget.Button


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.btn_next_activity).setOnClickListener {
            val intent = Intent(this, SecondActivity::class.java).apply {
                putExtra(AlarmClock.EXTRA_MESSAGE, "Hello, second.") // pass value to the activity
            }
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_next_activity_v2).setOnClickListener {
            startActivity(
                Intent(
                    baseContext,
                    SecondActivity::class.java
                )
            )
            overridePendingTransition(
                // 指定轉換的動畫
                R.anim.slide_in_right,   // 新 Activity 進入動畫
                R.anim.slide_out_left	 // 原 Activity 離開的動畫
            )
        }

        findViewById<Button>(R.id.btn_next_activity).setOnClickListener {
            val intent = Intent(this, SecondActivity::class.java).apply {
                putExtra(AlarmClock.EXTRA_MESSAGE, "Hello, second.") // pass value to the activity
            }
            startActivity(intent)
        }

    }
}