package com.htmlparser.app;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class MainActivity extends Activity {

    private EditText respText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final EditText edtUrl = (EditText) findViewById(R.id.edtURL);
        Button btnGo = (Button) findViewById(R.id.btnGo);
        respText = (EditText) findViewById(R.id.edtResp);
        btnGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String siteUrl = edtUrl.getText().toString().toLowerCase();
                if (! (siteUrl.substring(0, Math.min(siteUrl.length(), 7)).equals("http://")))
                    siteUrl = "http://" + siteUrl;
                ( new ParseURL() ).execute(new String[]{siteUrl});
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            }
        });
        //Button btnNewRec = (Button)findViewById(R.id.btnNewRecipe);
        /*btnNewRec.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this , NewRecipe.class);
                startActivity(intent);
            }
        });*/
    }

    public void newRec(View view){
        Intent intent = new Intent(this,NewRecipe.class);
        startActivity(intent);
    }

    @Override
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
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class ParseURL extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            StringBuffer buffer = new StringBuffer();
            try {
                Log.d("JSwa", "Connecting to ["+strings[0]+"]");
                Document doc  = Jsoup.connect(strings[0]).get();
                Log.d("JSwa", "Connected to ["+strings[0]+"]");
                // Get document (HTML page) title
                String title = doc.title();
                Log.d("JSwA", "Title ["+title+"]");
                buffer.append("Title: " + title + "\r\n");

                Elements topicList = doc.select(".recipeInstructions");
                buffer.append("Directions \r\n");
                for (Element topic : topicList) {
                    String data = topic.text();
                    buffer.append("Data [" +data+ "] \r\n");
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
        }

    }
}