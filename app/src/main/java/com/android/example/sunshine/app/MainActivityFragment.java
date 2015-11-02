package com.android.example.sunshine.app;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.Arrays;
import java.util.List;

public class MainActivityFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        List<String> weekForecast = Arrays.asList(
                "Today - Sunny - 25/20",
                "Tomorrow - Foggy - 21/19",
                "Weds - Cloudy - 20/16",
                "Thurs - Rainy - 21/16",
                "Fri - Foggy - 22/19",
                "Sat - Sunny - 25/16",
                "Sun - Sunny - 26/17"
        );

        ArrayAdapter<String> mForecastAdapter = new ArrayAdapter<>(getContext(),
                R.layout.list_item_forecast, R.id.list_item_forecast_textview, weekForecast);

        View view = inflater.inflate(R.layout.fragment_main, container, false);

        ListView listView = (ListView) view.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);

        return view;
    }
}
