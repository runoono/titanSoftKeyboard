/*
 * Copyright (C) 2008-2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.runo.softkeyboard;

import static com.runo.softkeyboard.LatinKeyboardView.KEYCODE_CTRL;
import static com.runo.softkeyboard.LatinKeyboardView.NOT_A_KEY;

import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Example of writing an input method for a soft keyboard.  This code is
 * focused on simplicity over completeness, so it should in no way be considered
 * to be a complete soft keyboard implementation.  Its purpose is to provide
 * a basic example for how you would get started writing an input method, to
 * be fleshed out as appropriate.
 */
public class SoftKeyboard extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private static final String TAG = "titan keyboard";
    private InputMethodManager mInputMethodManager;
    private LatinKeyboardView mInputView;
    private LatinKeyboard mSymbolsKeyboard;
    private LatinKeyboard mSymbolsShiftedKeyboard;
    private LatinKeyboard mQwertyKeyboard;
    private LatinKeyboard mNumericKeyboard;
    private LatinKeyboard mCurKeyboard;
    private long lastShiftTime = 0L;
    private long lastAltTime = 0L;
    private boolean altLock = false;
    private boolean shiftLock = false;
    private boolean altShortcut = false;
    private boolean shiftShortcut = false;
    private boolean isCtrlPressed = false;
    private Vibrator vibrationService;
//    private final ExecutorService keyboardExecutor = Executors.newSingleThreadExecutor();

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //note: this method is a bit spammy due to key repetition
        Log.i(TAG, "onKeyDown: "+keyCode);

        if((keyCode == KeyEvent.KEYCODE_PROG_RED || keyCode == KeyEvent.KEYCODE_CTRL_LEFT) && !isCtrlPressed){
            isCtrlPressed =true;
            mQwertyKeyboard.setCtrlState(true);
        }

        //I have decided that I fucking hate the default alt+backspace action, no I will not elaborate
        if((altLock || mCurKeyboard == mNumericKeyboard) && keyCode == KeyEvent.KEYCODE_DEL){ //dont sent alt+backspace with alt lock since it acts like ctrl backspace, its annoying
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
            return true;
        }

        LatinKeyboard current = (LatinKeyboard) mInputView.getKeyboard();
        if (current == mSymbolsKeyboard || current == mSymbolsShiftedKeyboard) { //translate physical keys to on screen keyboard
            if (keyCode == KeyEvent.KEYCODE_BACK){
                mInputView.setKeyboard(mCurKeyboard);
                return true;
            }
            try {
                int pressedKey = translateKeyToIndex(keyCode);
                if(pressedKey != NOT_A_KEY ){
                    int code = current.getKeys().get(pressedKey).codes[0];
                    handleCharacter(code, null);
                    return true;
                }
            }catch (Exception e){
                Log.e(TAG, "onKeyDown: ", e);
            }
        }


        if(altLock && keyCode != KeyEvent.KEYCODE_SPACE){
            KeyEvent ke = new KeyEvent(event.getDownTime(), event.getEventTime(), event.getAction(), event.getKeyCode(), event.getRepeatCount(), KeyEvent.META_ALT_ON, event.getDeviceId(), event.getScanCode());
            getCurrentInputConnection().sendKeyEvent(ke);
            ke = KeyEvent.changeAction(ke, KeyEvent.ACTION_UP);
            getCurrentInputConnection().sendKeyEvent(ke);
            return true;
        } else if (shiftLock) {
            KeyEvent ke = new KeyEvent(event.getDownTime(), event.getEventTime(), event.getAction(), event.getKeyCode(), event.getRepeatCount(), KeyEvent.META_SHIFT_ON, event.getDeviceId(), event.getScanCode());
            getCurrentInputConnection().sendKeyEvent(ke);
            ke = KeyEvent.changeAction(ke, KeyEvent.ACTION_UP);
            getCurrentInputConnection().sendKeyEvent(ke);
            return true;
        }


        if(event.isAltPressed()){
            switch (keyCode){
                case KeyEvent.KEYCODE_ENTER:
                    sendDefaultEditorAction(true);
                    return true;
                case KeyEvent.KEYCODE_SPACE:
                    handleCharacter(' ', null);
                    return true;
            }
        }

        if(event.isShiftPressed()){
            switch (keyCode){
                case KeyEvent.KEYCODE_ENTER:
                    handleCharacter('\n', null);
                    return true;
                case KeyEvent.KEYCODE_DEL:
                    sendDownUpKeyEvents(KeyEvent.KEYCODE_FORWARD_DEL);
                    return true;
            }
        }

        if(current.isCtrlOn() || (isCtrlPressed && !(keyCode == KeyEvent.KEYCODE_PROG_RED || keyCode == KeyEvent.KEYCODE_CTRL_LEFT))){
            KeyEvent ke = new KeyEvent(event.getDownTime(), event.getEventTime(), event.getAction(), event.getKeyCode(), event.getRepeatCount(), KeyEvent.META_CTRL_ON, event.getDeviceId(), event.getScanCode());
            getCurrentInputConnection().sendKeyEvent(ke);
            ke = KeyEvent.changeAction(ke, KeyEvent.ACTION_UP);
            getCurrentInputConnection().sendKeyEvent(ke);
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) { //pkb key up
        // If we want to do transformations on text being entered with a hard
        // keyboard, we need to process the up events to update the meta key
        // state we are tracking.
        Log.i(TAG, "onKeyUp: "+keyCode);

        if((keyCode == KeyEvent.KEYCODE_PROG_RED || keyCode == KeyEvent.KEYCODE_CTRL_LEFT)&& isCtrlPressed) {
            isCtrlPressed = false;
            mQwertyKeyboard.setCtrlState(false);
        }

        if (event.isAltPressed()) { //bindings for alt key
            if(keyCode != KeyEvent.KEYCODE_ALT_RIGHT){
                altShortcut = true;
                if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT) { //alt+space
                    cycleThroughKeyboardsLayers();
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null) {
                        ic.clearMetaKeyStates(KeyEvent.META_ALT_ON); //clear alt so it does mess up the next char
                    }
                }
                return true;
            }
        }
        if(keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SYM){
            cycleThroughKeyboardsLayers();
            return true;
        }

        Keyboard current = mInputView.getKeyboard();
        if(current == mQwertyKeyboard){
            if(keyCode == KeyEvent.KEYCODE_ALT_RIGHT){
                getCurrentInputConnection().clearMetaKeyStates(KeyEvent.META_SHIFT_ON);
                shiftLock = false;
                if(altShortcut){//key up was called after an alt shortcut ie alt + shift, alt + space
                    altShortcut = false;
                }else{
                    if(altLock){
                        if((System.currentTimeMillis() - lastAltTime) > 300L){
                            Log.d(TAG, "onKeyUp: alt lock off");
                            altLock = false;
                            getCurrentInputConnection().clearMetaKeyStates(KeyEvent.META_ALT_ON);
                            vibrate(1);
                            return true;
                        }
                    }else {//check for double tap
                        if((System.currentTimeMillis() - lastAltTime) < 800L){
                            Log.d(TAG, "onKeyUp: alt lock on");
                            altLock = true;
                            vibrate(2);

                            return true;
                        }else{
                            lastAltTime = System.currentTimeMillis();
                        }
                    }
                }
            }


            if(keyCode == KeyEvent.KEYCODE_SHIFT_LEFT){
                getCurrentInputConnection().clearMetaKeyStates(KeyEvent.META_ALT_ON);
                altLock = false;
                if(shiftShortcut){
                    shiftShortcut = false;
                }else{
                    if(shiftLock){
                        if((System.currentTimeMillis() - lastShiftTime) > 300L){
                            Log.d(TAG, "onKeyUp: shift lock off");
                            shiftLock = false;
                            getCurrentInputConnection().clearMetaKeyStates(KeyEvent.META_SHIFT_ON);
                            vibrate(1);
                            return true;
                        }
                    }else {//check for double tap
                        if((System.currentTimeMillis() - lastShiftTime) < 800L){
                            Log.d(TAG, "onKeyUp: shift lock on");
                            vibrate(2);
                            shiftLock = true;
                            return true;
                        }else{
                            lastShiftTime = System.currentTimeMillis();
                        }
                    }
                }
            }
        } else if (current == mNumericKeyboard) {
            return super.onKeyUp(keyCode, event);
        }

        return super.onKeyUp(keyCode, event);
    }

//    /**
//     * Helper to send a key down / key up pair to the current editor.
//     */
//    private void keyDownUp(int keyEventCode) {
//        getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
//        getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
//    }


    // Implementation of KeyboardViewListener
    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        Log.d(TAG, "onKey: "+primaryCode);
        if(primaryCode == NOT_A_KEY || primaryCode == KEYCODE_CTRL)
            return;
//        if (primaryCode == Keyboard.KEYCODE_CANCEL) {
//            handleClose();
//            return;
//        }
        else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE
                && mInputView != null) {
            cycleThroughKeyboardsLayers();
        } else if(primaryCode == LatinKeyboardView.KEYCODE_LEFT){
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT);
        } else if(primaryCode == LatinKeyboardView.KEYCODE_RIGHT){
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT);
        }
        else {
            handleCharacter(primaryCode, keyCodes);
        }
    }


    /**
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
    @Override public void onCreate() {
        Log.d(TAG, "onCreate: ");
        super.onCreate();
        mInputMethodManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        vibrationService = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }
    
    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    @Override
    public void onInitializeInterface() {
        Log.d(TAG, "onInitializeInterface: ");
//        if (mQwertyKeyboard != null) {
//            // Configuration changes can happen after the keyboard gets recreated,
//            // so we need to be able to re-build the keyboards if the available
//            // space has changed.
//            int displayWidth = getMaxWidth();
//            if (displayWidth == mLastDisplayWidth) return;
//            mLastDisplayWidth = displayWidth;
//        }

        mQwertyKeyboard = new LatinKeyboard(this, R.xml.qwerty);
        mSymbolsKeyboard = new LatinKeyboard(this, R.xml.symbols);
        mSymbolsShiftedKeyboard = new LatinKeyboard(this, R.xml.symbols_shift);
        mNumericKeyboard = new LatinKeyboard(this, R.xml.numpad);
    }
    
    /**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
    @Override
    public View onCreateInputView() {
        Log.d(TAG, "onCreateInputView: ");
        mInputView = (LatinKeyboardView) getLayoutInflater().inflate(R.layout.input, null);
        mInputView.setOnKeyboardActionListener(this);
        mInputView.setKeyboard(mQwertyKeyboard);
        return mInputView;
    }

    /**
     * Called by the framework when your view for showing candidates needs to
     * be generated, like {@link #onCreateInputView}.
     */
    @Override
    public View onCreateCandidatesView() {
        Log.d(TAG, "onCreateCandidatesView: ");
        return null;
    }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        Log.d(TAG, "onStartInput: ");
        super.onStartInput(attribute, restarting);

        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_DATETIME:
            case InputType.TYPE_CLASS_PHONE:
                mCurKeyboard = mNumericKeyboard;
//                mCurKeyboard = mSymbolsKeyboard;
                break;

            case InputType.TYPE_CLASS_TEXT:
                // This is general text editing.  We will default to the
                // normal alphabetic keyboard, and assume that we should
                // be doing predictive text (showing candidates as the
                // user types).
                mCurKeyboard = mQwertyKeyboard;
                
                // We now look for a few special variations of text that will
                // modify our behavior.
//                int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
//                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
//                        variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
//                    mPredictionOn = false;
//                }
                
//                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
//                        || variation == InputType.TYPE_TEXT_VARIATION_URI
//                        || variation == InputType.TYPE_TEXT_VARIATION_FILTER) {
                    // Our predictions are not useful for e-mail addresses
                    // or URIs.
//                    mPredictionOn = false;
//                }
                
//                if ((attribute.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    // If this is an auto-complete text view, then our predictions
                    // will not be shown and instead we will allow the editor
                    // to supply their own.  We only show the editor's
                    // candidates when in fullscreen mode, otherwise relying
                    // own it displaying its own UI.
//                    mPredictionOn = false;
//                    mCompletionOn = isFullscreenMode();
//                }
                
                // We also want to look at the current state of the editor
                // to decide whether our alphabetic keyboard should start out
                // shifted.
//                updateShiftKeyState(attribute);
                break;
                
            default:
                // For all unknown input types, default to the alphabetic
                // keyboard with no special features.
                mCurKeyboard = mQwertyKeyboard;
//                updateShiftKeyState(attribute);
        }
        
        // Update the label on the enter key, depending on what the application
        // says it will do.
//        mCurKeyboard.setImeOptions(getResources(), attribute.imeOptions); //my eyes have been opened
    }

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override
    public void onFinishInput() {
        Log.d(TAG, "onFinishInput: ");
        super.onFinishInput();
        altLock = false;
        shiftLock = false;
        altShortcut=false;
        mQwertyKeyboard.setCtrlState(false);
        if (mInputView != null) {
            mInputView.closing();
        }
    }
    
    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        Log.d(TAG, "onStartInputView: ");
//        keyboardViewRequested = true;
        super.onStartInputView(attribute, restarting);
        // Apply the selected keyboard to the input view.
        mInputView.setKeyboard(mCurKeyboard);
//        mInputView.closing();
//        final InputMethodSubtype subtype = mInputMethodManager.getCurrentInputMethodSubtype();
//        mInputView.setSubtypeOnSpaceKey(subtype);
    }

    @Override
    public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype) {
        Log.d(TAG, "onCurrentInputMethodSubtypeChanged: ");
        mInputView.setSubtypeOnSpaceKey(subtype);
    }

    /**
     * Deal with the editor reporting movement of its cursor.
     */
    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
        Log.d(TAG, "onUpdateSelection: ");
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
    }

    @Override
    public void onText(CharSequence text) {
    }

    private void handleCharacter(int primaryCode, int[] keyCodes) {
        if (isInputViewShown()) {
            if (mInputView.isShifted()) {
                primaryCode = Character.toUpperCase(primaryCode);
            }
        }

        if (primaryCode == 10){
            sendDefaultEditorAction(true);
            return;
        }

        if(primaryCode == NOT_A_KEY)
            return;
        getCurrentInputConnection().commitText(String.valueOf((char) primaryCode), 1);
    }

//    private void handleClose() {
////        commitTyped(getCurrentInputConnection());
//        requestHideSelf(0);
//        mInputView.closing();
//    }
    
    public void swipeRight() {
        Log.d(TAG, "swipeRight: ");
    }

    public void swipeLeft() {
        Log.d(TAG, "swipeLeft: ");
    }

    public void swipeDown() {
        Log.d(TAG, "swipeDown: ");
    }

    public void swipeUp() {
        Log.d(TAG, "swipeUp: ");
    }

    public void onPress(int primaryCode) {
        Log.d(TAG, "onPress: ");
    }

    public void onRelease(int primaryCode) {
        Log.d(TAG, "onRelease: ");
        vibrate(0);
    }

    public static int translateKeyToIndex(int keyCode){
        Log.i(TAG, "translateKeyToIndex: "+keyCode);
        int index = NOT_A_KEY; //used for bcksp alt etc, keys I dont wanna remap
        switch (keyCode){
            case KeyEvent.KEYCODE_Q://row 1
                index = 0;
                break;
            case KeyEvent.KEYCODE_W:
                index = 1;
                break;
            case KeyEvent.KEYCODE_E:
                index = 2;
                break;
            case KeyEvent.KEYCODE_R:
                index = 3;
                break;
            case KeyEvent.KEYCODE_T:
                index = 4;
                break;
            case KeyEvent.KEYCODE_Y:
                index = 5;
                break;
            case KeyEvent.KEYCODE_U:
                index = 6;
                break;
            case KeyEvent.KEYCODE_I:
                index = 7;
                break;
            case KeyEvent.KEYCODE_O:
                index = 8;
                break;
            case KeyEvent.KEYCODE_P:
                index = 9;
                break;
            case KeyEvent.KEYCODE_A://row 2
                index = 10;
                break;
            case KeyEvent.KEYCODE_S:
                index = 11;
                break;
            case KeyEvent.KEYCODE_D:
                index = 12;
                break;
            case KeyEvent.KEYCODE_F:
                index = 13;
                break;
            case KeyEvent.KEYCODE_G:
                index = 14;
                break;
            case KeyEvent.KEYCODE_H:
                index = 15;
                break;
            case KeyEvent.KEYCODE_J:
                index = 16;
                break;
            case KeyEvent.KEYCODE_K:
                index = 17;
                break;
            case KeyEvent.KEYCODE_L:
                index = 18;
                break;
//            case KeyEvent.KEYCODE_DEL:
//                index = 19;
//                break;
            case KeyEvent.KEYCODE_Z://row 3
                index = 20;
                break;
            case KeyEvent.KEYCODE_X:
                index = 21;
                break;
            case KeyEvent.KEYCODE_C:
                index = 22;
                break;
            case KeyEvent.KEYCODE_V:
                index = 23;
                break;
//            case KeyEvent.KEYCODE_SPACE:
//                index = 24;
//                break;
            case KeyEvent.KEYCODE_B:
                index = 25;
                break;
            case KeyEvent.KEYCODE_N:
                index = 26;
                break;
            case KeyEvent.KEYCODE_M:
                index = 27;
                break;
            case KeyEvent.KEYCODE_ENTER:
                index = 28;
                break;
        }
        return index;
    }

    private void vibrate(int type){
        switch (type){
            case 0://key feedback
                vibrationService.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
                break;
            case 1://notification single
                vibrationService.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
                break;
            case 2://notification double
                vibrationService.vibrate(VibrationEffect.createWaveform(new long[]{30L, 65L, 30L},new int[]{1,0,1},-1));
                break;

        }
    }
    private void cycleThroughKeyboardsLayers(){
        Keyboard current = mInputView.getKeyboard();
        if (current == mCurKeyboard) {
            mInputView.setKeyboard(mSymbolsKeyboard);
        } else if (current == mSymbolsKeyboard) {
            mInputView.setKeyboard(mSymbolsShiftedKeyboard);
        } else if (current == mSymbolsShiftedKeyboard) {
            mInputView.setKeyboard(mCurKeyboard);
        }
    }
}
