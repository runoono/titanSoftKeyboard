package com.runo.softkeyboard;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

public class KeyboardSettings extends AppCompatActivity {

    private InputMethodManager mImm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keyboard_settings);
        mImm = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        Button inputPicker = findViewById(R.id.button);
        inputPicker.setOnClickListener(view -> {
            mImm.showInputMethodPicker();
        });
    }
}