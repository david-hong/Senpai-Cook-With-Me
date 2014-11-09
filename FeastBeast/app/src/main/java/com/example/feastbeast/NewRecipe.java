package com.example.feastbeast;

import android.app.Fragment;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.speech.tts.TextToSpeech;

import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import ai.wit.sdk.IWitListener;
import ai.wit.sdk.Wit;
import ai.wit.sdk.model.WitOutcome;
import com.example.feastbeast.MainActivity.Directions;
import java.util.ArrayList;
import java.util.List;


public class NewRecipe extends ActionBarActivity implements IWitListener, TextToSpeech.OnInitListener {

    Wit _wit;
    private TextToSpeech tts;
    private Button btnSpeak;
    protected Directions directions;
    List list = new ArrayList();
    int indice = 0;
    double d = 0.7;
    float f = (float) d;
    int indice = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_recipe);
        String accessToken = "U55JDKGFF6CYR3XV64RJTTCX3OQZKL57";
        _wit = new Wit(accessToken, this);
        //_wit.enableContextLocation(getApplicationContext());
        tts = new TextToSpeech(this, this);
        tts.setSpeechRate(f);
        //btnSpeak = (Button) findViewById(R.id.butnSpeak);

        Intent intent = getIntent();
        String name = "item0";
        int count = 0;
        String temp = intent.getExtras().getString(name);
        if (temp == null){
            list.add("Senpai you did not enter a recipe");
            speakOut(0);
            list.remove(0);
        }

        while (temp != null) {
            list.add(temp);
            count++;
            name = "item" + count;
            temp = intent.getExtras().getString(name);
        }

        // button on click event
        /*btnSpeak.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                speakOut(indice);
            }

        });*/
    }

    @Override
    public void onDestroy() {
        // Don't forget to shutdown tts!
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    public void next_step()
    {
        if (indice + 1 < this.list.size()) {
            indice++;
            speakOut(indice);
        }
    }
    public void prev_step()
    {
        if (indice != 0 && indice < this.list.size()) {
            indice--;
            speakOut(indice);
        }
    }

    public void repeat_step()
    {
        if (indice < this.list.size())
            speakOut(indice);
    }

    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {

            int result = tts.setLanguage(Locale.US);

            /*if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            } else {
                btnSpeak.setEnabled(true);
                speakOut(indice);
            }*/

        } else {
            Log.e("TTS", "Initilization Failed!");
        }
        speakOut(0);

    }

    private void speakOut(int i) {
        if(list.size() != 0)
            tts.speak((String) list.get(i), TextToSpeech.QUEUE_FLUSH, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_recipe, menu);
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

    public void toggle(View v) {
        try {
            _wit.toggleListening();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void witDidGraspIntent(ArrayList<WitOutcome> witOutcomes, String messageId, Error error) {
        TextView jsonView = (TextView) findViewById(R.id.jsonView);
        jsonView.setMovementMethod(new ScrollingMovementMethod());
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        if (error != null) {
            jsonView.setText(error.getLocalizedMessage());
            return ;
        }
        String jsonOutput = gson.toJson(witOutcomes);

        if (jsonOutput.indexOf("next_step") != -1)
            next_step();
        else if (jsonOutput.indexOf("prev_step") != -1)
            prev_step();

        jsonView.setText(jsonOutput);

        if (jsonOutput.indexOf("next_step") != -1)
            next_step();
        else if (jsonOutput.indexOf("prev_step") != -1)
            prev_step();
        else if (jsonOutput.indexOf("repeat_step") != -1)
            repeat_step();

        ((TextView) findViewById(R.id.txtText)).setText("Done!");
    }

    @Override
    public void witDidStartListening() {
        ((TextView) findViewById(R.id.txtText)).setText("Witting...");
    }

    @Override
    public void witDidStopListening() {
        ((TextView) findViewById(R.id.txtText)).setText("Processing...");
    }

    @Override
    public void witActivityDetectorStarted() {
        ((TextView) findViewById(R.id.txtText)).setText("Listening");
    }

    @Override
    public String witGenerateMessageId() {
        return null;
    }

    public static class PlaceholderFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            return inflater.inflate(R.layout.wit_button, container, false);
        }
    }
    public void next_step()
    {
        indice++;
    }
    public void prev_step()
    {
        if (indice != 0)
            indice--;
    }
}
