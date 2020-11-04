package com.example.finalnews;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> content=new ArrayList<>();
    ArrayList<String> titles=new ArrayList<>();
    ArrayAdapter arrayAdapter;
    SQLiteDatabase articleDB;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        TextView textView=(TextView) findViewById(R.id.textView);
//        ImageView image =(ImageView) findViewById(R.id.imageView);
//        image.setImageResource(R.drawable.pic);
//        NavigationView nv=(NavigationView)findViewById(R.id.nView);
        ListView listView=(ListView) findViewById(R.id.listView);
        arrayAdapter=new ArrayAdapter(this,android.R.layout.simple_list_item_1,titles);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int i, long id) {
                Intent intent=new Intent(getApplicationContext(),ArticleActivity.class);
                intent.putExtra("content",content.get(i));
                startActivity(intent);
            }
        });
        articleDB=this.openOrCreateDatabase("articles",MODE_PRIVATE,null);
        articleDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId INTEGER, title VARCHAR , content VARCHAR )");
         update();
        Downloadtask task=new Downloadtask();
        try{
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void update(){

        Cursor c=articleDB.rawQuery("SELECT * FROM articles",null);
        int titleIdx=c.getColumnIndex("title");
        //int cidx=c.getColumnIndex("content");
        if(c.moveToFirst()){
            titles.clear();
            do{
                titles.add(c.getString(titleIdx));
              //  content.add(c.getString(cidx));

            }while(c.moveToNext());
            arrayAdapter.notifyDataSetChanged();

        }
    }

    public class Downloadtask extends AsyncTask<String,Void,String>{
        @Override
        public String doInBackground(String... strings){
            String result="";
            URL url;
            HttpURLConnection urlConnection=null;
            try {
                url=new URL(strings[0]);
                urlConnection=(HttpURLConnection)url.openConnection();
                InputStream in=urlConnection.getInputStream();
                InputStreamReader reader=new InputStreamReader(in);
                int data=reader.read();

                while(data!=-1){
                  char curr=(char)data;
                  result+=curr;
                  data=reader.read();
                }
                JSONArray jsonArray=new JSONArray(result);
                int n=50;
                if(jsonArray.length()<n){
                    n=jsonArray.length();
                }
                articleDB.execSQL("DELETE FROM articles");
                for(int i=0;i<n;i++){
                    String articleId=jsonArray.getString(i);

                    URL url1=new URL("https://hacker-news.firebaseio.com/v0/item/" + articleId + ".json?print=pretty");
                    HttpURLConnection urlConnection1=(HttpURLConnection)url1.openConnection();
                    InputStream in1=urlConnection1.getInputStream();
                    InputStreamReader reader1=new InputStreamReader(in1);
                    int data1=reader1.read();
                    String articleInfo="";

                    while(data1!=-1){
                        char curr1=(char)data1;
                        articleInfo+=curr1;
                        data1=reader1.read();
                    }
//                    Log.i("Info",articleInfo);title
                    JSONObject jsonObject=new JSONObject(articleInfo);

                    if(!jsonObject.isNull("title") && !jsonObject.isNull("url")){

                        String articleTitle=jsonObject.getString("title");
                        String articleUrl=jsonObject.getString("url");
                       Log.i("BHAWAY",articleTitle +"->"+articleUrl);
                        content.add(articleUrl);

//                         url=new URL(articleUrl);
//                         urlConnection=(HttpURLConnection)url.openConnection();
//                         in=urlConnection.getInputStream();
//                         reader=new InputStreamReader(in);
//                         data=reader.read();
//                        String articleContent="";
//                        while(data!=-1){
//                            char curr2=(char)data;
//                            articleContent+=curr2;
//                            data=reader.read();
//                        }

                        //Log.i("Content",articleContent);

                        String sql="INSERT INTO articles (articleId,title) VALUES(?,?)";
                        SQLiteStatement s=articleDB.compileStatement(sql);
                        s.bindString(1,articleId);
                        s.bindString(2,articleTitle);
                       // s.bindString(3,articleContent);
                        s.execute();
                    }

                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }


            return result;
        }

        @Override
        protected void onPostExecute(String s){
            super.onPostExecute(s);
            update();
        }
    }
}
