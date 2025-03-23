package com.nivra.compass;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Color;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] gravity;
    private float[] geomagnetic;
    private TextView directionTextView;
    private TextView degreeTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        directionTextView = findViewById(R.id.directionTextView);
        degreeTextView = findViewById(R.id.degreeTextView);

        // Initialize SensorManager and sensors
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }

        // Ensure sensors are available
        if (accelerometer == null || magnetometer == null) {
            directionTextView.setText("Sensors not available");
            degreeTextView.setText("");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
        if (magnetometer != null) {
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unregister sensor listener to prevent memory leaks
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Ensure sensors are unregistered when activity is destroyed
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null) return;

        // Update gravity and geomagnetic values based on sensor type
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values.clone();
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = event.values.clone();
        }

        // If we have both gravity and geomagnetic data, calculate orientation
        if (gravity != null && geomagnetic != null) {
            float[] r = new float[9];
            float[] i = new float[9];

            // Get rotation matrix
            if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(r, orientation);
                float azimuth = (float) Math.toDegrees(orientation[0]); // Convert azimuth to degrees

                if (azimuth < 0) {
                    azimuth += 360;
                }

                azimuth -= 180; // Apply inversion (optional)

                if (azimuth < 0) {
                    azimuth += 360;
                } else if (azimuth >= 360) {
                    azimuth -= 360;
                }

                // Update direction and degree text views
                String direction = getDirectionLabel(azimuth);
                String degree = (int) azimuth + "°";

                directionTextView.setText(direction);
                degreeTextView.setText(degree);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle sensor accuracy changes if needed
    }

    // Helper method to convert azimuth to compass direction and set text color
    private String getDirectionLabel(float azimuth) {
        String direction = "N";
        int color = Color.WHITE; // Default color

        if (azimuth >= 337.5 || azimuth < 22.5) {
            direction = "N";
            color = Color.parseColor("#FF0000"); // Red for North
        } else if (azimuth >= 22.5 && azimuth < 67.5) {
            direction = "NE";
            color = Color.parseColor("#FF7F7F"); // Light red for NE
        } else if (azimuth >= 67.5 && azimuth < 112.5) {
            direction = "E";
            color = Color.WHITE; // Default for East (no color change)
        } else if (azimuth >= 112.5 && azimuth < 157.5) {
            direction = "SE";
            color = Color.parseColor("#7F7FFF"); // Light blue for SE
        } else if (azimuth >= 157.5 && azimuth < 202.5) {
            direction = "S";
            color = Color.parseColor("#0000FF"); // Blue for South
        } else if (azimuth >= 202.5 && azimuth < 247.5) {
            direction = "SW";
            color = Color.parseColor("#7F7FFF"); // Light blue for SW
        } else if (azimuth >= 247.5 && azimuth < 292.5) {
            direction = "W";
            color = Color.WHITE; // Default for West (no color change)
        } else if (azimuth >= 292.5 && azimuth < 337.5) {
            direction = "NW";
            color = Color.parseColor("#FF7F7F"); // Light red for NW
        }

        directionTextView.setTextColor(color); // Set the color of the direction text
        return direction;
    }
}
