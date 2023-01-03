package com.example.plugin;

import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

import hu.tsulok.mlkit.textrecognizer.MLKitTextRecognizerPlugin;

public class MainActivity extends BridgeActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerPlugin(MLKitTextRecognizerPlugin.class);
    }
}
