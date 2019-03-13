package ozlemdogan.translatesmsapp

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
    }
    override fun onResume() {
        super.onResume()

        object: CountDownTimer(4000,1000){
            override fun onFinish() {
                intent= Intent(this@SplashActivity,HomeActivity::class.java)
                startActivity(intent)
                //finish()
            }

            override fun onTick(millisUntilFinished: Long) {

            }

        }.start()

    }
}
