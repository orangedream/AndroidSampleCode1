package tw.iotec.androidsamplecode1

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

    }
}