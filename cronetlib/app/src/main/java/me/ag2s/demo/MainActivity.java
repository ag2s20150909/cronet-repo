package me.ag2s.demo;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.ag2s.cronetlib.CronetClient;

public class MainActivity extends AppCompatActivity {
    static ExecutorService executor = Executors.newCachedThreadPool();
    String url="https://http3.is/";

    Button btn;
    TextView tv;
    TextView tv1;
    boolean useCronet = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn = findViewById(R.id.btn_send);
        tv = findViewById(R.id.tv);
        tv1 = findViewById(R.id.tvTimeOut);

        @SuppressLint("UseSwitchCompatOrMaterialCode")
        Switch aSwitch = findViewById(R.id.aswitch);
        aSwitch.setChecked(true);

        aSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            useCronet = isChecked;
        });
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (this) {
                            String response = "";
                            mHandler.obtainMessage(0, response).sendToTarget();
                            long st = SystemClock.elapsedRealtime();
                            CronetClient.getInstance().setUseCronet(useCronet);
                            int count=1;

                            for (int i = 0; i < count; i++) {
                                response = HttpTool.httpGet("https://cloudflare-quic.com/");
                            }
                            int all= (int) (SystemClock.elapsedRealtime() - st);
                            int pre=all/count;
                            String string = "All:"+all+"ms Pre:"+pre+"ms";
                            response = response.replaceAll("^(?s)(.*?)<div class=\"site-content  \">(.*?)</div>(.*?)", "$2");
                            mHandler.obtainMessage(1, string).sendToTarget();
                            mHandler.obtainMessage(0, response).sendToTarget();
                        }

                    }
                });

            }
        });

    }

private final Handler mHandler=new Handler(Looper.getMainLooper()){

    @Override
    public void handleMessage(@NonNull Message msg) {
        switch (msg.what){
            case 0:
                tv.setText(Html.fromHtml((String) msg.obj));
                return;
            case 1:
                tv1.setText((String) msg.obj);
                return;
        }
        super.handleMessage(msg);
    }

};




}