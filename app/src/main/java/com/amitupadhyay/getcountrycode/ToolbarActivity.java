package com.amitupadhyay.getcountrycode;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

/**
 * Created by aupadhyay on 1/24/17.
 */

public abstract class ToolbarActivity extends AppCompatActivity {

    protected Toolbar setupToolbar(boolean home) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (home)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        return toolbar;
    }
}
