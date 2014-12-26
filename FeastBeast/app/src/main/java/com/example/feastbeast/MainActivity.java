package com.example.feastbeast;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import com.google.gson.stream.JsonReader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.content.Intent;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;


public class MainActivity extends Activity {

    private TextView respText;
    public Directions directions = new Directions();
    public ArrayList<String> ingredients = new ArrayList<String>();
    public String title;
    public List<Recipe> recipes = new ArrayList<Recipe>();

    //TEST
    private Toast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        respText = (TextView) findViewById(R.id.edtResp);

        //LOAD RECIPES FROM JSON FILE ONLY IF MAINACTIVITY NOT LOADED ALREADY....
        recipes = new ArrayList<Recipe>();
        if(! getIntent().getBooleanExtra("opened", false)) {
            try {
                openFile("data");
            }
            catch(Exception e){
                //file not found? RIP
            }
        }

        //CHANGE ACTIONBAR COLORS
        getActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#ffffff")));
        int titleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
        TextView titleText = (TextView)findViewById(titleId);
        titleText.setTextColor(Color.parseColor("#000000"));

        //INITIALIZE FONTS
        Typeface brandonGrotesque = Typeface.createFromAsset(getAssets(), "fonts/Brandon_blk.otf");

        final EditText edtUrl = (EditText) findViewById(R.id.edtURL);
        final TextView txt1 = (TextView) findViewById(R.id.txt1);
        Button btnGo = (Button) findViewById(R.id.btnGo);
        btnGo.setTypeface(brandonGrotesque);
        txt1.setTypeface(brandonGrotesque);

        respText.setText("Only accepts valid recipes from food.com, foodnetwork.ca and allrecipes.com");
        btnGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String siteUrl = edtUrl.getText().toString();
                if(siteUrl.trim().length() != 0) {
                    directions = new Directions();
                    siteUrl.toLowerCase();
                    if (!(siteUrl.substring(0, Math.min(siteUrl.length(), 7)).equals("http://")))
                        siteUrl = "http://" + siteUrl;
                    (new ParseURL(view)).execute(new String[]{siteUrl});
                }
                else
                    respText.setText("error, you need to enter a URL");

                if(directions.current != null)
                    newRec(view);
                else
                    respText.setText("Error, only accepts valid recipes from food.com, foodnetwork.ca and allrecipes.com");
            }
        });

        //TEST
        final Button save = (Button) findViewById(R.id.save);
        final Button load = (Button) findViewById(R.id.load);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    FileOutputStream output = getApplicationContext().openFileOutput("data", Context.MODE_PRIVATE);
                    writeJsonStream(output, recipes);
                    output.close();
                }
                catch(Exception e){
                }
            }
        });

        load.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    openFile("data");
                }
                catch(Exception e){
                    //file not found? RIP
                }
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();

        //GET BOOKMARKED FROM INTENT -- MOVE TO ONRESUME()
        if (getIntent().getStringExtra("bookmarked"+0) != null)
            recipes = new ArrayList<Recipe>();
        Gson gs = new Gson();
        int i = 0;
        while(getIntent().getStringExtra("bookmarked"+i) != null) {
            String gson = getIntent().getStringExtra("bookmarked"+i);
            recipes.add(gs.fromJson(gson, Recipe.class));
            i++;
        }
    }

    @Override
    protected void onStop(){
        try {
            FileOutputStream output = getApplicationContext().openFileOutput("data", Context.MODE_PRIVATE);
            writeJsonStream(output, recipes);
            output.close();
        }
        catch(Exception e){
        }

        super.onStop();
    }

    public void openFile(String fileName) throws java.io.IOException{
        //InputStream input = getAssets().open(fileName);
        InputStream input = openFileInput(fileName);
        readJsonStream(input);
        input.close();
    }

    public void readJsonStream(InputStream in) throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        reader.beginArray();
        Gson gs = new Gson();
        while (reader.hasNext()) {
            Recipe recip = gs.fromJson(reader, Recipe.class);
            recipes.add(recip);
        }
        reader.endArray();
        reader.close();
        return;
    }

    public void writeJsonStream(OutputStream out, List<Recipe> messages) throws IOException {
        showToast("Valid output stream");
        Gson gs = new Gson();
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.setIndent("  ");
        showToast("Trying Saving");
        writer.beginArray();
        showToast("Saving");
        for (Recipe message : messages) {
            gs.toJson(message, Recipe.class, writer);
        }
        writer.endArray();
        writer.close();
    }

    private void showToast(String text) {
        if (mToast == null) {
            mToast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(text);
        }
        mToast.show();
    }

    //LOAD NEWRECIPE ACTIVITY
    public void newRec(View view){
        Intent intent = new Intent(this,NewRecipe.class);
        intent.putExtra("title", title);

        int count = 0;
        String name = "recipe-directions";

        intent.putExtra(name,"Senpye you did not enter a recipe");
        while (directions.current != null)
        {
            intent.putExtra(name, directions.current.value);
            count++;
            name="item"+count;
            directions.current=directions.current.next;
        }
        String[] strArrayHolder = new String[ingredients.size()];
        strArrayHolder = ingredients.toArray(strArrayHolder);
        intent.putExtra("ingredients",strArrayHolder);

        Gson gs = new Gson();
        String bookmarked;

        for(int i = 0; i<recipes.size();i++){
            bookmarked = gs.toJson(recipes.get(i));
            intent.putExtra("bookmarked"+i, bookmarked);
        }

        startActivity(intent);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        MenuItem bookmarked = menu.findItem(R.id.bookmarked);
        Intent intent2 = new Intent(this, ListViewRemovalAnimation.class);

        //BOOKMARKS
        Gson gs = new Gson();
        String bookmarks;
        for(int i = 0; i<recipes.size();i++){
            bookmarks = gs.toJson(recipes.get(i));
            intent2.putExtra("bookmarked"+i, bookmarks);
        }
        bookmarked.setIntent(intent2);

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

    class Node{
        String value = "";
        Node next = null;
        Node prev = null;

        public Node(String str){
            this.value = str;
        }
    }


    class Directions implements Serializable{
        Node current = null;

        public Directions(){
            this.current = null;
        }

        public Directions(String str){
            this.current = new Node(str);
        }

        protected void add(String str){
            if(this.current != null) {
                Node holder = this.current;
                while (holder.next != null)
                    holder = holder.next;
                holder.next = new Node(str);
                holder.next.prev = holder;
            }
            else
                this.current = new Node(str);
        }

        public String toString(){
            String str= "";
            Node holder = this.current;
            while(holder != null){
                str = str + holder.value;
                holder = holder.next;
            }
            return str;
        }
    }

    protected class ParseURL extends AsyncTask<String, Void, String> {

        String classFind = "li";
        String ingredientsFind = "";

        View view;

        public ParseURL(View v){
            this.view = v;
        }

        @Override
        protected String doInBackground(String... strings) {
            StringBuffer buffer = new StringBuffer();
            try {
                Log.d("JSwa", "Connecting to ["+strings[0]+"]");
                Document doc  = Jsoup.connect(strings[0]).get();
                Log.d("JSwa", "Connected to ["+strings[0]+"]");
                // Get document (HTML page) title
                title = doc.title();

                if(strings[0].substring(0, Math.min(strings[0].length(), 26)).equals("http://www.foodnetwork.ca/")) {
                    classFind = ".recipeInstructions p";
                    ingredientsFind = ".recipe-ingredients p";
                }
                else if(strings[0].substring(0, Math.min(strings[0].length(), 22)).equals("http://allrecipes.com/")) {
                    classFind = ".directions li";
                }
                else if(strings[0].substring(0, Math.min(strings[0].length(), 20)).equals("http://www.food.com/")) {
                    classFind = ".instructions li";
                }
                else {
                    buffer.setLength(0);
                    buffer.append("Error only accepts recipes from http://www.foodnetwork.ca/, http://allrecipes.com/, http://www.food.com/");
                }

                //GET RECIPE
                Elements topicList = doc.select(classFind);
                if(!buffer.toString().equals("Error only accepts recipes from http://www.foodnetwork.ca/, http://allrecipes.com/, http://www.food.com/")){
                    buffer.append("Directions \r\n");
                      for (Element topic : topicList) {
                          String data = topic.text();
                          directions.add(data);
                          buffer.append(data + "\r\n");
                      }
                    }

                // GET INGREDIENTS
                buffer = new StringBuffer();
                Elements ingredList = doc.select(ingredientsFind);
                if(!buffer.toString().equals("Error only accepts recipes from http://www.foodnetwork.ca/, http://allrecipes.com/, http://www.food.com/")){
                    buffer.append("Ingredients \r\n");
                    for (Element ingredient : ingredList) {
                        String data = ingredient.text();
                        ingredients.add(data);
                        buffer.append(data + "\r\n");
                    }
                }
            }
            catch(Throwable t) {
                t.printStackTrace();
            }
            return buffer.toString();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            respText.setText(s);
            newRec(view);
        }
    }
}