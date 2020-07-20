package com.example.lab11;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_PERMISSION_RECORD_AUDIO = 0;
    private static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 1;
    private ImageView imageViewMicrofono;
    private AudioRecorder audioRecorder;
    private TextView txtInstrucciones;
    private File recordingDir;
    private Button btnStart;
    private Button btnStop;
    private TextView txtRuta;
    File root;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /////////// Permisos para la aplicación///////////
        int permissionWriteToStorage = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permissionWriteToStorage == PackageManager.PERMISSION_DENIED) {
            requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);
        } else {
            setUpWorkingDirectory();
        }
        int permissionAudio = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO);

        if (permissionAudio == PackageManager.PERMISSION_DENIED) {
            requestPermission(Manifest.permission.RECORD_AUDIO, REQUEST_PERMISSION_RECORD_AUDIO);
        } else {
            audioRecorder = new AudioRecorder();
        }
        //////////////////////////////////////////////////

        imageViewMicrofono = (ImageView)findViewById(R.id.id_imgMicrofono);
        btnStart = (Button) findViewById(R.id.btnStartRecording);
        txtInstrucciones = (TextView) findViewById(R.id.id_txtInstrucciones);
        txtRuta = (TextView) findViewById(R.id.id_ruta);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecording(v);
                btnStart.setEnabled(false);
                btnStop.setEnabled(true);
                imageViewMicrofono.setImageResource(R.mipmap.ic_microfono);
                txtInstrucciones.setText("Presione el botón Detener para terminar de grabar");
            }
        });
        btnStop = (Button) findViewById(R.id.btnStopRecording);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording(v);
                btnStart.setEnabled(true);
                btnStop.setEnabled(false);
                imageViewMicrofono.setImageResource(R.mipmap.ic_microfono_off);
                txtInstrucciones.setText("Presione el botón Grabar para iniciar una nueva grabación");
                txtRuta.setText(recordingDir.getPath());

            }
        });
    }

    private void requestPermission(String permission, int permissionCode) {
        ActivityCompat.requestPermissions(this, new String[]{permission}, permissionCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE:
                setUpWorkingDirectory();
                break;
            case REQUEST_PERMISSION_RECORD_AUDIO:
                if (audioRecorder == null) {
                    audioRecorder = new AudioRecorder();
                }
                break;
            default:
                break;
        }
    }

    /**
     * Verificar los permisos para crear un archivo en el celular, crearlo si se tiene los permisos necesarios
     */
    private void setUpWorkingDirectory() {
        int permissionStorageWrite = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permissionStorageWrite == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(this, "Debe permitir guardar archivos en su directorio", Toast.LENGTH_SHORT).show();
            return;
        }
        File externalStorage = new File(android.os.Environment.getExternalStorageDirectory().getAbsolutePath());
        root = new File(externalStorage.getAbsolutePath() + "/android_audio_record_stereo");

        if (root.mkdirs()) {
            Log.d(TAG, "Creando la ruta del archivo");
        }
        // Creación del directorio
        Date date = new Date();
        String dia = (String) android.text.format.DateFormat.format("dd", date);
        String mes = (String) android.text.format.DateFormat.format("MM", date);
        recordingDir = new File(root.getAbsolutePath() + "/wav_samples_" + mes + "_" + dia);
        if (recordingDir.mkdirs()) {
            Log.d(TAG, "Directorio creado qeu contiene todos los archivos grabados el día de hoy");
        }
    }

    public void startRecording(View view) {
        // Verificando los permisos para la grabacion
        int permissionAudioRecord = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO);

        if (permissionAudioRecord == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(this, "Debe permitir el acceso al micrófono", Toast.LENGTH_SHORT).show();
            return;
        }

        File workingDir = new File(recordingDir.getAbsolutePath() + "/sample_" + SystemClock.elapsedRealtime());
        if (workingDir.mkdir()) {
            Log.d(TAG, "Dirección del directorio: " + workingDir.getAbsolutePath());
        }
        audioRecorder.start(workingDir);
    }
    public void stopRecording(View view) {
        int grabaciones = audioRecorder.stop();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioRecorder.stop();
        audioRecorder.cleanUp();
    }
}
