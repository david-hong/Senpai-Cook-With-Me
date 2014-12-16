package com.example.feastbeast;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.content.Intent;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;


public class MainActivity extends Activity {

    private TextView respText;
    public Directions directions = new Directions();
    public ArrayList<String> ingredients = new ArrayList<String>();
    public String title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Typeface brandonGrotesque = Typeface.createFromAsset(getAssets(), "fonts/Brandon_blk.otf");

        final EditText edtUrl = (EditText) findViewById(R.id.edtURL);
        final TextView txt1 = (TextView) findViewById(R.id.txt1);
        Button btnGo = (Button) findViewById(R.id.btnGo);
        btnGo.setTypeface(brandonGrotesque);
        txt1.setTypeface(brandonGrotesque);

        respText = (TextView) findViewById(R.id.edtResp);
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
                    /*
                    else
                        onClick(view);
                     */
                }
                else
                    respText.setText("error, you need to enter a URL");

                if(directions.current != null)
                    newRec(view);
                else
                    respText.setText("Error, only accepts valid recipes from food.com, foodnetwork.ca and allrecipes.com");
            }
        });
    }

    public void newRec(View view){
        //EditText x = (EditText)findViewById(R.id.edtURL);
        //x.setText(directions.current.value);
        Intent intent = new Intent(this,NewRecipe.class);
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

        intent.putExtra("title", title);
        startActivity(intent);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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
                //Log.d("JSwA", "Title ["+title+"]");
                //buffer.append("Title: " + title + "\r\n");

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