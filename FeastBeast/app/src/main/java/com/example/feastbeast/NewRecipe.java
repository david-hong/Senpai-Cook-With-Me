package com.example.feastbeast;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import android.widget.Toast;

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
import java.util.Arrays;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.scanner.ScanActivity;
import com.thalmic.myo.Quaternion;

import android.media.AudioManager;
import com.google.gson.Gson;


public class NewRecipe extends ActionBarActivity implements IWitListener, TextToSpeech.OnInitListener {

    Wit _wit;
    private TextToSpeech tts;

    String recipeName;
    List<String> list = new ArrayList<String>();
    StringBuffer recipeStr = new StringBuffer();
    protected List<String> ingredients = new ArrayList<String>();
    StringBuffer ingredientsStr = new StringBuffer();
    public List<Recipe> recipes = new ArrayList<Recipe>();
    List<String> titles = new ArrayList<String>();

    double d = 0.7;
    float f = (float) d;
    int indice = 0;
    boolean recipe = true;
    boolean volumeMode = false;
    boolean bookmarked = false;

    private Toast mToast;
    // Classes that inherit from AbstractDeviceListener can be used to receive events from Myo devices.
    // If you do not override an event, the default behavior is to do nothing.
    private DeviceListener mListener = new AbstractDeviceListener() {
        @Override
        public void onConnect(Myo myo, long timestamp) {
            showToast("Connected");
        }
        @Override
        public void onDisconnect(Myo myo, long timestamp) {
            showToast("Disconnected");
        }
        // onPose() is called whenever the Myo detects that the person wearing it has changed their pose, for example,
        // making a fist, or not making a fist anymore.
        @Override
        public void onPose(Myo myo, long timestamp, Pose pose) {
            // Handle the cases of the Pose enumeration, and change the text of the text view
            // based on the pose we receive.
            switch (pose) {
                case UNKNOWN:
                    volumeMode = false;
                    showToast("Unknown");
                    break;
                case REST:
                case DOUBLE_TAP:
                    volumeMode = false;
                    showToast("Double Tap");
                    break;
                case FIST:
                    volumeMode = true;
                    showToast("Fist");
                    break;
                case WAVE_IN:
                    volumeMode = false;
                    prev_step();
                    showToast("Wave in");
                    break;
                case WAVE_OUT:
                    volumeMode = false;
                    next_step();
                    showToast("Wave Out");
                    break;
                case FINGERS_SPREAD:
                    volumeMode = false;
                    repeat_step();
                    showToast("Spread Fingers");
                    break;
            }
            if (pose != Pose.UNKNOWN && pose != Pose.REST) {
                // Tell the Myo to stay unlocked until told otherwise. We do that here so you can
                // hold the poses without the Myo becoming locked.
                myo.unlock(Myo.UnlockType.HOLD);
                // Notify the Myo that the pose has resulted in an action, in this case changing
                // the text on the screen. The Myo will vibrate.
                myo.notifyUserAction();
            } else {
                // Tell the Myo to stay unlocked only for a short period. This allows the Myo to
                // stay unlocked while poses are being performed, but lock after inactivity.
                myo.unlock(Myo.UnlockType.TIMED);
            }
        }

        @Override
        public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
            float roll = (float) Math.toDegrees(Quaternion.roll(rotation));
            float pitch = (float) Math.toDegrees(Quaternion.pitch(rotation));
            float yaw = (float) Math.toDegrees(Quaternion.yaw(rotation));

            if(volumeMode){
                showToast("roll");
                AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                int volume = Math.min((int)((roll + 60)*(10.0/80.0)), 10);
                showToast(Integer.toString(volume));
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0); // 15 max, cap at 10/11
                //-60 to 20
            }
        }
    };

    private void showToast(String text) {
        if (mToast == null) {
            mToast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(text);
        }
        mToast.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_recipe);

        //CHANGE ACTIONBAR COLORS
        getActionBar().setBackgroundDrawable(new
                ColorDrawable(Color.parseColor("#ffffff")));
        int titleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
        TextView titleText = (TextView)findViewById(titleId);
        titleText.setTextColor(Color.parseColor("#000000"));

        // First, we initialize the Hub singleton with an application identifier.
        Hub hub = Hub.getInstance();
        if (!hub.init(this, getPackageName())) {
            showToast("Couldn't initialize Hub");
            finish();
            return;
        }
        // Disable standard Myo locking policy. All poses will be delivered.
        //hub.setLockingPolicy(Hub.LockingPolicy.NONE);
        // Next, register for DeviceListener callbacks.
        hub.addListener(mListener);
        // Finally, scan for Myo devices and connect to the first one found that is very near.
        hub.attachToAdjacentMyo();

        //WIT AI STUFF
        String accessToken = "U55JDKGFF6CYR3XV64RJTTCX3OQZKL57";
        _wit = new Wit(accessToken, this);
        tts = new TextToSpeech(this, this);
        tts.setSpeechRate(f);

        //INTENT DATA
        Intent intent = getIntent();

        final TextView title = (TextView) findViewById(R.id.heading);
        recipeName = intent.getExtras().getString("title");
        title.setText(recipeName);

        int count = 0;
        String name = "recipe-directions";
        String temp = intent.getExtras().getString(name);
        while (temp != null) {
            list.add(temp);
            count++;
            name = "item" + count;
            temp = intent.getExtras().getString(name);
        }

        //format recipeStr
        for (int i = 0; i<list.size();i++){
            recipeStr.append(list.get(i));
            recipeStr.append("\n");
            recipeStr.append("\n");
        }

        ingredients = Arrays.asList(intent.getExtras().getStringArray("ingredients"));

        //format ingredientsStr
        for (int i = 0; i<ingredients.size();i++){
            ingredientsStr.append(ingredients.get(i));
            ingredientsStr.append("\n");
        }

        ((TextView) findViewById(R.id.txtText)).setText(recipeStr.toString());

        final Button recipeBtn = (Button) findViewById(R.id.recipeBtn);
        final Button ingredientsBtn = (Button) findViewById(R.id.ingredientsBtn);
        recipeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!recipe) {
                    ((TextView) findViewById(R.id.txtText)).setText(recipeStr.toString());
                    recipe = true;
                    recipeBtn.setBackgroundColor(Color.parseColor("#44b9cb"));
                    ingredientsBtn.setBackgroundColor(Color.parseColor("#2eaac0"));
                }
            }
        });
        ingredientsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (recipe) {
                    ((TextView) findViewById(R.id.txtText)).setText(ingredientsStr.toString());
                    recipe = false;
                    recipeBtn.setBackgroundColor(Color.parseColor("#2eaac0"));
                    ingredientsBtn.setBackgroundColor(Color.parseColor("#44b9cb"));
                }
            }
        });

        //GET BOOKMARKED
        Gson gs = new Gson();
        int i = 0;
        while(getIntent().getStringExtra("bookmarked"+i) != null) {
            String gson = getIntent().getStringExtra("bookmarked"+i);
            recipes.add(gs.fromJson(gson, Recipe.class));
            i++;
        }

        //Check if current is bookmarked
        for(int j = 0; j<recipes.size();j++){
            titles.add(recipes.get(j).toString());
        }

        if(titles.contains(recipeName)) {
            bookmarked = true;
            //((TextView) findViewById(R.id.txtText)).setText("true");
        }
        else {
            bookmarked = false;
            //((TextView) findViewById(R.id.txtText)).setText("false");
        }
    }

    @Override
    public void onDestroy() {
        // Don't forget to shutdown tts!
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();

        Hub.getInstance().removeListener(mListener);
        Hub.getInstance().shutdown();
    }

    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
        } else {
            Log.e("TTS", "Initilization Failed!");
        }
        //speakOut(0);
    }

    private void speakOut(int i) {
        if(list.size() != 0)
            tts.speak((String) list.get(i), TextToSpeech.QUEUE_FLUSH, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_recipe, menu);

        MenuItem myoConnect = menu.findItem(R.id.myo_connect);
        Intent intent = new Intent(this, ScanActivity.class);
        myoConnect.setIntent(intent);

        MenuItem bookmarkedItem = menu.findItem(R.id.bookmarked);
        MenuItem newURL = menu.findItem(R.id.newURL);

        Intent intent2 = new Intent(this, ListViewRemovalAnimation.class);
        Intent intent4 = new Intent(this, MainActivity.class);

        //BOOKMARKS
        Gson gs = new Gson();
        String bookmarks;
        for(int i = 0; i<recipes.size();i++){
            bookmarks = gs.toJson(recipes.get(i));
            intent2.putExtra("bookmarked"+i, bookmarks);
            intent4.putExtra("bookmarked" + i, bookmarks);
        }

        bookmarkedItem.setIntent(intent2);
        newURL.setIntent(intent4);

        //BOOKMAKRING OR UNBOOKMARKING
        Intent intent3 = new Intent(this, NewRecipe.class);

        MenuItem bookmark = menu.findItem(R.id.bookmark);
        if(list.get(0).equals((String) "Senpye you did not enter a recipe")){
            bookmark.setTitle("Invalid URL");
        }
        else {
            if (bookmarked) {
                bookmark.setTitle("unbookmark");
                recipes.remove(titles.indexOf(recipeName));
            } else {
                Recipe recipeHolder = new Recipe(recipeName, list, ingredients);
                recipes.add(recipeHolder);
            }

            intent3.putExtra("title", recipeName);

            String[] strArrayHolder = new String[ingredients.size()];
            strArrayHolder = ingredients.toArray(strArrayHolder);
            intent3.putExtra("ingredients", strArrayHolder);

            intent3.putExtra("recipe-directions", list.get(0));
            for (int i = 1; i < list.size() + 1; i++) {
                intent3.putExtra("item" + i, list.get(i - 1));
            }

            for (int i = 0; i < recipes.size(); i++) {
                bookmarks = gs.toJson(recipes.get(i));
                intent3.putExtra("bookmarked" + i, bookmarks);
            }

            bookmark.setIntent(intent3);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

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
        //TextView jsonView = (TextView) findViewById(R.id.jsonView);
        //jsonView.setMovementMethod(new ScrollingMovementMethod());
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        if (error != null) {
            //jsonView.setText(error.getLocalizedMessage());
            return ;
        }
        String jsonOutput = gson.toJson(witOutcomes);
        //jsonView.setText(jsonOutput);

        if (jsonOutput.indexOf("next_step") != -1)
            next_step();
        else if (jsonOutput.indexOf("prev_step") != -1)
            prev_step();
        else if (jsonOutput.indexOf("repeat_step") != -1)
            repeat_step();

        //((TextView) findViewById(R.id.txtText)).setText("Done!");
    }

    @Override
    public void witDidStartListening() {
        //((TextView) findViewById(R.id.txtText)).setText("Witting...");
    }

    @Override
    public void witDidStopListening() {
        //((TextView) findViewById(R.id.txtText)).setText("Processing...");
    }

    @Override
    public void witActivityDetectorStarted() {
        //((TextView) findViewById(R.id.txtText)).setText("Listening");
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
        tts.stop();
        if (indice + 1 < this.list.size()) {
            indice++;
            speakOut(indice);
        }
    }
    public void prev_step()
    {
        tts.stop();
        if (indice != 0 && indice < this.list.size()) {
            indice--;
            speakOut(indice);
        }
    }

    public void repeat_step()
    {
        tts.stop();
        if (indice < this.list.size())
            speakOut(indice);
    }
}
