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

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import java.util.List;

/**
 * Example of writing an input method for a soft keyboard.  This code is
 * focused on simplicity over completeness, so it should in no way be considered
 * to be a complete soft keyboard implementation.  Its purpose is to provide
 * a basic example for how you would get started writing an input method, to
 * be fleshed out as appropriate.
 */
public class SoftKeyboard extends InputMethodService 
        implements KeyboardView.OnKeyboardActionListener {
//    static final boolean DEBUG = false;
    
    /**
     * This boolean indicates the optional example code for performing
     * processing of hard keys in addition to regular text generation
     * from on-screen interaction.  It would be used for input methods that
     * perform language translations (such as converting text entered on 
     * a QWERTY keyboard to Chinese), but may not be used for input methods
     * that are primarily intended to be used for on-screen text entry.
     */
//    static final boolean PROCESS_HARD_KEYS = true;
    private static final String TAG = "titan keyboard";
    private InputMethodManager mInputMethodManager;

    private LatinKeyboardView mInputView;
//    private CandidateView mCandidateView;
//    private CompletionInfo[] mCompletions;
    
//    private StringBuilder mComposing = new StringBuilder();

//    private boolean mCompletionOn;
    private int mLastDisplayWidth;

    private LatinKeyboard mSymbolsKeyboard;
    private LatinKeyboard mSymbolsShiftedKeyboard;
    private LatinKeyboard mQwertyKeyboard;
    private LatinKeyboard mNumericKeyboard;

    private LatinKeyboard mCurKeyboard;

//    private String mWordSeparators;

    private long lastShiftTime = 0L;
    private long lastAltTime = 0L;
    private boolean altLock = false;
    private boolean shiftLock = false;
    private boolean altShortcut = true;
    /**
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
    @Override public void onCreate() {
        Log.d(TAG, "onCreate: ");
        super.onCreate();
        mInputMethodManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
//        mWordSeparators = getResources().getString(R.string.word_separators);
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
        mInputView = (LatinKeyboardView) getLayoutInflater().inflate(
                R.layout.input, null);
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
//        mCandidateView = new CandidateView(this);
//        mCandidateView.setService(this);
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
        
        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
//        mComposing.setLength(0);
//        updateCandidates();

        if (!restarting) {
            // Clear shift states.
//            mMetaState = 0;
        }
        
//        mPredictionOn = false;
//        mCompletionOn = false;
//        mCompletions = null;
        
        // We are now going to initialize our state based on the type of
        // text being edited.
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
                int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
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
        mCurKeyboard.setImeOptions(getResources(), attribute.imeOptions);
    }

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override
    public void onFinishInput() {
        Log.d(TAG, "onFinishInput: ");
        super.onFinishInput();
        
        // Clear current composing text and candidates.
//        mComposing.setLength(0);
//        updateCandidates();
        
        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid popping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
//        setCandidatesViewShown(false);
        
        mCurKeyboard = mQwertyKeyboard;
        if (mInputView != null) {
            mInputView.closing();
        }
    }
    
    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        Log.d(TAG, "onStartInputView: ");
        super.onStartInputView(attribute, restarting);
        // Apply the selected keyboard to the input view.
        mInputView.setKeyboard(mCurKeyboard);
//        mInputView.closing();
        final InputMethodSubtype subtype = mInputMethodManager.getCurrentInputMethodSubtype();
        mInputView.setSubtypeOnSpaceKey(subtype);
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
    public void onUpdateSelection(int oldSelStart, int oldSelEnd,
            int newSelStart, int newSelEnd,
            int candidatesStart, int candidatesEnd) {
        Log.d(TAG, "onUpdateSelection: ");
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);

        // If the current selection in the text view changes, we should
        // clear whatever candidate text we have.
//        if (mComposing.length() > 0 && (newSelStart != candidatesEnd
//                || newSelEnd != candidatesEnd)) {
//            mComposing.setLength(0);
//            updateCandidates();
//            InputConnection ic = getCurrentInputConnection();
//            if (ic != null) {
//                ic.finishComposingText();
//            }
//        }
    }

    /**
     * This tells us about completions that the editor has determined based
     * on the current text in it.  We want to use this in fullscreen mode
     * to show the completions ourself, since the editor can not be seen
     * in that situation.
     */
//    @Override public void onDisplayCompletions(CompletionInfo[] completions) {
//        Log.d(TAG, "onDisplayCompletions: ");
//        if (mCompletionOn) {
//            mCompletions = completions;
//            if (completions == null) {
//                setSuggestions(null, false, false);
//                return;
//            }
//
//            List<String> stringList = new ArrayList<String>();
//            for (CompletionInfo ci : completions) {
//                if (ci != null) stringList.add(ci.getText().toString());
//            }
//            setSuggestions(stringList, true, true);
//        }
//    }
    
    /**
     * This translates incoming hard key events in to edit operations on an
     * InputConnection.  It is only needed when using the
     * PROCESS_HARD_KEYS option.
     */
//    private boolean translateKeyDown(int keyCode, KeyEvent event) {
//        mMetaState = MetaKeyKeyListener.handleKeyDown(mMetaState,
//                keyCode, event);
//        int c = event.getUnicodeChar(MetaKeyKeyListener.getMetaState(mMetaState));
//        mMetaState = MetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
//        InputConnection ic = getCurrentInputConnection();
//        if (c == 0 || ic == null) {
//            return false;
//        }
//
//        boolean dead = false;
//
//        if ((c & KeyCharacterMap.COMBINING_ACCENT) != 0) {
//            dead = true;
//            c = c & KeyCharacterMap.COMBINING_ACCENT_MASK;
//        }
//
//        if (mComposing.length() > 0) {
//            char accent = mComposing.charAt(mComposing.length() -1 );
//            int composed = KeyEvent.getDeadChar(accent, c);
//
//            if (composed != 0) {
//                c = composed;
//                mComposing.setLength(mComposing.length()-1);
//            }
//        }
//
//        onKey(c, null);
//
//        return true;
//    }
    
    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //note: this method is a bit spammy due to key repetition
        Log.d(TAG, "onKeyDown: "+keyCode);

        if((altLock && keyCode == KeyEvent.KEYCODE_DEL) || mCurKeyboard == mNumericKeyboard){ //dont sent alt+backspace with alt lock since it acts like ctrl backspace, its annoying
            keyDownUp(KeyEvent.KEYCODE_DEL);
            return true;
        }

        Keyboard current = mInputView.getKeyboard();
        if (current == mSymbolsKeyboard || mCurKeyboard == mSymbolsShiftedKeyboard) {
            try {
                int pressedKey = translateKeyToIndex(keyCode);
                if(pressedKey != -1 ){
                    getCurrentInputConnection().commitText(
                            String.valueOf((char) current.getKeys().get(pressedKey).codes[0]), 1);
                    return true;
                }
            }catch (Exception e){
                Log.e(TAG, "onKeyDown: ", e);
            }
        }

//        switch (keyCode) {
//            case KeyEvent.META_CTRL_ON: //dont got one of these lol
//                    break;

//            case KeyEvent.KEYCODE_BACK:
//                // The InputMethodService already takes care of the back
//                // key for us, to dismiss the input method if it is shown.
//                // However, our keyboard could be showing a pop-up window
//                // that back should dismiss, so we first allow it to do that.
//                if (event.getRepeatCount() == 0 && mInputView != null) {
//                    if (mInputView.handleBack()) {
//                        return true;
//                    }
//                }
//                break;
                
//            case KeyEvent.KEYCODE_DEL: //dont got one of these either
//                // Special handling of the delete key: if we currently are
//                // composing text for the user, we want to modify that instead
//                // of let the application to the delete itself.
//                if (mComposing.length() > 0) {
//                    onKey(Keyboard.KEYCODE_DELETE, null);
//                    return true;
//                }
//                break;
                
//            case KeyEvent.KEYCODE_ENTER:
//                // Let the underlying text editor always handle these.
//                return false;
                
//            default:
                // For all other keys, if we want to do transformations on
                // text being entered with a hard keyboard, we need to process
                // it and do the appropriate action.

//        }
        
        return super.onKeyDown(keyCode, event);
//        return false;
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

        Log.d(TAG, "onKeyUp: "+keyCode);
        Keyboard current = mInputView.getKeyboard();
        if(current == mQwertyKeyboard){
            if(keyCode == KeyEvent.KEYCODE_ALT_RIGHT){
                if(altShortcut){//key up was called after an alt shortcut ie alt + shift, alt + space
                    altShortcut = false;
                }else{
                    if(altLock){
                        Log.d(TAG, "onKeyUp: alt lock off");
                        altLock = false;
                        getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ALT_RIGHT));
                        return true;
                    }else {//check for double tap
                        if((System.currentTimeMillis() - lastAltTime) < 600L){
                            Log.d(TAG, "onKeyUp: alt lock on");
                            getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ALT_RIGHT));
                            altLock = true;
                            return true;
                        }else{
                            lastAltTime = System.currentTimeMillis();
                        }
                    }
                }
            }


            if(keyCode == KeyEvent.KEYCODE_SHIFT_LEFT){
                if(shiftLock){
                    Log.d(TAG, "onKeyUp: shift lock off");
                    shiftLock = false;
                    getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_LEFT));
                    return true;
                }else {//check for double tap
                    if((System.currentTimeMillis() - lastShiftTime) < 600L){
                        Log.d(TAG, "onKeyUp: shift lock on");
                        getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT));
                        shiftLock = true;
                        return true;
                    }else{
                        lastShiftTime = System.currentTimeMillis();
                    }
                }
            }
        } else if (current == mNumericKeyboard) {

        } else if (current == mSymbolsKeyboard || mCurKeyboard == mSymbolsShiftedKeyboard) {
//            int pressedIndex = translateKeyToIndex(keyCode);
//            List<Keyboard.Key> list = current.getKeys();
//            int c = list.get(pressedIndex).codes[0];
        }

        if (event.isAltPressed()) { //bindings for alt key
            if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT) { //alt+space
                //cycle through sym layers
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    if (current == mCurKeyboard) {
                        mInputView.setKeyboard(mSymbolsKeyboard);
                    } else if (current == mSymbolsKeyboard) {
                        mInputView.setKeyboard(mSymbolsShiftedKeyboard);
                    } else if (current == mSymbolsShiftedKeyboard) {
                        mInputView.setKeyboard(mCurKeyboard);
                    }
                    ic.clearMetaKeyStates(KeyEvent.META_ALT_ON); //clear alt so it does mess up the next char

                }
                altShortcut = true;
                return true;
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    /**
     * Helper function to commit any text being composed in to the editor.
     */
//    private void commitTyped(InputConnection inputConnection) {
//        if (mComposing.length() > 0) {
//            inputConnection.commitText(mComposing, mComposing.length());
//            mComposing.setLength(0);
//            updateCandidates();
//        }
//    }

    /**
     * Helper to update the shift state of our keyboard based on the initial
     * editor state.
     */
    private void updateShiftKeyState(EditorInfo attr) {
//        if (attr != null
//                && mInputView != null && mQwertyKeyboard == mInputView.getKeyboard()) {
//            int caps = 0;
//            EditorInfo ei = getCurrentInputEditorInfo();
//            if (ei != null && ei.inputType != InputType.TYPE_NULL) {
//                caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
//            }
//            mInputView.setShifted(mCapsLock || caps != 0);
//        }
    }
    
    /**
     * Helper to determine if a given character code is alphabetic.
     */
//    private boolean isAlphabet(int code) {
//        return Character.isLetter(code);
//    }
    
    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode) {
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }
    
    /**
     * Helper to send a character to the editor as raw key events.
     */
//    private void sendKey(int keyCode) {
//        switch (keyCode) {
//            case '\n':
//                keyDownUp(KeyEvent.KEYCODE_ENTER);
//                break;
//            default:
//                if (keyCode >= '0' && keyCode <= '9') {
//                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
//                } else {
//                    getCurrentInputConnection().commitText(String.valueOf((char) keyCode), 1);
//                }
//                break;
//        }
//    }

    // Implementation of KeyboardViewListener
    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        Log.d(TAG, "onKey: "+primaryCode);
//        if (isWordSeparator(primaryCode)) {
//            // Handle separator
//            if (mComposing.length() > 0) {
//                commitTyped(getCurrentInputConnection());
//            }
//            sendKey(primaryCode);
//            updateShiftKeyState(getCurrentInputEditorInfo());
//        } else
//        if (primaryCode == Keyboard.KEYCODE_DELETE) {
////            handleBackspace();
//        } else
        if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            handleShift();
        } else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
            handleClose();
            return;
        }
//        else if (primaryCode == LatinKeyboardView.KEYCODE_OPTIONS) {
//            // Show a menu or somethin'
//        }
        else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE
                && mInputView != null) {
            Keyboard current = mInputView.getKeyboard();
            if (current == mSymbolsKeyboard || current == mSymbolsShiftedKeyboard) {
                current = mQwertyKeyboard;
            } else {
                current = mSymbolsKeyboard;
            }
            mInputView.setKeyboard(current);
            if (current == mSymbolsKeyboard) {
                current.setShifted(false);
            }
        } else if(primaryCode == 1){
            //left
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT);
        } else if(primaryCode == 2){
            //right
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT);
        }
        else if (primaryCode == 4096){
            Log.d("keeyb", "onKey: ");
            Keyboard current = mInputView.getKeyboard();
            List<Keyboard.Key> mods = current.getModifierKeys();
            Log.d("keeyb", "onKey: ");
        }
        else {
            handleCharacter(primaryCode, keyCodes);
        }
    }

    @Override
    public void onText(CharSequence text) {
//        InputConnection ic = getCurrentInputConnection();
//        if (ic == null) return;
//        ic.beginBatchEdit();
//        if (mComposing.length() > 0) {
//            commitTyped(ic);
//        }
//        ic.commitText(text, 0);
//        ic.endBatchEdit();
//        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    /**
     * Update the list of available candidates from the current composing
     * text.  This will need to be filled in by however you are determining
     * candidates.
     */
//    private void updateCandidates() {
//        if (!mCompletionOn) {
//            if (mComposing.length() > 0) {
//                ArrayList<String> list = new ArrayList<String>();
//                list.add(mComposing.toString());
//                setSuggestions(list, true, true);
//            } else {
//                setSuggestions(null, false, false);
//            }
//        }
//    }
    
//    public void setSuggestions(List<String> suggestions, boolean completions,
//            boolean typedWordValid) {
//        if (suggestions != null && suggestions.size() > 0) {
//            setCandidatesViewShown(true);
//        } else if (isExtractViewShown()) {
//            setCandidatesViewShown(true);
//        }
//        if (mCandidateView != null) {
//            mCandidateView.setSuggestions(suggestions, completions, typedWordValid);
//        }
//    }
    
//    private void handleBackspace() {
//        final int length = mComposing.length();
//        if (length > 1) {
//            mComposing.delete(length - 1, length);
//            getCurrentInputConnection().setComposingText(mComposing, 1);
//            updateCandidates();
//        } else if (length > 0) {
//            mComposing.setLength(0);
//            getCurrentInputConnection().commitText("", 0);
//            updateCandidates();
//        } else {
//            keyDownUp(KeyEvent.KEYCODE_DEL);
//        }
//        updateShiftKeyState(getCurrentInputEditorInfo());
//    }

    private void handleShift() {
        if (mInputView == null) {
            return;
        }
        
        Keyboard currentKeyboard = mInputView.getKeyboard();
//        if (mQwertyKeyboard == currentKeyboard) {
//            // Alphabet keyboard
//            checkToggleCapsLock();
//            mInputView.setShifted(mCapsLock || !mInputView.isShifted());
//        } else
        if (currentKeyboard == mSymbolsKeyboard) {
            mSymbolsKeyboard.setShifted(true);
            mInputView.setKeyboard(mSymbolsShiftedKeyboard);
            mSymbolsShiftedKeyboard.setShifted(true);
        } else if (currentKeyboard == mSymbolsShiftedKeyboard) {
            mSymbolsShiftedKeyboard.setShifted(false);
            mInputView.setKeyboard(mSymbolsKeyboard);
            mSymbolsKeyboard.setShifted(false);
        }
    }
    
    private void handleCharacter(int primaryCode, int[] keyCodes) {
        if (isInputViewShown()) {
            if (mInputView.isShifted()) {
                primaryCode = Character.toUpperCase(primaryCode);
            }
        }
//        if (isAlphabet(primaryCode) && mPredictionOn) {
//            mComposing.append((char) primaryCode);
//            getCurrentInputConnection().setComposingText(mComposing, 1);
//            updateShiftKeyState(getCurrentInputEditorInfo());
//            updateCandidates();
//        } else {
//            getCurrentInputConnection().commitText(
//                    String.valueOf((char) primaryCode), 1);
//        }
        getCurrentInputConnection().commitText(
                String.valueOf((char) primaryCode), 1);
    }

    private void handleClose() {
//        commitTyped(getCurrentInputConnection());
        requestHideSelf(0);
        mInputView.closing();
    }

//    private String getWordSeparators() {
//        return mWordSeparators;
//    }
    
//    public boolean isWordSeparator(int code) {
//        String separators = getWordSeparators();
//        return separators.contains(String.valueOf((char)code));
//    }

//    public void pickDefaultCandidate() {
//        pickSuggestionManually(0);
//    }
    
    public void pickSuggestionManually(int index) {
//        if (mCompletionOn && mCompletions != null && index >= 0
//                && index < mCompletions.length) {
//            CompletionInfo ci = mCompletions[index];
//            getCurrentInputConnection().commitCompletion(ci);
//            if (mCandidateView != null) {
//                mCandidateView.clear();
//            }
//            updateShiftKeyState(getCurrentInputEditorInfo());
//        } else if (mComposing.length() > 0) {
//            // If we were generating candidate suggestions for the current
//            // text, we would commit one of them here.  But for this sample,
//            // we will just commit the current text.
//            commitTyped(getCurrentInputConnection());
//        }
    }
    
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
    }

    public int translateKeyToIndex(int keyCode){
        Log.i(TAG, "translateKeyToIndex: "+keyCode);
        int index = -1; //used for bcksp alt etc, keys I dont wanna remap
        switch (keyCode){
            case KeyEvent.KEYCODE_Q://row 3
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
            case KeyEvent.KEYCODE_B:
                index = 24;
                break;
            case KeyEvent.KEYCODE_N:
                index = 25;
                break;
            case KeyEvent.KEYCODE_M:
                index = 26;
                break;
//            case KeyEvent.KEYCODE_ENTER:
//                index = 27;
//                break;
        }
        return index;
    }

}
