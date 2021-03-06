package com.example.fydel.cryptocompare;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

import com.android.volley.toolbox.Volley;

import com.example.fydel.cryptocompare.Model.GetRates;

/**
 * Created by FYDEL on 10/20/2017.
 */

public class ExchangeRateActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    LinearLayout mainView;
    SwipeRefreshLayout mSwipeRefreshLayout;
    ListView ratesListView;
    public GetRates ratesLedger;
    RequestQueue rQueue;
    String requestUrl;

    SharedPreferences settings;
    int orderMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        mainView = (LinearLayout) findViewById(R.id.main_view);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.layout_swipe_refresh);
        ratesListView = (ListView) findViewById(R.id.items_listView);
        ratesListView.setDivider(null); //to remove dividers from the list view

        ratesLedger = new GetRates();

        rQueue = Volley.newRequestQueue(getApplicationContext());

        settings = getSharedPreferences("mSettings", MODE_PRIVATE);
        orderMode = settings.getInt("orderMode", FixedValues.ORDER_ALPHABETICAL);


        requestUrl = "https://min-api.cryptocompare.com/data/pricemulti?fsyms=BTC,ETH&tsyms=" +
                "AFN,DZD,NGN,USD,EUR,INR,GBP,EGP,JPY,GBP,AUD,CAD,CHF,XOF,CNY,KES,GHS,UGX,ZAR,XAF,NZD,MYR,RUB,BND,GEL";

        downloadRates();

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                downloadRates();
            }
        });

    }


    private static class ViewHolder {
        private TextView currencyTextView;
        private TextView btcTextView;
        private TextView ethTextView;
    }


    private class MyAdapter extends BaseAdapter {
        ArrayList<GetRates.MoneyRate> rates;

        private MyAdapter(ArrayList<GetRates.MoneyRate> ratesInstance) {
            rates = ratesInstance;
        }

        public int getCount() {
            return rates.size();
        }

        public Object getItem(int arg0) {
            return null;
        }

        public long getItemId(int position) {
            return position;
        }

        @SuppressLint("DefaultLocale")
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.list_view_header_exchange_rate, parent, false);
                holder = new ViewHolder();
                holder.currencyTextView = (TextView) convertView.findViewById(R.id.currency_TextViewList);
                holder.btcTextView = (TextView) convertView.findViewById(R.id.btc_getrateTextViewList);
                holder.ethTextView = (TextView) convertView.findViewById(R.id.eth_getrateTextViewList);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final GetRates.MoneyRate rateRow = rates.get(position);

            final String crossCurrency = rateRow.getCurrency();
            final double crossBtc = rateRow.getBtcRate();
            final double crossEth = rateRow.getEthRate();

            holder.currencyTextView.setText(crossCurrency);
            holder.btcTextView.setText(String.format("%1$,.2f", crossBtc));
            holder.ethTextView.setText(String.format("%1$,.2f", crossEth));

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(ExchangeRateActivity.this, ExchangeConvertActivity.class);
                    intent.putExtra(FixedValues.EXTRA_CURRENCY, crossCurrency);
                    intent.putExtra(FixedValues.EXTRA_BTC_RATE, crossBtc);
                    intent.putExtra(FixedValues.EXTRA_ETH_RATE, crossEth);
                    startActivity(intent);
                }
            });

            return convertView;

        }
    }

    //Get JSON objects for currencies using the API URLs
    public void downloadRates() {
        ratesLedger = new GetRates();

        JsonObjectRequest requestNameAvatar = new JsonObjectRequest(Request.Method.GET, requestUrl, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject btc_rates = response.getJSONObject("BTC".trim());
                            JSONObject eth_rates = response.getJSONObject("ETH".trim());

                            Iterator<?> keysBTC = btc_rates.keys();
                            Iterator<?> keysETH = eth_rates.keys();

                            while (keysBTC.hasNext() && keysETH.hasNext()) {
                                String keyBTC = (String) keysBTC.next();
                                String keyETH = (String) keysETH.next();

                                ratesLedger.add(keyBTC, btc_rates.getDouble(keyBTC), eth_rates.getDouble(keyETH));
                            }

                            mSwipeRefreshLayout.setRefreshing(false); //to remove the progress bar for refresh
                            ratesLedger.orderList(orderMode);
                            ratesListView.setAdapter(new MyAdapter(ratesLedger.ratesArrayList));


                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(), "Cannot download currency list", Toast.LENGTH_LONG).show();
                        }
                    }
                }, new Response.ErrorListener() {


            @Override
            public void onErrorResponse(VolleyError error) {
                Snackbar.make(mainView, FixedValues.REFRESH_ERROR, Snackbar.LENGTH_LONG).setAction("Action", null).show();
                mSwipeRefreshLayout.setRefreshing(false);
            }


        });

        rQueue.add(requestNameAvatar);
    }

    @Override
    protected void onStop() {
        super.onStop();
        rQueue.cancelAll(this);
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //Menu options for refresh, order and about
        if (id == R.id.action_refresh) {
            mSwipeRefreshLayout.setRefreshing(true); // progress bar for refresh
            downloadRates();
            return true;
        }else if (id == R.id.action_exit) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        //Navigation bar menu
        int id = item.getItemId();



        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}

