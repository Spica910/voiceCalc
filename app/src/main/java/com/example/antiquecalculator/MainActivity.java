package com.example.antiquecalculator;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast; // Ensure Toast is imported

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView displayTextView;
    private StringBuilder currentInput;
    private Double operand1 = null;
    private String pendingOperation = null;
    private DecimalFormat decimalFormat;

    private static final int SPEECH_REQUEST_CODE = 123;
    private ImageButton voiceInputButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        displayTextView = findViewById(R.id.displayTextView);
        currentInput = new StringBuilder();
        decimalFormat = new DecimalFormat("#.##########");

        updateDisplay("0");

        voiceInputButton = findViewById(R.id.voiceInputButton);
        if (voiceInputButton != null) {
            voiceInputButton.setOnClickListener(view -> startVoiceRecognition());
        }
    }

    private void updateDisplay(String text) {
        displayTextView.setText(text);
    }

    private void appendNumber(String number) {
        if (currentInput.toString().equals("0") && !number.equals(".")) {
            currentInput.setLength(0);
        }
        if (number.equals(".") && currentInput.toString().contains(".")) {
            return;
        }
        currentInput.append(number);
        updateDisplay(currentInput.toString());
    }

    private void chooseOperation(String operation) {
        if (currentInput.length() > 0) {
            try {
                operand1 = Double.parseDouble(currentInput.toString());
                currentInput.setLength(0);
                pendingOperation = operation;
                updateDisplay(decimalFormat.format(operand1) + " " + operation);
            } catch (NumberFormatException e) {
                clearInputOnError("Error: Invalid number"); // More specific error
            }
        } else if (operand1 != null) {
            pendingOperation = operation;
            updateDisplay(decimalFormat.format(operand1) + " " + operation);
        }
    }

    private void calculateResult() {
        // Performs the calculation based on operand1, pendingOperation, and currentInput (as operand2).
        // The result is then stored in currentInput and displayed, and operand1 is updated for chained calculations.
        if (operand1 == null || pendingOperation == null || currentInput.length() == 0) {
            return;
        }

        double operand2;
        try {
            operand2 = Double.parseDouble(currentInput.toString());
        } catch (NumberFormatException e) {
            clearInputOnError("Error: Invalid number");
            return;
        }

        double result = 0;
        boolean error = false;

        switch (pendingOperation) {
            case "+":
                result = operand1 + operand2;
                break;
            case "-":
                result = operand1 - operand2;
                break;
            case "*":
                result = operand1 * operand2;
                break;
            case "/":
                if (operand2 == 0) {
                    clearInputOnError("Error: Cannot divide by zero"); // User-friendly message
                    error = true;
                } else {
                    result = operand1 / operand2;
                }
                break;
        }

        if (!error) {
            currentInput.setLength(0);
            if (result == (long) result) {
                currentInput.append((long) result);
            } else {
                currentInput.append(decimalFormat.format(result));
            }
            updateDisplay(currentInput.toString());
            operand1 = result;
            pendingOperation = null;
        }
    }

    private void clearInputOnError(String errorMessage) {
        updateDisplay(errorMessage);
        // currentInput.setLength(0); // Keep error message on display until next user action
        // operand1 = null;           // Let user decide to clear or continue
        // pendingOperation = null;
    }

    private void clearInput() {
        currentInput.setLength(0);
        operand1 = null;
        pendingOperation = null;
        updateDisplay("0");
    }

    // --- Voice Input Methods ---
    public void onVoiceInputClick(View view) {
        startVoiceRecognition();
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a number or command");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        try {
            Toast.makeText(this, "Listening...", Toast.LENGTH_SHORT).show(); // Feedback for user
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Speech recognition not available on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0).toLowerCase().trim();
                Toast.makeText(this, "Heard: " + spokenText, Toast.LENGTH_SHORT).show();
                processSpokenText(spokenText);
            } else {
                // Feedback if speech was recognized but results are empty
                Toast.makeText(this, "Didn't catch that. Please try again.", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == SPEECH_REQUEST_CODE) {
            // Feedback if recognition wasn't successful (e.g. cancelled, network error)
             Toast.makeText(this, "Speech recognition failed. Please try again.", Toast.LENGTH_LONG).show();
        }
    }

    private void processSpokenText(String spokenText) {
        // This method processes the spoken text word by word.
        // It tries to map words to numbers or calculator commands.
        // More sophisticated parsing (e.g., for phrases like "two point five") could be added.
        String[] words = spokenText.split("\\s+");

        for (String word : words) {
            // Normalize common number words
            String processedWord = word;
            if (word.equals("for")) processedWord = "four"; // common misrecognition for "four"

            switch (processedWord) {
                case "zero": appendNumber("0"); break;
                case "one": appendNumber("1"); break;
                case "two": case "to": appendNumber("2"); break; // "to" for "two"
                case "three": appendNumber("3"); break;
                case "four": appendNumber("4"); break;
                case "five": appendNumber("5"); break;
                case "six": appendNumber("6"); break;
                case "seven": appendNumber("7"); break;
                case "eight": appendNumber("8"); break;
                case "nine": appendNumber("9"); break;
                case "plus": case "add": chooseOperation("+"); break;
                case "minus": case "subtract": chooseOperation("-"); break;
                case "times": case "multiply": case "multiplied": chooseOperation("*"); break;
                case "divide": case "divided":
                    chooseOperation("/");
                    break;
                case "by":
                    if (pendingOperation != null && pendingOperation.equals("/")) {
                        // part of "divided by", already handled by "divide"
                    } else {
                         // could be an error or part of another phrase
                    }
                    break;
                case "equals": case "equal": case "result": calculateResult(); break;
                case "clear": case "reset": clearInput(); break;
                case "point": case "decimal": appendNumber("."); break;
                default:
                    try {
                        Double.parseDouble(processedWord);
                        appendNumber(processedWord);
                    } catch (NumberFormatException e) {
                        // Not a recognized command or a direct number.
                    }
                    break;
            }
        }
    }

    // --- Button Click Handlers ---
    public void onNumberClick(View view) {
        Button button = (Button) view;
        appendNumber(button.getText().toString());
    }

    public void onOperationClick(View view) {
        Button button = (Button) view;
        String operation = button.getText().toString();
        chooseOperation(operation);
    }

    public void onEqualsClick(View view) {
        calculateResult();
        pendingOperation = null;
    }

    public void onClearClick(View view) {
        clearInput();
    }

    public void onDecimalClick(View view) {
        appendNumber(".");
    }

    public void onPlusMinusClick(View view) {
        if (currentInput.length() > 0 && !currentInput.toString().equals("0")) {
            try {
                double currentValue = Double.parseDouble(currentInput.toString());
                currentValue *= -1;
                currentInput.setLength(0);
                currentInput.append(decimalFormat.format(currentValue));
                updateDisplay(currentInput.toString());
            } catch (NumberFormatException e) {
                // Should not happen
            }
        }
    }

    public void onPercentClick(View view) {
        if (currentInput.length() > 0) {
            try {
                double currentValue = Double.parseDouble(currentInput.toString());
                currentValue /= 100.0;
                currentInput.setLength(0);
                currentInput.append(decimalFormat.format(currentValue));
                updateDisplay(currentInput.toString());
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
    }
}
