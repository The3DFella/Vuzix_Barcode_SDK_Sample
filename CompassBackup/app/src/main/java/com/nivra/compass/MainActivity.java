package com.nivra.compass;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

// Explicitly import the R class
import com.nivra.compass.R;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] gravity;
    private float[] geomagnetic;
    private TextView directionTextView;
    private TextView degreeTextView;
    private TextView magnetometerDataTextView;  // New TextView to display raw magnetometer data

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        directionTextView = findViewById(R.id.directionsView);
        degreeTextView = findViewById(R.id.degreeView);
        magnetometerDataTextView = findViewById(R.id.magnetometerDataView);  // Initialize new TextView

        // Initialize SensorManager and sensors
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            // Get the default sensors for accelerometer and magnetic field
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }

        // Ensure sensors are available
        if (accelerometer == null || magnetometer == null) {
            // Handle error - sensors are unavailable
            directionTextView.setText("Sensors not available");
            degreeTextView.setText("");
            magnetometerDataTextView.setText("No Magnetometer Data");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register the sensors to listen for changes
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
        // Unregister sensors when not needed to save battery
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

            // Format the raw magnetometer data to display XYZ on separate lines
            String rawMagnetometerData = "X: " + geomagnetic[0] + "\nY: " + geomagnetic[1] + "\nZ: " + geomagnetic[2];
            magnetometerDataTextView.setText(rawMagnetometerData);
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

                // Ensure azimuth is between 0 and 360 degrees
                if (azimuth < 0) {
                    azimuth += 360;
                }

                // Apply the inversion: Subtract 180° to invert the direction
                azimuth -= 180;

                // Ensure azimuth is still between 0 and 360 degrees after inverting
                if (azimuth < 0) {
                    azimuth += 360;
                } else if (azimuth >= 360) {
                    azimuth -= 360;
                }

                // Set the compass direction and degree text
                directionTextView.setText(getDirectionLabel(azimuth));
                degreeTextView.setText((int) azimuth + "°");
            }
        }
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // This method can be used to handle changes in sensor accuracy, if needed
    }

    // Helper method to convert azimuth to compass direction
    private String getDirectionLabel(float azimuth) {
        if (azimuth >= 337.5 || azimuth < 22.5) return "N";
        if (azimuth >= 22.5 && azimuth < 67.5) return "NE";
        if (azimuth >= 67.5 && azimuth < 112.5) return "E";
        if (azimuth >= 112.5 && azimuth < 157.5) return "SE";
        if (azimuth >= 157.5 && azimuth < 202.5) return "S";
        if (azimuth >= 202.5 && azimuth < 247.5) return "SW";
        if (azimuth >= 247.5 && azimuth < 292.5) return "W";
        if (azimuth >= 292.5 && azimuth < 337.5) return "NW";
        return "N"; // Default to North
    }
}
