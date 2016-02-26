package it.jaschke.alexandria.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import it.jaschke.alexandria.Author;
import it.jaschke.alexandria.Book;
import it.jaschke.alexandria.Category;
import it.jaschke.alexandria.MainActivity;
import it.jaschke.alexandria.R;
import it.jaschke.alexandria.Utility;
import it.jaschke.alexandria.data.AlexandriaContract;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 */
public class BookService extends IntentService {

    private final String LOG_TAG = BookService.class.getSimpleName();

    public static final String FETCH_BOOK = "it.jaschke.alexandria.services.action.FETCH_BOOK";
    public static final String DELETE_BOOK = "it.jaschke.alexandria.services.action.DELETE_BOOK";
    public static final String ADD_BOOK = "it.jaschke.alexandria.services.action.ADD_BOOK";
    public static final String EXTRA_RESULT = "EXTRA_RESULT";
    public static final String ACTION_BOOK_SERVICE = "it.jaschke.alexandria.bookservice.RESPONSE";
    public static final String EAN = "it.jaschke.alexandria.services.extra.EAN";
    public static final String EXTRA_BOOK = "it.jaschke.alexandria.services.extra.BOOK";

    public BookService() {
        super("Alexandria");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();

            if (FETCH_BOOK.equals(action)) {
                final String ean = intent.getStringExtra(EAN);
                Book book = fetchBook(ean);

                // **PAA** Return result
                Intent intentResponse = new Intent();
                intentResponse.setAction(ACTION_BOOK_SERVICE);
                intentResponse.addCategory(Intent.CATEGORY_DEFAULT);
                intentResponse.putExtra(EXTRA_RESULT, book);
                sendBroadcast(intentResponse);
            }

            else if (DELETE_BOOK.equals(action)) {
                final String ean = intent.getStringExtra(EAN);
                deleteBook(ean);
            }

            // **PAA** Defining AddBook service (separate from fetch book service)
            else if (ADD_BOOK.equals(action)) {
                final Book bookToAdd = intent.getParcelableExtra(EXTRA_BOOK);
                addBook(bookToAdd);
            }

        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void deleteBook(String ean) {
        if(ean!=null) {
            getContentResolver().delete(AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(ean)), null, null);
        }
    }

    // **PAA** Defining a separate service that adds a book to the database
    private void addBook(Book book){

        if (book != null && book.getEan().length() == 13) {

            // **PAA** Checking if the book already exists in the database so we don't try to insert
            // it again
            Cursor bookEntry = getContentResolver().query(
                    AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(book.getEan())),
                    null, // leaving "columns" null just returns all the columns.
                    null, // cols for "where" clause
                    null, // values for "where" clause
                    null  // sort order
            );

            if (bookEntry.getCount() > 0) {
                bookEntry.close();
                return;
            }

            bookEntry.close();

            // **PAA** Add book and respective authors and categories to the database
            writeBackBook(book.getEan(), book.getTitle(), book.getSubtitle(), book.getDescription(),
                    book.getImgURL());
            writeBackAuthors(book.getEan(), book.getAuthors());
            writeBackCategories(book.getEan(), book.getCategories());

        }

    }

    /**
     * Handle action fetchBook in the provided background thread with the provided
     * parameters.
     */
    private Book fetchBook(String ean) {


        if(ean.length()!=13){
            return null;
        }

        /*
        Cursor bookEntry = getContentResolver().query(
                AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(ean)),
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );

        if(bookEntry.getCount()>0){
            bookEntry.close();
            return null;
        }

        bookEntry.close();
        */

        if(Utility.deviceIsConnected(getApplicationContext())) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String bookJsonString = null;

            try {
                final String FORECAST_BASE_URL = "https://www.googleapis.com/books/v1/volumes?";
                final String QUERY_PARAM = "q";

                final String ISBN_PARAM = "isbn:" + ean;

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, ISBN_PARAM)
                        .build();

                URL url = new URL(builtUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return null;
                }

                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                    buffer.append("\n");
                }

                if (buffer.length() == 0) {
                    return null;
                }
                bookJsonString = buffer.toString();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error ", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }

            }

            final String ITEMS = "items";

            final String VOLUME_INFO = "volumeInfo";

            final String TITLE = "title";
            final String SUBTITLE = "subtitle";
            final String AUTHORS = "authors";
            final String DESC = "description";
            final String CATEGORIES = "categories";
            final String IMG_URL_PATH = "imageLinks";
            final String IMG_URL = "thumbnail";

            try {
                JSONObject bookJson = new JSONObject(bookJsonString);
                JSONArray bookArray;
                if (bookJson.has(ITEMS)) {
                    bookArray = bookJson.getJSONArray(ITEMS);
                } else {
                    Intent messageIntent = new Intent(MainActivity.MESSAGE_EVENT);
                    messageIntent.putExtra(MainActivity.MESSAGE_KEY, getResources().getString(R.string.not_found));
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);
                    return null;
                }

                JSONObject bookInfo = ((JSONObject) bookArray.get(0)).getJSONObject(VOLUME_INFO);

                String title = bookInfo.getString(TITLE);

                String subtitle = "";
                if (bookInfo.has(SUBTITLE)) {
                    subtitle = bookInfo.getString(SUBTITLE);
                }

                String desc = "";
                if (bookInfo.has(DESC)) {
                    desc = bookInfo.getString(DESC);
                }

                String imgUrl = "";
                if (bookInfo.has(IMG_URL_PATH) && bookInfo.getJSONObject(IMG_URL_PATH).has(IMG_URL)) {
                    imgUrl = bookInfo.getJSONObject(IMG_URL_PATH).getString(IMG_URL);
                }

                // **PAA** Don't add the book by default just after fetching it. Allow the user
                // to confirm the add by clicking the button "Add" or click "Cancel"

                ArrayList<Author> authors = new ArrayList<>();
                ArrayList<Category> categories = new ArrayList<>();

                if (bookInfo.has(AUTHORS))
                    authors = getAuthors(bookInfo.getJSONArray(AUTHORS));

                if (bookInfo.has(CATEGORIES))
                    categories = getCategories(bookInfo.getJSONArray(CATEGORIES));

                return new Book(ean, title, subtitle, desc, imgUrl, authors,
                        categories);

                /*
                writeBackBook(ean, title, subtitle, desc, imgUrl);

                if (bookInfo.has(AUTHORS)) {
                    writeBackAuthors(ean, bookInfo.getJSONArray(AUTHORS));
                }
                if (bookInfo.has(CATEGORIES)) {
                    writeBackCategories(ean, bookInfo.getJSONArray(CATEGORIES));
                }
                */

            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error ", e);
            }
        }

        else {

            // Since this service runs on a background thread in order to display a message
            // to the user to inform them that they don't have an internet connection a handler
            // that runs on the main thread and displays a toast is necessary
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), getString(R.string.no_internet_error),
                            Toast.LENGTH_LONG).show();
                }
            });
        }

        return null;
    }

    private ArrayList<Author> getAuthors(JSONArray authors) throws JSONException{

        ArrayList<Author> authorsList = new ArrayList<>();
        for (int i = 0; i < authors.length(); i++) {
            if (authors.getString(i) != null){
                String[] authorsArray = authors.getString(i).split(",");
                for(int index=0; index<authorsArray.length; index++)
                  authorsList.add(new Author(authorsArray[index]));
            }
        }

        return authorsList;
    }

    private ArrayList<Category> getCategories(JSONArray categories) throws JSONException{

        ArrayList<Category> categoriesList = new ArrayList<>();
        for (int i = 0; i < categories.length(); i++) {
            if (categories.getString(i) != null){
                String[] categoriesArray = categories.getString(i).split(",");
                for(int index=0; index<categoriesArray.length; index++)
                    categoriesList.add(new Category(categoriesArray[index]));
            }
        }

        return categoriesList;
    }

    private void writeBackBook(String ean, String title, String subtitle, String desc, String imgUrl) {
        ContentValues values= new ContentValues();
        values.put(AlexandriaContract.BookEntry._ID, ean);
        values.put(AlexandriaContract.BookEntry.TITLE, title);
        values.put(AlexandriaContract.BookEntry.IMAGE_URL, imgUrl);
        values.put(AlexandriaContract.BookEntry.SUBTITLE, subtitle);
        values.put(AlexandriaContract.BookEntry.DESC, desc);
        getContentResolver().insert(AlexandriaContract.BookEntry.CONTENT_URI,values);
    }

    // **PAA** This method now reads from an ArrayList<Author> instead of a JSONArray
    private void writeBackAuthors(String ean, ArrayList<Author> authors) {

        if(authors != null) {
            ContentValues values = new ContentValues();
            for (int i = 0; i < authors.size(); i++) {
                values.put(AlexandriaContract.AuthorEntry._ID, ean);
                values.put(AlexandriaContract.AuthorEntry.AUTHOR, (authors.get(i)).getName());
                getContentResolver().insert(AlexandriaContract.AuthorEntry.CONTENT_URI, values);
                values = new ContentValues();
            }
        }
    }

    // **PAA** This method now reads from an ArrayList<Category> instead of a JSONArray
    private void writeBackCategories(String ean, ArrayList<Category> categories){

        if(categories != null) {
            ContentValues values = new ContentValues();
            for (int i = 0; i < categories.size(); i++) {
                values.put(AlexandriaContract.CategoryEntry._ID, ean);
                values.put(AlexandriaContract.CategoryEntry.CATEGORY, (categories.get(i)).getName());
                getContentResolver().insert(AlexandriaContract.CategoryEntry.CONTENT_URI, values);
                values = new ContentValues();
            }
        }
    }
 }