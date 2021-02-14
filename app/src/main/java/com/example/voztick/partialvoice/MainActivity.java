package com.example.voztick.partialvoice;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

@TargetApi(Build.VERSION_CODES.ECLAIR)
public class MainActivity extends AppCompatActivity {
    long start = 0;
    long elapsed = 0;

    //Declarando Variáveis de Configuração Bluetooth
    boolean bluetoothIsConnected = false;
    private static String MAC = null;
    final int bluetoothRequest = 1;
    final int bluetoothPair = 2;
    final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    BluetoothDevice bluetoothDevice = null;
    BluetoothSocket bluetoothSocket = null;
    UUID meuUUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private OutputStream outputStream;

    //Declarando Variáveis Relacionadas ao Reconhecimento de Fala
    private SpeechRecognizer speech = null;
    boolean isListenning = false;

    //Declarando elementos da tela
    Button bluetoothBtn;
    ImageButton micButton;
    private TextView command;

    //Setando contexto dos logs
    private static final String TAG = "MyActivity";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (shouldAskPermissions()) {
            askPermissions();
        }
        requestRecordAudioPermission();

        command = (TextView) findViewById(R.id.commandText);
        micButton = (ImageButton) findViewById(R.id.micButton);
        bluetoothBtn = (Button) findViewById(R.id.bluetoothButton);


        speech = SpeechRecognizer.createSpeechRecognizer(this);
        speech.setRecognitionListener(new listener());
        ActivityCompat.requestPermissions(
                this, new String[]{Manifest.permission.RECORD_AUDIO}, 19);

        //Função do Botão de Bluetooth
        if (bluetoothAdapter == null) {
            Toast.makeText(
                    getApplicationContext(),
                    "Seu dispositivo não suporta bluetooth", Toast.LENGTH_LONG).show();
            finish();
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                final Intent ativaBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(ativaBluetooth, bluetoothRequest);
            }
        }
        bluetoothBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (bluetoothIsConnected) {
                    try {
                        bluetoothSocket.close();
                        bluetoothIsConnected = false;
                    } catch (final IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    final Intent abreLista = new Intent(
                            MainActivity.this, DeviceList.class);
                    startActivityForResult(abreLista, bluetoothPair);
                }
            }
        });

        // Função do botão de Microfone
        micButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (!isListenning) {
                    speech.startListening(getRecognizerIntent());
                    isListenning = true;
                } else {
                    speech.stopListening();
                    command.setText("Reconhecimento de Fala interrompido");
                    isListenning = false;
                }
            }
        });

    }


    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case bluetoothRequest:
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(getApplicationContext(), "Bluetooth Ativado", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "O Bluetooth NÃO Foi Ativado, encerrando a aplicação", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            case bluetoothPair:
                if (resultCode == RESULT_OK) {
                    MAC = data.getExtras().getString(DeviceList.MAC);
                    bluetoothDevice = bluetoothAdapter.getRemoteDevice(MAC);
                    try {
                        bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(meuUUID);
                        bluetoothSocket.connect();
                        outputStream = bluetoothSocket.getOutputStream();
                        Toast.makeText(getApplicationContext(), "Conectado com: " + MAC, Toast.LENGTH_LONG).show();
                        bluetoothIsConnected = true;
                    } catch (final IOException e) {
                        Toast.makeText(getApplicationContext(), "Ocorreu um erro \n " + e, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Conexão não estabelecida", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    protected boolean shouldAskPermissions() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    @TargetApi(23)
    protected void askPermissions() {
        final String[] permissions = {
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.RECORD_AUDIO"
        };
        final int requestCode = 200;
        requestPermissions(permissions, requestCode);
    }

    private void requestRecordAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final String requiredPermission = Manifest.permission.RECORD_AUDIO;

            if (checkCallingOrSelfPermission(requiredPermission) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(new String[]{requiredPermission}, 101);
            }
        }
    }

    public Intent getRecognizerIntent() {
        final Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000);

        return intent;
    }

    class listener implements RecognitionListener {
        @Override
        public void onReadyForSpeech(final Bundle params) {
            Log.d(TAG, "onReadyForSpeech");
            command.setGravity(Gravity.CENTER_HORIZONTAL);
            command.setText("Ouvindo...");
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech");
            command.setGravity(Gravity.CENTER_HORIZONTAL);
            command.setText("Ouvindo...");
            start = System.currentTimeMillis();
        }

        @Override
        public void onRmsChanged(final float rmsdB) {
        }

        @Override
        public void onBufferReceived(final byte[] buffer) {
            Log.d(TAG, "onBufferReceived");
        }

        @Override
        public void onEndOfSpeech() {
            Log.d(TAG, "onEndofSpeech");
        }

        @Override
        public void onError(final int error) {
            Log.d(TAG, "error " + error);
            command.setGravity(Gravity.CENTER_HORIZONTAL);
            command.setText("");
            if (error == 6 || error == 7) {
                restartListening();
            }
        }


        @Override
        public void onEvent(final int eventType, final Bundle params) {
            Log.d(TAG, "onEvent " + eventType);
        }

        @RequiresApi(api = Build.VERSION_CODES.N)


        @Override
        public void onResults(final Bundle results) {
            final ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            String stringCommand = "";

            if (matches != null) {
                stringCommand = matches.get(0);
            }else {
                restartListening();
            }

            if (commandIsValid(stringCommand)) {
                stringCommand = stringCommand.trim().replaceAll(" +", ",");
                final String[] r = stringCommand.split(",");
                command.setGravity(Gravity.CENTER_HORIZONTAL);
                command.setText("Comando enviado:\n");
                for (final String s : r) {
                    Arrays.stream(TextCommandsEnum.values()).forEach(new Consumer<TextCommandsEnum>() {
                        @Override
                        public void accept(TextCommandsEnum tc) {
                            if(s.contains(tc.getAlias())){
                                if(bluetoothIsConnected) {
                                    try {
                                        outputStream.write(tc.getJoystickCommand().getBytes());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                command.setText(command.getText() + " " + tc.name());
                            }
                        }
                    });
                }
            }
            elapsed = System.currentTimeMillis() - start;
            Log.d(TAG, "Tempo de envio: " + elapsed);
            restartListening();
        }

        @Override
        public void onPartialResults(final Bundle bundle) {

        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        private boolean commandIsValid(final String command) {
            return Arrays.stream(TextCommandsEnum.values()).anyMatch(new Predicate<TextCommandsEnum>() {
                @Override
                public boolean test(TextCommandsEnum tc) {
                    return command.contains(tc.getAlias());
                }
            });
        }

        private void restartListening() {
            start = 0;
            elapsed = 0;
            speech.stopListening();
            isListenning = false;
            speech.startListening(getRecognizerIntent());
            isListenning = true;
        }
    }
}