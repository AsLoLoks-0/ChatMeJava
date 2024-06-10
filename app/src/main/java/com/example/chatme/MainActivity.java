package com.example.chatme;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.net.URISyntaxException;
import java.util.ArrayList;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    String UserNick = "Anon";
    String UserRoom = "All";
    String UserMessage = "";

    private Socket _Socket;
    private ArrayList<String> messageList;
    private ArrayAdapter<String> messageAdapter;
    private ListView ListViewChat;

    {
        Log.i("socket.io", "Establishing connection");

        try {
            _Socket = IO.socket("https://elegant-monarch-deadly.ngrok-free.app/");
            Log.i("socket.io", "Connection succeeded");

        } catch (URISyntaxException e) {
            Log.e("socket.io", "Connection fail");
        }
    }

    public JSONObject PreparedDataToSend() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("nick", UserNick);
            jsonObject.put("room", UserRoom);
            jsonObject.put("message", UserMessage);

            return jsonObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        _Socket.connect();
        _Socket.on("server_respond", addToChat);

        Button ButtonSend = findViewById(R.id.ButtonSend);
        Button ButtonSwitchRoom = findViewById(R.id.ButtonRoomSwitch);
        EditText EditTextNick = findViewById(R.id.editTextNick);
        EditText EditTextMessage = findViewById(R.id.editTextMessage);
        TextView TextViewCurrentRoom = findViewById(R.id.textViewCurrentRoom);
        TextView TextViewSid = findViewById(R.id.textViewUserSid);
        ListViewChat = findViewById(R.id.listViewChat);

        TextViewCurrentRoom.setText("All");
        _Socket.emit("join_room", UserRoom);


        // Initialize message list and adapter
        messageList = new ArrayList<>();
        messageAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, messageList);
        ListViewChat.setAdapter(messageAdapter);

        ButtonSwitchRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                _Socket.emit("exit_room", UserRoom);
                UserRoom = String.valueOf(EditTextMessage.getText());
                TextViewCurrentRoom.setText(String.valueOf(EditTextMessage.getText()));
                _Socket.emit("join_room", UserRoom);
                messageList.clear();
                messageAdapter.notifyDataSetChanged();
                EditTextMessage.setText("");
            }
        });

        ButtonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UserNick = String.valueOf(EditTextNick.getText());
                UserMessage = String.valueOf(EditTextMessage.getText());
                JSONObject Data = PreparedDataToSend();
                _Socket.emit("client_to_server_message", Data);
                TextViewSid.setText(_Socket.id());
                EditTextMessage.setText("");
            }
        });
        TextViewSid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String textToCopy = TextViewSid.getText().toString();
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("SID", textToCopy);
                clipboard.setPrimaryClip(clip);

                Toast.makeText(MainActivity.this, "SID copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        TextViewCurrentRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String textToCopy = TextViewCurrentRoom.getText().toString();
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Room", textToCopy);
                clipboard.setPrimaryClip(clip);

                Toast.makeText(MainActivity.this, "Current room copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        ListViewChat.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedElement = (String) parent.getItemAtPosition(position);

                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Message", selectedElement);
                clipboardManager.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, "Copied message", Toast.LENGTH_SHORT).show();
            }
        });



    }

    private Emitter.Listener addToChat = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject data = new JSONObject ((String) args[0]);
                        String message = data.getString("message");
                        String nick = data.getString("nick");
                        String formattedMessage = nick + ": " + message;
                        messageList.add(formattedMessage);
                        messageAdapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                }
            });
        }
    };
}
