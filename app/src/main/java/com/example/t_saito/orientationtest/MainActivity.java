package com.example.t_saito.orientationtest;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.Settings.System;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener {

    private static String TAG = MainActivity.class.getSimpleName();

    private Button btn1,
            btn2,
            btn3,
            btn4,
            btn5,
            btn6,
            btn7,
            btn8,
            btn9,
            btn10,
            btn11,
            btn12,
            btn13,
            btn14,
            btnPortrait,
            btnLandscape;

    private Button[] btns = { btn1,
            btn2,
            btn3,
            btn4,
            btn5,
            btn6,
            btn7,
            btn8,
            btn9,
            btn10,
            btn11,
            btn12,
            btn13,
            btn14
    };

    private TextView txtSensor, txtOrientation, txtSensorPolling;
    private SensorManager mSensorManager;

    /** センサーポーリング */
    private SensorPollingHandler mSensorPollingHandler = new SensorPollingHandler();

    private boolean mIsMagSensor;
    private boolean mIsAccSensor;
    private float mAngle;
    private int mOrientation = -1;

    private static final int MATRIX_SIZE = 16;
    /** 回転行列 */
    float[]  inR = new float[MATRIX_SIZE];
    float[] outR = new float[MATRIX_SIZE];
    float[]    I = new float[MATRIX_SIZE];
    /** センサーの値 */
    float[] orientationValues   = new float[3];
    float[] magneticValues      = new float[3];
    float[] accelerometerValues = new float[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // センサ・マネージャを取得する
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        int[] reses = {
                R.id.screen_orientation_unspecified,
                R.id.screen_orientation_landscape,
                R.id.screen_orientation_portrait,
                R.id.screen_orientation_user,
                R.id.screen_orientation_behind,
                R.id.screen_orientation_sensor,
                R.id.screen_orientation_nosensor,
                R.id.screen_orientation_sensor_landscape,
                R.id.een_orientation_sensor_portrait,
                R.id.screen_orientation_reverse_landscape,
                R.id.screen_orientation_reverse_portrait,
                R.id.screen_orientation_full_sensor,
                R.id.screen_orientation_user_landscape,
                R.id.screen_orientation_user_portrait
        };

        int cnt = 0;
        for (Button btn : btns) {
            btn = (Button) findViewById(reses[cnt]);
            btn.setOnClickListener(this);
            cnt++;
        }

        txtOrientation = (TextView) findViewById(R.id.user_setting_orientation);
        txtSensor = (TextView) findViewById(R.id.sensor);
        txtSensorPolling = (TextView) findViewById(R.id.sensor_polling);

        btnLandscape = (Button) findViewById(R.id.land_button);
        btnLandscape.setOnClickListener(this);
        btnPortrait = (Button) findViewById(R.id.port_button);
        btnPortrait.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mOrientation = -1;
        try {
            mOrientation = System.getInt(getContentResolver(), System.ACCELEROMETER_ROTATION);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        if (mOrientation == 1) {
            // ポーリングスタート
            mSensorPollingHandler.start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        int ori = -1;
        try {
             ori = System.getInt(getContentResolver(),System.ACCELEROMETER_ROTATION);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        txtOrientation.setText((ori == 0 ? "ロック" : (ori == 1 ? "アンロック" : "取得失敗")));

        // センサの取得
        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);

        // センサマネージャへリスナーを登録(implements SensorEventListenerにより、thisで登録する)
        for (Sensor sensor : sensors) {

            if( sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
                mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
                mIsMagSensor = true;
            }

            if( sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
                mIsAccSensor = true;
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) return;

        switch (event.sensor.getType()) {
            case Sensor.TYPE_MAGNETIC_FIELD:
                magneticValues = event.values.clone();
                break;
            case Sensor.TYPE_ACCELEROMETER:
                accelerometerValues = event.values.clone();
                break;
        }

        if (magneticValues != null && accelerometerValues != null) {

            SensorManager.getRotationMatrix(inR, I, accelerometerValues, magneticValues);

            //Activityの表示が縦固定の場合。横向きになる場合、修正が必要です
            SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Z, outR);
            SensorManager.getOrientation(outR, orientationValues);

            // 角度取得
            mAngle = roundOrientationDegree( radianToDegree(orientationValues[2]) );

            String z = String.valueOf( radianToDegree(orientationValues[0]) );
            String x = String.valueOf( radianToDegree(orientationValues[1]) );
            String y = String.valueOf( radianToDegree(orientationValues[2]) );
            Log.v("Orientation",
                            z + ", " + //Z軸方向,azimuth
                            x  + ", " + //X軸方向,pitch
                            y  );       //Y軸方向,roll
            txtSensor.setText("Z軸:"+ z + "\nX軸:"+ x +"\nY軸:"+ y +"\n角度:" + mAngle);

        }
    }

    int radianToDegree(float rad){
        return (int) Math.floor( Math.toDegrees(rad) ) ;
    }

    private float roundOrientationDegree(float roll){

        //inputのroll(Y軸のDegree)は-180～180の範囲を想定
        if(-225 < roll  && roll <= -135  )return  180.0f;
        if(-135 < roll  && roll <=  -45  )return   90.0f;
        if( -45 < roll  && roll <=   45  )return    0.0f;
        if(  45 < roll  && roll <=  135  )return  -90.0f;
        if( 135 < roll  && roll <=  225  )return -180.0f;

        return 0.0f;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOrientation == 1) {
            // ポーリングストップ
            mSensorPollingHandler.stop();
        }

        //センサーマネージャのリスナ登録破棄
        if (mIsMagSensor || mIsAccSensor) {
            mSensorManager.unregisterListener(this);
            mIsMagSensor = false;
            mIsAccSensor = false;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.screen_orientation_unspecified:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                break;
            case R.id.screen_orientation_landscape:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case R.id.screen_orientation_portrait:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case R.id.screen_orientation_user:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
                break;
            case R.id.screen_orientation_behind:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_BEHIND);
                break;
            case R.id.screen_orientation_sensor:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                break;
            case R.id.screen_orientation_nosensor:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
                break;
            case R.id.screen_orientation_sensor_landscape:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                break;
            case R.id.een_orientation_sensor_portrait:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                break;
            case R.id.screen_orientation_reverse_landscape:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                break;
            case R.id.screen_orientation_reverse_portrait:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                break;
            case R.id.screen_orientation_full_sensor:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                break;
            case R.id.screen_orientation_user_landscape:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
                break;
            case R.id.screen_orientation_user_portrait:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
                break;
            case R.id.land_button:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
                btnLandscape.setVisibility(View.GONE);
                btnPortrait.setVisibility(View.VISIBLE);
                break;
            case R.id.port_button:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
                btnLandscape.setVisibility(View.VISIBLE);
                btnPortrait.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Toast.makeText(this, "orientation:" + newConfig.orientation, Toast.LENGTH_SHORT).show();
    }

    /**
     * センサーの角度を0.5秒おきに取得
     */
    private class SensorPollingHandler extends PollingTask {

        private static final int INTERVAL = 2000;

        public SensorPollingHandler() {
            super(INTERVAL);
        }

        @Override
        public boolean onPolling() {
            updateAngle();
            return true;
        }
    }

    /**
     * 角度取得
     * @return
     */
    private synchronized float updateAngle() {
        float angle = mAngle;
        Log.v(TAG, "コールバック角度：" + angle);
        changeAngleView(angle);
        return angle;
    }

    private synchronized void changeAngleView(final float angle) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // テキスト更新
                if (txtSensorPolling != null) {
                    txtSensorPolling.setText("コールバック時の角度：" + angle);
                }
                if (angle == 90) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    btnLandscape.setVisibility(View.GONE);
                    btnPortrait.setVisibility(View.VISIBLE);
                } else if (angle == -90) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                    btnLandscape.setVisibility(View.GONE);
                    btnPortrait.setVisibility(View.VISIBLE);
                } else if (angle == 0) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    btnLandscape.setVisibility(View.VISIBLE);
                    btnPortrait.setVisibility(View.GONE);
                } else if (angle == 180) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                    btnLandscape.setVisibility(View.VISIBLE);
                    btnPortrait.setVisibility(View.GONE);
                }
            }
        });
    }
}
