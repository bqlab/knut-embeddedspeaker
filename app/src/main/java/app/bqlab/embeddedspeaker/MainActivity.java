package app.bqlab.embeddedspeaker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    final int DEVICE_OFF = 0;
    final int SONG_BBIBBI = 1;
    final int SONG_TRAVEL = 2;
    final int SONG_PHONECERT = 3;
    final int SETTING_FND_OFF = 4;
    final int SETTING_FND_ON = 5;
    final int SETTING_MOTOR_OFF = 6;
    final int SETTING_MOTOR_ON = 7;
    final int REQUEST_ENABLE_BT = 8;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice connectedDevice;
    private ConnectionManager connectionManager;

    LinearLayout mainMusic;
    ImageView mainMusicProfile;
    TextView mainMusicName, mainMusicMusician;

    EditText h1, h2, m1, m2, s1, s2;
    Switch mainBodyFndSwitch, mainBodyMotorSwitch;

    Button mainBarPrev, mainBarPlay, mainBarNext;

    boolean isConnected, isFinished, isRunning;
    int currentSong, setTime;

    Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isConnected = false;
    }

    private void init() {
        //Data setting
        currentSong = SONG_BBIBBI;

        //main_music setting
        mainMusic = findViewById(R.id.main_music);
        mainMusicProfile = findViewById(R.id.main_music_profile);
        mainMusicProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isConnected)
                    playCurrentSong();
            }
        });
        mainMusicName = findViewById(R.id.main_music_name);
        mainMusicMusician = findViewById(R.id.main_music_musician);

        //main_body setting
        timer = new Timer();
        h1 = findViewById(R.id.main_body_timer_h1);
        h2 = findViewById(R.id.main_body_timer_h2);
        m1 = findViewById(R.id.main_body_timer_m1);
        m2 = findViewById(R.id.main_body_timer_m2);
        s1 = findViewById(R.id.main_body_timer_s1);
        s2 = findViewById(R.id.main_body_timer_s2);
        h1.setFocusable(View.NOT_FOCUSABLE);
        h2.setFocusable(View.NOT_FOCUSABLE);
        m1.setFocusable(View.NOT_FOCUSABLE);
        m2.setFocusable(View.NOT_FOCUSABLE);
        timer.get().add(h1);
        timer.get().add(h2);
        timer.get().add(m1);
        timer.get().add(m2);
        timer.get().add(s1);
        timer.get().add(s2);
        for (int i = 0; i < timer.get().size(); i++) {
            final int finalI = i;
            timer.get().get(i).addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (!(finalI == 5) && !timer.get().get(finalI).getText().toString().equals(""))
                        timer.get().get(finalI + 1).requestFocus();
                }
            });
        }
        mainBodyFndSwitch = findViewById(R.id.main_body_fnd_switch);
        mainBodyFndSwitch.setChecked(getSharedPreferences("setting", MODE_PRIVATE).getBoolean("fnd", true));
        mainBodyFndSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isConnected) {
                    setAboutBluetooth();
                    mainBodyFndSwitch.setChecked(getSharedPreferences("setting", MODE_PRIVATE).getBoolean("fnd", true));
                } else {
                    getSharedPreferences("setting", MODE_PRIVATE).edit().putBoolean("fnd", isChecked).apply();
                    if (getSharedPreferences("setting", MODE_PRIVATE).getBoolean("fnd", true))
                        connectionManager.write(Integer.toString(SETTING_FND_ON));
                    else
                        connectionManager.write(Integer.toString(SETTING_FND_OFF));
                }
            }
        });
        mainBodyMotorSwitch = findViewById(R.id.main_body_motor_switch);
        mainBodyMotorSwitch.setChecked(getSharedPreferences("setting", MODE_PRIVATE).getBoolean("motor", true));
        mainBodyMotorSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isConnected) {
                    setAboutBluetooth();
                    mainBodyMotorSwitch.setChecked(getSharedPreferences("setting", MODE_PRIVATE).getBoolean("motor", true));
                } else {
                    getSharedPreferences("setting", MODE_PRIVATE).edit().putBoolean("motor", isChecked).apply();
                    if (getSharedPreferences("setting", MODE_PRIVATE).getBoolean("motor", true))
                        connectionManager.write(Integer.toString(SETTING_MOTOR_ON));
                    else
                        connectionManager.write(Integer.toString(SETTING_MOTOR_OFF));
                }
            }
        });

        //main_bar setting
        mainBarPrev = findViewById(R.id.main_bar_prev);
        mainBarPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentSong--;
                showCurrentSong();
            }
        });
        mainBarPlay = findViewById(R.id.main_bar_play);
        mainBarPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (timer.ready()) {
                    showCurrentSong();
                    playCurrentSong();
                }
            }
        });
        mainBarNext = findViewById(R.id.main_bar_next);
        mainBarNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentSong++;
                showCurrentSong();
            }
        });
    }

    private void setAboutBluetooth() {
        try {
            //Necessary objects.
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
            final BluetoothDevice[] pairedDevices = bondedDevices.toArray(new BluetoothDevice[0]);

            //Check the device support bluetooth.
            if (bluetoothAdapter == null)
                showUnsupportedDeviceDialog();
                //Check bluetooth activated.
            else if (!bluetoothAdapter.isEnabled()) {
                Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(i, REQUEST_ENABLE_BT);
            }
            //Check this device have paired devices.
            else if (pairedDevices.length == 0) {
                new AlertDialog.Builder(this)
                        .setTitle("페어링된 디바이스가 없습니다.")
                        .setMessage("블루투스 설정 화면으로 이동하여 디바이스를 페어링한 후 다시 시도하세요.")
                        .setPositiveButton("설정화면으로 이동", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                            }
                        }).show();
            }
            //Check this device connected to other device.
            else if (!isConnected) {
                String[] items = new String[pairedDevices.length];
                for (int i = 0; i < pairedDevices.length; i++)
                    items[i] = pairedDevices[i].getName();

                new AlertDialog.Builder(this)
                        .setTitle("페어링된 디바이스를 선택하세요.")
                        .setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, final int which) {
                                ConnectionTasker connectionTasker = new ConnectionTasker(pairedDevices[which]);
                                connectionTasker.execute();
                                dialog.dismiss();
                            }
                        })
                        .setNeutralButton("설정화면으로 이동", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                            }
                        })
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
            }
        } catch (NullPointerException e) {
            showUnsupportedDeviceDialog();
        }
    }

    @SuppressLint("SetTextI18n")
    private void showCurrentSong() {
        if (isConnected) {
            switch (currentSong) {
                case SONG_BBIBBI:
                    mainMusic.setBackground(getResources().getDrawable(R.drawable.main_music1));
                    mainMusicProfile.setBackground(getResources().getDrawable(R.drawable.main_music_profile1));
                    mainMusicName.setText("삐삐");
                    mainMusicMusician.setText("아이유");
                    break;
                case SONG_TRAVEL:
                    mainMusic.setBackground(getResources().getDrawable(R.drawable.main_music2));
                    mainMusicProfile.setBackground(getResources().getDrawable(R.drawable.main_music_profile2));
                    mainMusicName.setText("여행");
                    mainMusicMusician.setText("볼빨간사춘기");
                    break;
                case SONG_PHONECERT:
                    mainMusic.setBackground(getResources().getDrawable(R.drawable.main_music3));
                    mainMusicProfile.setBackground(getResources().getDrawable(R.drawable.main_music_profile3));
                    mainMusicName.setText("폰서트");
                    mainMusicMusician.setText("10CM");
                    break;
            }
        }
    }

    private void showNoneSong() {
        mainMusic.setBackground(getResources().getDrawable(R.drawable.main_music_none));
        mainMusicProfile.setBackground(getResources().getDrawable(R.drawable.main_music_profile_none));
        mainMusicMusician.setText(getResources().getString(R.string.main_music_musician_none));
        mainMusicName.setText(getResources().getString(R.string.main_music_name_none));
    }

    @SuppressLint("SetTextI18n")
    private void playCurrentSong() {
        if (isConnected) {
            mainBarPlay.setBackground(getResources().getDrawable(R.drawable.main_bar_pause));
            timer.start();
            switch (currentSong) {
                case SONG_BBIBBI:
                    connectionManager.write(Integer.toString(SONG_BBIBBI));
                    break;
                case SONG_TRAVEL:
                    connectionManager.write(Integer.toString(SONG_TRAVEL));
                    break;
                case SONG_PHONECERT:
                    connectionManager.write(Integer.toString(SONG_PHONECERT));
                    break;
            }
        } else
            setAboutBluetooth();
    }

    private void showUnsupportedDeviceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("지원하지 않는 기기입니다.")
                .setMessage("현재 디바이스에서 블루투스 기능을 사용할 수 없습니다. 다른 기기로 시도하세요.")
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finishAffinity();
                    }
                }).show();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_ENABLE_BT:
                    setAboutBluetooth();
                    break;
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            switch (requestCode) {
                case REQUEST_ENABLE_BT:
                    new AlertDialog.Builder(this)
                            .setTitle("블루투스 기능이 꺼져 있습니다.")
                            .setMessage("블루투스 기능이 계속해서 꺼져있을 경우 디바이스와 연결할 수 없습니다.")
                            .setPositiveButton("연결", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    setAboutBluetooth();
                                }
                            })
                            .setNegativeButton("연결하지 않음", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(MainActivity.this, "블루투스 연결을 하지 않습니다.", Toast.LENGTH_LONG).show();
                                    dialog.dismiss();
                                }
                            }).show();
            }
        }
    }

    //Class that try to be connecting device.
    @SuppressLint("StaticFieldLeak")
    private class ConnectionTasker extends AsyncTask<Void, Void, Boolean> {

        private BluetoothSocket socket = null;

        ConnectionTasker(BluetoothDevice device) {
            try {
                connectedDevice = device;
                socket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
                connectionManager = new ConnectionManager(socket);
            } catch (IOException e) {
                showUnsupportedDeviceDialog();
            }
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            bluetoothAdapter.cancelDiscovery();
            try {
                socket.connect();
            } catch (IOException e) {
                try {
                    socket.close();
                    Handler handler = new Handler(Looper.getMainLooper()) {
                        @Override
                        public void handleMessage(Message message) {
                            timer.reset();
                            showNoneSong();
                            MainActivity.this.isConnected = false;
                            Toast.makeText(MainActivity.this, connectedDevice.getName() + " 항목과 연결할 수 없습니다.", Toast.LENGTH_LONG).show();
                        }
                    };
                    handler.obtainMessage().sendToTarget();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                return false;
            }
            return true;
        }
    }

    //Class that manage connection state after be connecting.
    @SuppressLint("StaticFieldLeak")
    private class ConnectionManager extends AsyncTask<Void, String, Boolean> {

        private InputStream inputStream;
        private OutputStream outputStream;
        private BluetoothSocket bluetoothSocket;

        ConnectionManager(BluetoothSocket socket) {
            try {
                bluetoothSocket = socket;
                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                showUnsupportedDeviceDialog();
            }

            Toast.makeText(MainActivity.this, connectedDevice.getName() + " 항목과 연결합니다.", Toast.LENGTH_LONG).show();
            MainActivity.this.isConnected = true;
            showCurrentSong();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            byte[] readBuffer = new byte[1024];
            int readBufferPosition = 0;

            while (true) {
                if (isCancelled())
                    return false;
                try {
                    int bytesAvailable = inputStream.available();
                    if (bytesAvailable > 0) {
                        byte[] packetBytes = new byte[bytesAvailable];
                        int input = inputStream.read(packetBytes);

                        for (int i = 0; i < bytesAvailable; i++) {
                            byte b = packetBytes[i];
                            if (b == '\n') {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                String recvMessage = new String(encodedBytes, "UTF-8");
                                readBufferPosition = 0;
                                publishProgress(recvMessage);
                            } else
                                readBuffer[readBufferPosition++] = b;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        //When message received on Android.
        @Override
        protected void onProgressUpdate(String... recvMessage) {
            Log.d("받은메시지: ", recvMessage[0]);
        }

        @Override
        protected void onPostExecute(Boolean isSucess) {
            super.onPostExecute(isSucess);
            if (isSucess)
                connectionManager.execute();
            else
                closeSocket();
        }

        @Override
        protected void onCancelled(Boolean aBoolean) {
            super.onCancelled();
        }

        private void closeSocket() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void write(String msg) {
            msg += "\n";
            try {
                outputStream.write(msg.getBytes());
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class Timer {
        ArrayList<EditText> timer;
        int setTime = 0;

        Timer() {
            timer = new ArrayList<>();
        }

        ArrayList<EditText> get() {
            return timer;
        }

        boolean ready() {
            if (isConnected) {
                setAboutBluetooth();
                return false;
            } else if (timer.get(4).getText().toString().equals("") || timer.get(5).getText().toString().equals("")) {
                Toast.makeText(MainActivity.this, "타이머를 다시 확인하세요.", Toast.LENGTH_LONG).show();
                return false;
            } else if (isRunning) {
                Toast.makeText(MainActivity.this, "타이머가 끝날 때까지 기다리세요.", Toast.LENGTH_SHORT).show();
                return false;
            } else
                return true;
        }

        void reset() {
            for (EditText e : timer) {
                e.setTextColor(getResources().getColor(R.color.colorBlack));
                e.setText("");
            }

            Handler handler = new Handler(getMainLooper()) {
                @Override
                public void handleMessage(Message message) {
                    s1.setFocusable(true);
                    s2.setFocusable(true);
                    s1.setFocusableInTouchMode(true);
                    s2.setFocusableInTouchMode(true);
                    mainBarPlay.setBackground(getResources().getDrawable(R.drawable.main_bar_play));
                }
            };
            handler.obtainMessage().sendToTarget();
        }

        @SuppressLint("SetTextI18n")
        void start() {
            isRunning = true;

            timer.get(0).setText(Integer.toString(0));
            timer.get(1).setText(Integer.toString(0));
            timer.get(2).setText(Integer.toString(0));
            timer.get(3).setText(Integer.toString(0));

            setTime = Integer.parseInt(s1.getText().toString()) * 10 + Integer.parseInt(s2.getText().toString());

            if (setTime > 59 || setTime < 10) {
                Toast.makeText(MainActivity.this, "타이머를 다시 확인하세요.", Toast.LENGTH_LONG).show();
                for (EditText e : timer) {
                    e.setTextColor(getResources().getColor(R.color.colorBlack));
                    e.setText("");
                }

            } else {

                for (EditText e : timer) {
                    e.setTextColor(getResources().getColor(R.color.colorMagenta));
                    e.setFocusableInTouchMode(false);
                    e.setFocusable(false);
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (isRunning) {
                            try {
                                Thread.sleep(1000);
                                runOnUiThread(new Runnable() {
                                    @SuppressLint("SetTextI18n")
                                    @Override
                                    public void run() {
                                        if (setTime == 0) {
                                            reset();
                                            isRunning = false;
                                        } else {
                                            try {
                                                int logTime = setTime;
                                                logTime -= Integer.parseInt(m2.getText().toString()) * 60;
                                                s1.setText(Integer.toString(logTime / 10));
                                                logTime -= Integer.parseInt(s1.getText().toString()) * 10;
                                                s2.setText(Integer.toString(logTime));
                                            } catch (Exception e) {
                                                isRunning = false;
                                            }
                                        }
                                        setTime--;
                                    }
                                });
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }
        }
    }
}