package barqsoft.footballscores;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import barqsoft.footballscores.service.myFetchService;
import barqsoft.footballscores.widget.DetailWidgetRemoteViewsService;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainScreenFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>
{
    public scoresAdapter mAdapter;
    public static final int SCORES_LOADER = 0;
    private String[] fragmentdate = new String[1];
    private int last_selected_item = -1;
    private ListView score_list;
    private TextView mEmptyView;
    private static final String FRAG_DATE = "date";

    public MainScreenFragment()
    {
    }

    private void update_scores()
    {
        Intent service_start = new Intent(getActivity(), myFetchService.class);
        getActivity().startService(service_start);
    }
    public void setFragmentDate(String date)
    {
        fragmentdate[0] = date;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        update_scores();
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        score_list = (ListView) rootView.findViewById(R.id.scores_list);
        mEmptyView = (TextView) rootView.findViewById(R.id.empty_view);
        score_list.setEmptyView(mEmptyView);
        mAdapter = new scoresAdapter(getActivity(),null,0);
        score_list.setAdapter(mAdapter);
        score_list.setContentDescription(getString(R.string.a11y_score_list, fragmentdate[0]));
        getLoaderManager().initLoader(SCORES_LOADER, null, this);
        mAdapter.detail_match_id = MainActivity.selected_match_id;

        // **PAA** When the app is launched from the widget, obtain the selected match's id
        // and provide the match's detail view in the app's layout
        if(getActivity().getIntent().
                getStringExtra(DetailWidgetRemoteViewsService.MATCH_ID_WIDGET) != null){

            // **PAA** Notify the adapter that a match has been selected so it displays
            // the appropriate detail view
            mAdapter.detail_match_id = Double.parseDouble(getActivity().getIntent().
                    getStringExtra(DetailWidgetRemoteViewsService.MATCH_ID_WIDGET));
            MainActivity.selected_match_id = (int)mAdapter.detail_match_id;
            mAdapter.notifyDataSetChanged();
            // **PAA** Remove the match id from the intent to prevent the app from restoring
            // to the original widget selected match every time the configuration changes
            getActivity().getIntent().removeExtra(DetailWidgetRemoteViewsService.MATCH_ID_WIDGET);
        }


        score_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ViewHolder selected = (ViewHolder) view.getTag();
                mAdapter.detail_match_id = selected.match_id;
                MainActivity.selected_match_id = (int) selected.match_id;
                mAdapter.notifyDataSetChanged();
            }
        });

        // **PAA** Restore the fragment's date after a configuration change
        if(savedInstanceState != null){
            if(savedInstanceState.containsKey(FRAG_DATE)) {
                fragmentdate[0] = savedInstanceState.getString(FRAG_DATE);
            }
        }

        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle)
    {
        return new CursorLoader(getActivity(),DatabaseContract.scores_table.buildScoreWithDate(),
                null,null,fragmentdate,null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor)
    {
        //Log.v(FetchScoreTask.LOG_TAG,"loader finished");
        //cursor.moveToFirst();
        /*
        while (!cursor.isAfterLast())
        {
            Log.v(FetchScoreTask.LOG_TAG,cursor.getString(1));
            cursor.moveToNext();
        }
        */

        int i = 0;
        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            i++;
            cursor.moveToNext();
        }
        //Log.v(FetchScoreTask.LOG_TAG,"Loader query: " + String.valueOf(i));
        mAdapter.swapCursor(cursor);
        //mAdapter.notifyDataSetChanged();

        // **PAA** When the app is launched from the widget (after is done loading its data),
        // obtain the position of the item selected so it can be set as the scrolled and selected
        // position in the opened app
        if(getActivity().getIntent().
                getStringExtra(DetailWidgetRemoteViewsService.SCROLL_POSITION_WIDGET) != null){

            int pos = Integer.parseInt(getActivity().getIntent().
                    getStringExtra(DetailWidgetRemoteViewsService.SCROLL_POSITION_WIDGET));

            if (pos != ListView.INVALID_POSITION) {
                score_list.setSelection(pos);
                score_list.smoothScrollToPosition(pos);
            }
            // **PAA** Remove the scroll position from the intent to prevent the app from restoring
            // to the original widget selected item's scroll position every time the configuration
            // changes
            getActivity().getIntent().removeExtra(DetailWidgetRemoteViewsService.SCROLL_POSITION_WIDGET);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader)
    {
        mAdapter.swapCursor(null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        // **PAA** Save fragment's date before a configuration change in order to be restored
        // later
        if(fragmentdate[0] != null)
          outState.putString(FRAG_DATE, fragmentdate[0]);

        super.onSaveInstanceState(outState);
    }
}
