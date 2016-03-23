package barqsoft.footballscores.widget;

/**
 * Created by alvarpao on 3/22/2016.
 */

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.text.SimpleDateFormat;
import java.util.Date;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.PagerFragment;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilies;


// RemoteViewsService controlling the data being shown in the scrollable scores detail widget

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class DetailWidgetRemoteViewsService extends RemoteViewsService {
    public final String LOG_TAG = DetailWidgetRemoteViewsService.class.getSimpleName();
    public static final String MATCH_ID_WIDGET = "match_id_widget";

    private static final String[] SCORES_COLUMNS = {
            DatabaseContract.SCORES_TABLE + "." + DatabaseContract.scores_table._ID,
            DatabaseContract.scores_table.HOME_COL,
            DatabaseContract.scores_table.AWAY_COL,
            DatabaseContract.scores_table.HOME_GOALS_COL,
            DatabaseContract.scores_table.AWAY_GOALS_COL,
            DatabaseContract.scores_table.TIME_COL,
            DatabaseContract.scores_table.MATCH_ID
    };
    // these indices must match the projection
    private static final int INDEX_SCORE_ID = 0;
    private static final int INDEX_HOME = 1;
    private static final int INDEX_AWAY = 2;
    private static final int INDEX_HOME_GOALS = 3;
    private static final int INDEX_AWAY_GOALS = 4;
    private static final int INDEX_TIME = 5;
    private static final int INDEX_MATCH_ID = 6;

    private String[] todayDate = new String[1];

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }
                // This method is called by the app hosting the widget (the launcher)
                // However, the ContentProvider is not exported so it doesn't have access to the
                // data. Therefore we need to clear and restore the calling identity so
                // that calls use our process and permission
                final long identityToken = Binder.clearCallingIdentity();
                // Get today's data from the ContentProvider
                Uri scoreWithDateUri = DatabaseContract.scores_table.buildScoreWithDate();
                Date today = new Date(System.currentTimeMillis()+((0-PagerFragment.NUMBER_DAYS_QUERY)*86400000));
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                todayDate[0] = format.format(today);

                data = getContentResolver().query(scoreWithDateUri, SCORES_COLUMNS, null,
                        todayDate, null);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {

                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }

                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.widget_detail_list_item);

                String homeTeamName = data.getString(INDEX_HOME);
                int homeCrestResourceId = Utilies.getTeamCrestByTeamName(homeTeamName);
                String awayTeamName = data.getString(INDEX_AWAY);
                int awayCrestResourceId = Utilies.getTeamCrestByTeamName(awayTeamName);

                String score = Utilies.getScores(data.getInt(INDEX_HOME_GOALS), data.getInt(INDEX_AWAY_GOALS));
                String matchTime = data.getString(INDEX_TIME);
                Double matchId = data.getDouble(INDEX_MATCH_ID);

                // Add the data to the RemoteViews
                views.setImageViewResource(R.id.home_crest, homeCrestResourceId);
                views.setImageViewResource(R.id.away_crest, awayCrestResourceId);



                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    setRemoteContentDescription(views, homeTeamName, awayTeamName, score, matchTime);
                }
                views.setTextViewText(R.id.home_name, homeTeamName);
                views.setTextViewText(R.id.away_name, awayTeamName);
                views.setTextViewText(R.id.score_textview, score);
                views.setTextViewText(R.id.data_textview, matchTime);

                // Pass to the intent the information on the match selected, so the
                // app can display the appropriate details in its layout
                final Intent fillInIntent = new Intent();
                fillInIntent.putExtra(MATCH_ID_WIDGET, matchId.toString());
                views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);
                return views;
            }

            @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
            private void setRemoteContentDescription(RemoteViews views, String homeName, String awayName,
                                                     String score, String matchTime) {
                views.setContentDescription(R.id.home_name, getString(R.string.a11y_home_team, homeName));
                views.setContentDescription(R.id.away_name, getString(R.string.a11y_visiting_team, awayName));
                views.setContentDescription(R.id.score_textview, getString(R.string.a11y_score, score));
                views.setContentDescription(R.id.data_textview, getString(R.string.a11y_date, matchTime));
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_detail_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position))
                    return data.getLong(INDEX_SCORE_ID);
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}