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


public class help extends Activity {

    public List<Recipe> recipes = new ArrayList<Recipe>();

    private Toast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help);

        //CHANGE ACTIONBAR COLORS
        getActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#ffffff")));
        int titleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
        TextView titleText = (TextView)findViewById(titleId);
        titleText.setTextColor(Color.parseColor("#000000"));
        getActionBar().setIcon(android.R.color.transparent);

        //GET BOOKMARKED
        Intent intent = getIntent();
        Gson gs = new Gson();
        int i = 0;
        final ArrayList<String> cheeseList = new ArrayList<String>();
        while(getIntent().getStringExtra("bookmarked"+i) != null) {
            String gson = getIntent().getStringExtra("bookmarked"+i);
            recipes.add(gs.fromJson(gson, Recipe.class));
            i++;
        }


        final Button newRecipe = (Button) findViewById(R.id.newRecip);
        final Button create = (Button) findViewById(R.id.create);
        final Button bookmarks = (Button) findViewById(R.id.bookmarks);

        newRecipe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent mainAct = new Intent(help.this, MainActivity.class);
                Gson gs2 = new Gson();
                String bookmarkss;
                mainAct.putExtra("opened", true);
                for(int j = 0; j<recipes.size();j++){
                    bookmarkss = gs2.toJson(recipes.get(j));
                    mainAct.putExtra("bookmarked"+j, bookmarkss);
                }
                startActivity(mainAct);
            }
        });
        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent createAct = new Intent(help.this,CreateRecipe.class);
                Gson gs2 = new Gson();
                String bookmarkss;
                for(int j = 0; j<recipes.size();j++){
                    bookmarkss = gs2.toJson(recipes.get(j));
                    createAct.putExtra("bookmarked" + j, bookmarkss);
                }
                startActivity(createAct);
            }
        });
        bookmarks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent bookmarkAct = new Intent(help.this, ListViewRemovalAnimation.class);
                Gson gs2 = new Gson();
                String bookmarkss;
                for(int j = 0; j<recipes.size();j++){
                    bookmarkss = gs2.toJson(recipes.get(j));
                    bookmarkAct.putExtra("bookmarked" + j, bookmarkss);
                }
                startActivity(bookmarkAct);
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();

        //GET BOOKMARKED FROM INTENT
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

    private void showToast(String text) {
        if (mToast == null) {
            mToast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(text);
        }
        mToast.show();
    }

    public void writeJsonStream(OutputStream out, List<Recipe> messages) throws IOException {
        Gson gs = new Gson();
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.setIndent("  ");
        writer.beginArray();
        for (Recipe message : messages) {
            gs.toJson(message, Recipe.class, writer);
        }
        writer.endArray();
        writer.close();
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
}