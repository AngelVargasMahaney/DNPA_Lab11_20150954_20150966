package com.example.lab11;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class AudioRecorder {
    private static final String TAG = "Recorder";
    /////// Uso de AudioRecord////////////
    private AudioRecord recorder;
    ///////////////////////////////////
    private HandlerThread recordThread;
    private short nChannels = 2;
    private int sampleRate = 44100;
    private short bSamples = 16;
    private int bufferSize;
    private int aChannels = AudioFormat.CHANNEL_IN_STEREO;
    private int aSource = MediaRecorder.AudioSource.MIC;
    private int aFormat = AudioFormat.ENCODING_PCM_16BIT;

    private int framePeriod;

    private byte[]  buffer;

    private int payloadSize;

    private boolean recording = false;
    private RandomAccessFile grabandoAudio;

    private int noRecordings;

    public AudioRecorder() {
        this.noRecordings = 0;

        int recordLength = 500; // ms
        framePeriod = sampleRate * recordLength / 1000;
        bufferSize = framePeriod * 2 * bSamples * nChannels / 8;
        int audioBufferInit = AudioRecord.getMinBufferSize(sampleRate, nChannels, aFormat);
        Log.d(TAG, "Tamaño del buffer: " + audioBufferInit);
        if (bufferSize < audioBufferInit) {
            bufferSize = audioBufferInit;
            // update frame period
            framePeriod = bufferSize / (2 * bSamples * nChannels / 8);
            Log.w(TAG, "El incremento del buffer " + bufferSize);
        }
    }

    private boolean initRecorder() {
        Log.d(TAG, "Init recorder");
        recorder = new AudioRecord(aSource, sampleRate, aChannels, aFormat, bufferSize);

        int sessionId = recorder.getAudioSessionId();

        if (recordThread == null) {
            recordThread = new HandlerThread("RecorderThread", Thread.MAX_PRIORITY);
            recordThread.start();
        }
        Handler handler = new Handler(recordThread.getLooper());
        recorder.setPositionNotificationPeriod(framePeriod);
        recorder.setRecordPositionUpdateListener(updateListener, handler);

        if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord no puede ser iniciado");
            return false;
        }
        Log.d(TAG, "Grabador iniciado");
        return true;
    }

    public void start(File wavDir) {
        if (recorder == null) {
            initRecorder();
        }

        String filePath = wavDir.getAbsolutePath() + "/sample_" + SystemClock.uptimeMillis() + "_Lab11.wav";
        grabandoAudio = prepareFile(filePath, grabandoAudio);


        payloadSize = 0;
        recording = true;
        if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
            if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED) {
                Log.d(TAG, "Empezar grabacion");
                recorder.startRecording();
                recorder.read(buffer, 0, buffer.length);
            }
        }

    }

    public int stop() {
        recording = false;

        if (recorder != null) {
            if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                recorder.stop();
                Log.d(TAG, "Grabacion detenida");
            }
            recorder.release();
            recorder = null;
            recordThread.interrupt();
            if (grabandoAudio != null) {
                try {
                    grabandoAudio.seek(4); // write size to RIFF header
                    grabandoAudio.writeInt(Integer.reverseBytes(36 + payloadSize));
                    grabandoAudio.seek(40);    // write size to Subchunk2Size field
                    grabandoAudio.writeInt(Integer.reverseBytes(payloadSize));
                    grabandoAudio.close();
                } catch (IOException e) {
                    Log.e(TAG, e.getLocalizedMessage());
                }
            }
        }
        return noRecordings;
    }
    //metodo para acceder a un archivo de forma aleatoria
    public RandomAccessFile prepareFile(String pathToFile, RandomAccessFile file) {
        try {
            Log.d(TAG, "Preparando archivo en la ruta: " + pathToFile);
            // Acceso
            file = new RandomAccessFile(pathToFile, "rw");
            short localNChannels = 2;
            file.setLength(0);    // En 0, para prevenir errores en caso de que el archivo ya exista
            file.writeBytes("RIFF");
            file.writeInt(0); // Tamaño final, en 0 porque aun no se conoce
            file.writeBytes("WAVE");
            file.writeBytes("fmt ");
            file.writeInt(Integer.reverseBytes(16));  // audio de 16 bits
            file.writeShort(Short.reverseBytes((short)1));    // Formato de audio
            file.writeShort(Short.reverseBytes(localNChannels));   // Numero de canales de audio
            file.writeInt(Integer.reverseBytes(sampleRate));
            file.writeInt(Integer.reverseBytes(sampleRate*bSamples*localNChannels/8)); //
            file.writeShort(Short.reverseBytes((short)(nChannels*bSamples/8))); //
            file.writeShort(Short.reverseBytes(bSamples));
            file.writeBytes("data");
            file.writeInt(0);

            buffer = new byte[framePeriod*bSamples/8*nChannels];
            Log.d(TAG, "Archivo WAVE preparado, tamaño del buffer: " + buffer.length);
            return file;
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        return null;
    }

    private AudioRecord.OnRecordPositionUpdateListener updateListener = new AudioRecord.OnRecordPositionUpdateListener() {
        @Override
        public void onMarkerReached(AudioRecord recorder) {
        }

        @Override
        public void onPeriodicNotification(AudioRecord recorder) {

            if (recording) {
                recorder.read(buffer, 0, buffer.length);
                Log.d(TAG, "Intentando grabar en el archivo");
                try {

                    grabandoAudio.write(buffer);

                    payloadSize += buffer.length;
                } catch (Exception e) {
                    Log.e(TAG, e.getLocalizedMessage());
                }
            }
        }
    };
    public void cleanUp() {
        Log.d(TAG, "Limpiando el estado");
        if (recorder != null) {
            if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                recorder.stop();
                recorder.release();
                recorder = null;
            }
        }
        if (recordThread != null) {
            recordThread.quitSafely();
            recordThread.interrupt();
            recordThread = null;
        }
    }
}