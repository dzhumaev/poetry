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
import android.text.Html;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends ActionBarActivity {

    private static int listenerCounter = 0;

    private class MatchingPoem {
        private final SpeechRecognizer speechRecognizer;
        private String[] lines;
        private String lastGuess;
        private int currentLine;
        private int numMatchedLines;
        private int numGuessedLines;
        private boolean[] isLineGuessed;
        boolean isRunning;

        public MatchingPoem(SpeechRecognizer speechRecognizer, String poem) {
            this.speechRecognizer = speechRecognizer;
            this.lines = poem.split("\\n+");
            resetMatch();
        }

        public void resetMatch() {
            currentLine = 0;
            numMatchedLines = 0;
            numGuessedLines = 0;
            isLineGuessed = new boolean[lines.length];
        }

        public boolean currentLineMatches(List<String> guesses) {
            double minScore = 2;
            for (String guess : guesses) {
                double currentScore = similarityScore(guess, lines[currentLine]);
                if (currentScore < minScore) {
                    minScore = currentScore;
                    lastGuess = guess;
                }
            }
            Log.i("MIN_SCORE", Double.toString(minScore));
            return minScore < 0.45;
        }

        String cleanWord(String dirtyWord) {
            return dirtyWord.replaceAll("[^a-zA-Z']", "").toLowerCase();
        }

        String markUnrecognized(String word) {
            return "-" + word + "-";
        }

        private String attachConfidenceMarkupToReference(String reference, String guess) {
            String[] referenceWords = reference.split("\\s+");
            String[] guessWords = guess.split("\\s+");
            String[] cleanedReferenceWords = new String[referenceWords.length];
            for (int i = 0; i < referenceWords.length; ++i) {
                cleanedReferenceWords[i] = cleanWord(referenceWords[i]);
            }
            boolean[] unrecognizedWords = LevenshteinDistance.showUnrecognizedWords(
                    cleanedReferenceWords, guessWords);

            for (int i = 0; i < referenceWords.length; ++i) {
                if (unrecognizedWords[i]) {
                    referenceWords[i] = markUnrecognized(referenceWords[i]);
                }
            }

            String referenceWithMarkup = Joiner.on(" ").join(referenceWords);
            Log.i("REF_W_MARKUP", referenceWithMarkup);

            return referenceWithMarkup;
        }

        public void markLineSuccess() {
            // TODO: show in UI

            numGuessedLines++;

           // String referenceWithMarkup = attachConfidenceMarkupToReference(
           //         lines[currentLine], lastGuess);

//            mTvPoem.setText(mTvPoem.getText() + "\n" + "+ " + referenceWithMarkup);
            isLineGuessed[currentLine] = true;
            checkIfContinueMatching();
        }

        public void markLineFail() {
            // TODO: show in UI

            mTvPoem.setText(mTvPoem.getText() + "\n" + "- " + lines[currentLine]);

//            pronounceLine(lines[currentLine]);
            isLineGuessed[currentLine] = false;
            checkIfContinueMatching();
        }

        public void speechTimeout() {

        }

        public void checkIfContinueMatching() {

            mTvScore.setText(""+numGuessedLines);

            currentLine++;
            showPoem();

            if (currentLine < lines.length) {
                continueMatching();
            } else {
                endMatching();
            }
        }

        public void showPoem() {
            mTvPoem.setText(generateHtml());
        }

        public void endMatching() {
            // TODO: implement

            mTvPoem.setText(mTvPoem.getText() + "\n" + numMatchedLines + "/" + lines.length);
        }

        private double similarityScore(String guess, String reference) {
            double score = LevenshteinDistance.computeLevenshteinDistance(guess, reference)
                    / (guess.length() + reference.length() / 2.);

            Log.d("REFERENCE", reference);
            Log.d("GUESS", guess);
            Log.d("LEVEN_SENTENCES_NORM", Double.toString(score));

            return score;
        }

        public void pauseMatching() {
            isRunning = false;
        }

        public void continueMatching() {
            isRunning = true;
            createListenerForNewLine(speechRecognizer);
        }

        private String getHtmlForGuessedPart() {
            String html = "";
            for (int i = 0; i < currentLine; i++) {
                if (isLineGuessed[i]) {
                    html = html + "<br><font color=\"#347C17\">" + lines[i] + "</font></br>\n";
                } else {
                    html = html + "<br><font color=\"#9F000F\">" + lines[i] + "</font></br>\n";
                }
            }
            return html;
        }

        private String getHtmlForEnding() {
            String html = "";
            for (int i = currentLine; i < lines.length; i++) {
                html = html + "<br><font color=\"gray\">"+lines[i]+"</font></br>\n";
            }
            return html;
        }

        private Spanned generateHtml() {
            return Html.fromHtml(getHtmlForGuessedPart());
        }

        public void showPoemWithEnding() {
            mTvPoem.setText(Html.fromHtml(getHtmlForGuessedPart() + getHtmlForEnding()));
        }
    }

    MatchingPoem matchingPoem;

    public static final int LISTENING_TIMEOUT = 3000;

    private Resources mRes;
    private SharedPreferences mPrefs;

    private SpeechRecognizer mSr;

    private TextView mTvPoem;
    private TextView mTvScore;
    private Button mButtonReset;
    private Button mButtonStart;
    private boolean isPressed = true;
    private int score = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRes = getResources();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mTvPoem = (TextView) findViewById(R.id.text_poem);
        mTvPoem.setMovementMethod(new ScrollingMovementMethod());
        mTvPoem.setText(R.string.poem);
        mTvScore = (TextView) findViewById(R.id.score);
        mTvScore.setText(""+score);
        mButtonReset = (Button) findViewById(R.id.button_reset);
        mButtonStart = (Button) findViewById(R.id.button_start);
    }

    @Override
    public void onStart() {
        super.onStart();

        ComponentName serviceComponent = getServiceComponent();
        if (serviceComponent != null) {
            mSr = SpeechRecognizer.createSpeechRecognizer(this, serviceComponent);
            matchingPoem = new MatchingPoem(mSr, mTvPoem.getText().toString());
        }
        matchingPoem.showPoemWithEnding();
        prepareMemorization();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
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

    private Intent createRecognizerIntent() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getApplicationContext().getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
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

    private void prepareMemorization() {
        mButtonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                score = 0;
                mTvScore.setText(""+score);
                matchingPoem.resetMatch();
                matchingPoem.showPoemWithEnding();
                isPressed = true;
                mButtonStart.setText(R.string.button_start);
            }
        });
        mButtonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("BUTTON_START", "click");
                if (isPressed) {
                    isPressed = false;
                    mButtonStart.setText(R.string.button_show);
                    matchingPoem.showPoem();
                    matchingPoem.continueMatching();
                } else {
                    isPressed = true;
                    mButtonStart.setText(R.string.button_start);
                    matchingPoem.pauseMatching();
                    matchingPoem.showPoemWithEnding();
//                    mTvPoem.setText(generateHtml(poem.length));
                }
            }
        });
    }

    private void createListenerForNewLine(final SpeechRecognizer sr) {
        Log.i("CREATE_LISTENER", "");
        Intent intentRecognizer = createRecognizerIntent();

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
                        matchingPoem.speechTimeout();
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onResults(Bundle results) {
                handler.removeCallbacks(stopListening);
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                if (matchingPoem.isRunning) {
                    if (matchingPoem.currentLineMatches(matches)) {
                        matchingPoem.markLineSuccess();
                    } else {
                        matchingPoem.markLineFail();
                    }
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {

            }

            @Override
            public void onEvent(int eventType, Bundle params) {

            }
        });

        sr.startListening(intentRecognizer);
    }

}
