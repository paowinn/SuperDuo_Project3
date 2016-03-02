package it.jaschke.alexandria;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Created by alvarpao on 3/1/2016.
 */
public class BookDetailActivity extends ActionBarActivity {

    private BookDetail mBookDetailFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.book_detail);

        if(savedInstanceState == null){
            mBookDetailFragment = new BookDetail();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.right_container, mBookDetailFragment)
                    .commit();
        }

        else{

            mBookDetailFragment =  new BookDetail();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.right_container,
                            mBookDetailFragment)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
