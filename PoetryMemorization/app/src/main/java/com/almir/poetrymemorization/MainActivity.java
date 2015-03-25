package com.almir.poetrymemorization;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognitionService;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class MainActivity extends ActionBarActivity {

    public static final int LISTENING_TIMEOUT = 3000;

    private Resources mRes;
    private SharedPreferences mPrefs;

    private SpeechRecognizer mSr;

    private TextView mTvPoem;
    private Button mButtonShowPoem;
    private Button mButtonStart;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRes = getResources();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mTvPoem = (TextView) findViewById(R.id.text_poem);
        mTvPoem.setMovementMethod(new ScrollingMovementMethod());
        mTvPoem.setText(R.string.poem);
        mButtonShowPoem = (Button) findViewById(R.id.button_show);
        mButtonStart = (Button) findViewById(R.id.button_start);

    }

    @Override
    public void onStart() {
        super.onStart();

        ComponentName serviceComponent = getServiceComponent();
        if (serviceComponent != null) {
            mSr = SpeechRecognizer.createSpeechRecognizer(this, serviceComponent);
            if (mSr != null) {
                prepareMemorization(mSr);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private Intent createRecognizerIntent(String verse) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getApplicationContext().getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, verse);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().getLanguage());
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        return intent;
    }

    private ComponentName getServiceComponent() {
        List<ResolveInfo> services = getPackageManager().queryIntentServices(
                new Intent(RecognitionService.SERVICE_INTERFACE), 0);
        if (services.isEmpty()) {
            return null;
        }
        ResolveInfo ri = services.iterator().next();
        String pkg = ri.serviceInfo.packageName;
        String cls = ri.serviceInfo.name;
        return new ComponentName(pkg, cls);
    }

    private void prepareMemorization(final SpeechRecognizer sr) {
        mButtonShowPoem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTvPoem.setText(R.string.poem);
            }
        });
        mButtonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTvPoem.setText("");

                listenVerse(sr, getString(R.string.poem).split("\n")[0]);
            }
        });
    }

    private void listenVerse(SpeechRecognizer sr, String verse) {
        startListening(sr, verse);
    }

    private void startListening(final SpeechRecognizer sr, String phrase) {
        Intent intentRecognizer = createRecognizerIntent(phrase);

        final Runnable stopListening = new Runnable() {
            @Override
            public void run() {
                sr.stopListening();
            }
        };
        final Handler handler = new Handler();

        sr.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                handler.postDelayed(stopListening, LISTENING_TIMEOUT);
            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float rmsdB) {

            }

            @Override
            public void onBufferReceived(byte[] buffer) {

            }

            @Override
            public void onEndOfSpeech() {
                handler.removeCallbacks(stopListening);
            }

            @Override
            public void onError(int error) {
                handler.removeCallbacks(stopListening);
                switch (error) {
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        listenVerse(sr, "");
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onResults(Bundle results) {
                handler.removeCallbacks(stopListening);
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (!matches.isEmpty()) {
                    String result = matches.iterator().next();
                    //show result
                    mTvPoem.setText(mTvPoem.getText()+"\n"+result);
                    listenVerse(sr, "");
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                String[] results = partialResults.getStringArray("com.google.android.voicesearch.UNSUPPORTED_PARTIAL_RESULTS");
                if (results != null) {
                    //show results
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {

            }
        });

        sr.startListening(intentRecognizer);
    }
}
