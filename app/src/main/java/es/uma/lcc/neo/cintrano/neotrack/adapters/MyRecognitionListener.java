package es.uma.lcc.neo.cintrano.neotrack.adapters;

import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.text.Normalizer;
import java.util.List;

import es.uma.lcc.neo.cintrano.neotrack.TrackActivity;

/**
 * Created by Christian Cintrano on 14/03/16.
 * Listener to recognition speech to the device
 */
public class MyRecognitionListener implements RecognitionListener {

    private final TrackActivity context;

    public MyRecognitionListener(TrackActivity context) {
        this.context = context;
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d("Speech", "onBeginningOfSpeech");
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.d("Speech", "onBufferReceived");
    }

    @Override
    public void onEndOfSpeech() {
        Log.d("Speech", "onEndOfSpeech");
    }

    @Override
    public synchronized void onError(int error) {
        String text = "";
        // Translate Android SpeechRecognizer errors to Web Speech API errors.
        switch(error) {
            case SpeechRecognizer.ERROR_AUDIO:
                text = "ERROR_AUDIO";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                text = "ERROR_CLIENT";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                text = "ERROR_RECOGNIZER_BUSY";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_SERVER:
                text = "ERROR_NETWORK";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                text = "ERROR_N O_MATCH";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                text = "ERROR_SPEECH_TIMEOUT";
                context.speakerOut.speak("Motivo no introducido", TextToSpeech.QUEUE_ADD, null);
                // Save input
                Location location = context.locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                context.myLocationChanged(location, "STOP");
                break;
        }
        Log.d("Speech", "onError " + text);
//        recognizeSpeechDi/rectly();
        context.runningSpeech = false;
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        Log.d("Speech", "onEvent");
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        Log.d("Speech", "onPartialResults");
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.d("Speech", "onReadyForSpeech");
    }


    @Override
    public void onResults(Bundle results) {
        Log.d("Speech", "onResults");
        List<String> stringList = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        for (int i = 0; i < (stringList != null ? stringList.size() : 0); i++ ) {
            Log.d("Speech", "result=" + stringList.get(i));
        }

//        context.sr.startListening(RecognizerIntent.getVoiceDetailsIntent(context.getApplicationContext()));
        if (stringList != null) {
            context.speakerOut.speak("Ha introducido " + getStopType(stringList), TextToSpeech.QUEUE_ADD, null);
        }
        context.speeching = false;
//        context.newSpeech = true;
        context.runningSpeech = false;
        // Save input
        Location location = context.locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        context.myLocationChanged(location, "STOP");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
//        Log.d("Speech", "onRmsChanged");
    }

    private String getStopType(List<String> result) {
        final String[] stopChoices = {"Atasco", "Obras", "Accidente", "Otros", "Reanudar", "Rojo"};
        final String[] stopChoicesPattern = {"asco", "bra", "ente", "tro", "anudar", "ojo"};

        for(String str : result) {
            System.out.println(str);
            str = Normalizer.normalize(str, Normalizer.Form.NFD);
            // remove accents
            str = str.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
            for(int i = 0; i<stopChoicesPattern.length;i++) {
                if(str.toLowerCase().contains(stopChoicesPattern[i])) {
                    return stopChoices[i];
                }
            }
        }
        return null;
    }

}

