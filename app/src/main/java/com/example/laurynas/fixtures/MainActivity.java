package com.example.laurynas.fixtures;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends ListActivity implements AdapterView.OnItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Date currentDate_1 = new Date();
        String date = formatter.format(currentDate_1);
        String html, oldDate;
        TextView textView = (TextView) findViewById(R.id.textView3);
        textView.setText("");

        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.spinner_array, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
        spinner.setOnItemSelectedListener(this);
        if (isNetworkAvailable(getApplicationContext())) {
            HTMLParser parser = null;
            try {
                parser = new HTMLoadTask().execute("http://www.skysports.com/football/fixtures").get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            html = getHTML(parser);/*
            Toast.makeText(getApplicationContext(), html, Toast.LENGTH_SHORT).show();
            System.out.print(html);*/
            writeToFile(date + "\n" + html, getApplicationContext());
        } else {
            spinner.setVisibility(View.GONE);
            TextView tv = (TextView) findViewById(R.id.textView3);
            tv.setVisibility(View.GONE);
            html = readFromFile(getApplicationContext());
            RelativeLayout.LayoutParams pr = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);

            pr.addRule(RelativeLayout.BELOW, R.id.imageView);
            getListView().setLayoutParams(pr);
            String[] p = html.split("/");
            oldDate = p[0] + "/" + p[1] + "/" + p[2].charAt(0) + p[2].charAt(1) + p[2].charAt(2) + p[2].charAt(3);
            Toast.makeText(getApplicationContext(), "No internet connection, using data from last login, that was on " + oldDate, Toast.LENGTH_SHORT).show();
        }


        final String[] leagues = html.split("<h5");
        final List<String> arrayList = getAllThingsBetween("class=\"fixres__header3\">", "</h5>", html);
        //textView.setVisibility(View.GONE);
        getListView().setVisibility(View.VISIBLE);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getListView().getContext(), android.R.layout.simple_list_item_1, arrayList);
        getListView().setAdapter(adapter);
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {
                Intent i = new Intent(getApplicationContext(), GamesActivity.class);
                i.putExtra("HTML", leagues[position + 1]);
                i.putExtra("LeagueName", arrayList.get(position));
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
                Date currentDate_1 = new Date();
                String date = formatter.format(currentDate_1);
                i.putExtra("Date", date);
                startActivity(i);
            }
        });

    }

    public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {

        switch (position) {
            case 0:
                break;
            case 1:
                toYesterday();
                break;
            case 2:
                toTomorrow();
                break;
            case 3:
                toDate();
                break;
            case 4:
                toSeparateFixtures();
                break;
            case 5:
                toLeagues();
                break;
        }

    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // TODO Auto-generated method stub
    }
    public void toTest(){
        Intent i = new Intent(getApplicationContext(), SeparateGameActivity.class);
        startActivity(i);
    }
    public void toLeagues(){
        Intent i = new Intent(getApplicationContext(), LeagueTablesActivity.class);
        startActivity(i);
    }
    public void toDate() {
        Intent i = new Intent(getApplicationContext(), DateActivity.class);
        startActivity(i);
    }

    public void toYesterday() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Date currentDate_1 = new Date();
        String date = formatter.format(currentDate_1);
        String[] parts = date.split("/");
        int day = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int year = Integer.parseInt(parts[2]);
        if (isChangedYear(day, month)) {
            day = 31;
            month = 12;
            year--;
        } else if (isChangedBack(day)) {
            day = getDays(month--, year);
        } else day--;
        Intent i = new Intent(getApplicationContext(), DayActivity.class);

        int monthString = month;
        i.putExtra("Date", String.valueOf(day) + "-" + String.valueOf(monthString) + "-" + String.valueOf(year));
        startActivity(i);
    }

    public void toTomorrow() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Date currentDate_1 = new Date();
        String date = formatter.format(currentDate_1);
        String[] parts = date.split("/");
        int day = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int year = Integer.parseInt(parts[2]);
        if (isChangedYear(day, month)) {
            day = 1;
            month = 1;
            year++;
        } else if (isChangedForward(day, month, year)) {
            day = 1;
            month++;
        } else day++;
        Intent i = new Intent(getApplicationContext(), DayActivity.class);
        i.putExtra("Date", String.valueOf(day) + "-" + String.valueOf(month) + "-" + String.valueOf(year));
        startActivity(i);

    }

    public void toSeparateFixtures() {
        Intent i = new Intent(getApplicationContext(), LeaguesActivity.class);
        startActivity(i);
    }

    public boolean isChangedYear(int day, int m) {
        if ((day == 1 && m == 1) || (day == 31 && m == 12)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isChangedBack(int day) {
        if (day > 1) {
            return false;
        } else {
            return true;
        }
    }

    public boolean isChangedForward(int day, int m, int y) {
        if (m == 1 || m == 3 || m == 5 || m == 7 || m == 8 || m == 10 || m == 12) {
            if (day == 31) return true;
        } else if (m == 4 || m == 6 || m == 9 || m == 11) {
            if (day == 30) return true;
        } else if (m == 2 && y % 4 == 0) {
            if (day == 29) return true;
        } else if (m == 2) {
            if (day == 28) return true;
        }
        return false;
    }

    public int getDays(int m, int y) {
        if (m == 1 || m == 3 || m == 5 || m == 7 || m == 8 || m == 10 || m == 12) {
            return 31;
        } else if (m == 4 || m == 6 || m == 9 || m == 11) {
            return 30;
        } else if (m == 2 && y % 4 == 0) {
            return 29;
        } else if (m == 2) {
            return 28;
        } else return 0;
    }

    public String getHTML(HTMLParser htmlParser) {
        String html = "";
        Elements content = htmlParser.getContent();


        html += content.html();
        return html;
    }

    private List<String> getAllThingsBetween(String pat1, String pat2, String data) {
        List<String> stringList = new ArrayList<String>();
        String regexString = Pattern.quote(pat1) + "(.*?)" + Pattern.quote(pat2);
        Pattern pattern = Pattern.compile(regexString);
        Matcher matcher = pattern.matcher(data);

        while (matcher.find()) {
            String textInBetween = matcher.group(1);
            stringList.add(textInBetween);
        }
        return stringList;
    }


    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (conMan.getActiveNetworkInfo() != null && conMan.getActiveNetworkInfo().isConnected())
            return true;
        else
            return false;
    }

    private void writeToFile(String data, Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("config.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private String readFromFile(Context context) {

        String ret = "";

        try {
            InputStream inputStream = context.openFileInput("config.txt");

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        } catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }

    void loadedScreen() {
        TextView textView = (TextView) findViewById(R.id.textView3);
        textView.setText("");
    }

    class HTMLoadTask extends AsyncTask<String, Void, HTMLParser> {
        public int a = 1;

        @Override
        protected void onPreExecute() {
            //loadingScreen();
        }

        /*void loadingScreen(){
            ImageView imageView = (ImageView)findViewById(R.id.imageView);
            imageView.setVisibility(View.VISIBLE);
            getListView().setVisibility(View.GONE);
            Button button1 = (Button) findViewById(R.id.button);
            button1.setVisibility(View.GONE);
            button1 = (Button) findViewById(R.id.button2);
            button1.setVisibility(View.GONE);
            button1 = (Button) findViewById(R.id.button3);
            button1.setVisibility(View.GONE);
            TextView textView = (TextView)findViewById(R.id.textView3);
            textView.setText("Loading results & Fixtures");
        }*/
        @Override
        protected HTMLParser doInBackground(String... urls) {
            HTMLParser parser = new HTMLParser();
            parser.load(urls[0]);

            return parser;
        }

    }
}
