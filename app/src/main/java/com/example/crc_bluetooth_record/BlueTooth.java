package com.example.crc_bluetooth_record;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BlueTooth extends AppCompatActivity {

    private TextView lastText;
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
    BluetoothDevice selectedDevice = null;
    BluetoothSocket bluetoothSocket;
    OutputStream outputStream = null;
    InputStream inputStream = null;
    int readBufferPosition;
    byte[] readBuffer;
    Thread workerThread = null;
    List<String> text_list = new ArrayList<>();  // 리스트 생성
    ArrayAdapter<String> adapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blue_tooth);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(),"기기가 블루투스를 지원 안함",Toast.LENGTH_LONG).show();
            //finish();
            // Device does not support Bluetooth
        }
        else if(!mBluetoothAdapter.isEnabled()){
            mBluetoothAdapter.enable();
        }
        ListView text_listview = (ListView)findViewById(R.id.text_listview); // 리스트뷰 생성


        // 리스트뷰와 리스트 연결하기 위한 어뎁터
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, text_list){
            @Override
            public View getView(int position, View convertView, ViewGroup parent){
                // Get the Item from ListView
                View view = super.getView(position, convertView, parent);
                // Initialize a TextView for ListView each Item
                TextView tv = (TextView) view.findViewById(android.R.id.text1);
                // Set the text color of TextView (ListView Item)
                tv.setTextColor(Color.rgb(16,24,32));
                // Generate ListView Item using TextView
                return view;
            }
        };

        text_listview.setAdapter(adapter);   // 리스트뷰의 어뎁터를 지정
        lastText = (TextView)findViewById(R.id.lastText);   // 텍스트뷰 생성
        lastText.setTextColor(Color.rgb(16,24,32)); // 텍스트뷰 폰트 색





        text_list.add("TEST1");
        text_list.add("dddddddddddddddddddddddddddddddddddddddddd\ndddddddddddddddddddddddd");
        text_list.add("TEST3");
        text_list.add("TEST1");
        text_list.add("TEST2");
        text_list.add("TEST3");
        text_list.add("TEST1");
        text_list.add("TEST2");
        text_list.add("TEST3");
        text_list.add("TEST1");
        text_list.add("TEST2");
        text_list.add("ddddddddddddddddddddddddddddddddddddddd\nddd");

        //리스트뷰의 아이템을 클릭시 해당 아이템의 문자열을 가져오기 위한 처리
        String last_item = (String)adapter.getItem(text_list.size()-1);
        //텍스트뷰에 출력
        lastText.setText(last_item);
    }

    public void onBackButtonClicked(View v){
        //Toast.makeText(getApplicationContext(),"뒤로가기 눌렸음",Toast.LENGTH_LONG.show());
        finish();
    }

    public void scan(View view) {
        List<String> ble_list = new ArrayList<String>();
        for(BluetoothDevice bt : pairedDevices)
            ble_list.add(bt.getName());
        CharSequence[] ble_array = ble_list.toArray(new String[0]);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("목록");
        builder.setItems(ble_array, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(BlueTooth.this, ble_array[which], Toast.LENGTH_SHORT).show();
                dialog.dismiss();

                for(BluetoothDevice device : pairedDevices){
                    if(ble_array[which].equals(device.getName())){
                        selectedDevice = device;
                        break;
                    }
                }
                UUID uuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

                // Rfcomm 채널을 통해 블루투스 디바이스와 통신하는 소켓 생성
                try {
                    bluetoothSocket = selectedDevice.createRfcommSocketToServiceRecord(uuid);
                    bluetoothSocket.connect();
                    // 데이터 송,수신 스트림을 얻어옵니다.
                    outputStream = bluetoothSocket.getOutputStream();
                    inputStream = bluetoothSocket.getInputStream();
                    // 데이터 수신 함수 호출
                    receiveData();
                    Toast.makeText(getApplicationContext(),"연결됨",Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    workerThread.interrupt();
                    Toast.makeText(getApplicationContext(),"연결 끊김",Toast.LENGTH_LONG).show();
                }
            }
        });
        builder.show();

    }

    public void receiveData() {
        final Handler handler = new Handler();
        // 데이터를 수신하기 위한 버퍼를 생성
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        // 데이터를 수신하기 위한 쓰레드 생성
        workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted()) {
                    try {
                        // 데이터를 수신했는지 확인합니다.
                        int byteAvailable = inputStream.available();
                        // 데이터가 수신 된 경우
                        if(byteAvailable > 0) {
                            // 입력 스트림에서 바이트 단위로 읽어 옵니다.
                            byte[] bytes = new byte[byteAvailable];
                            inputStream.read(bytes);
                            // 입력 스트림 바이트를 한 바이트씩 읽어 옵니다.
                            for(int i = 0; i < byteAvailable; i++) {
                                byte tempByte = bytes[i];
                                // 개행문자를 기준으로 받음(한줄)
                                if(tempByte == '\n') {
                                    // readBuffer 배열을 encodedBytes로 복사
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    // 인코딩 된 바이트 배열을 문자열로 변환
                                    final String text = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            // 텍스트 뷰에 출력
                                            text_list.add(text);
                                            lastText.setText(text);
                                            adapter.notifyDataSetChanged();
                                        }
                                    });
                                } // 개행 문자가 아닐 경우
                                else {
                                    readBuffer[readBufferPosition++] = tempByte;
                                }
                            }
                        }
                    } catch (IOException e) {
                        workerThread.interrupt();
                        Toast.makeText(getApplicationContext(),"연결 끊김",Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
        workerThread.start();
    }


    public void connect(View view) {
        UUID uuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        // Rfcomm 채널을 통해 블루투스 디바이스와 통신하는 소켓 생성
        try {
            if(selectedDevice == null){
                Toast.makeText(getApplicationContext(),"선택 안됨",Toast.LENGTH_LONG).show();
                return;
            }
            bluetoothSocket = selectedDevice.createRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect();
            // 데이터 송,수신 스트림을 얻어옵니다.
            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();
            // 데이터 수신 함수 호출
            receiveData();
        } catch (IOException e) {
            workerThread.interrupt();
            Toast.makeText(getApplicationContext(),"연결 끊김",Toast.LENGTH_LONG).show();
        }
    }

    public void clear_list(View view) {
        text_list.clear();
        adapter.notifyDataSetChanged();
    }
}