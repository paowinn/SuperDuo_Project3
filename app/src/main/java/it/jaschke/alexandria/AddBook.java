package it.jaschke.alexandria;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import it.jaschke.alexandria.services.BookService;
import it.jaschke.alexandria.services.DownloadImage;


public class AddBook extends Fragment {
    private static final String TAG = "INTENT_TO_SCAN_ACTIVITY";
    private EditText ean;
    private final int LOADER_ID = 1;
    private View rootView;
    private final String EAN_CONTENT="eanContent";
    private static final String SCAN_FORMAT = "scanFormat";
    private static final String SCAN_CONTENTS = "scanContents";

    private String mScanFormat = "Format:";
    private String mScanContents = "Contents:";

    private Book book = null;
    private ServiceBroadcastReceiver serviceBroadcastReceiver;

    // **PAA** Request code for BarcodeScannerActivity **
    static int SCAN_BOOK = 1;

    public AddBook(){
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(ean!=null) {
            outState.putString(EAN_CONTENT, ean.getText().toString());
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_add_book, container, false);
        ean = (EditText) rootView.findViewById(R.id.ean);
        serviceBroadcastReceiver = new ServiceBroadcastReceiver();

        // **PAA** Register ServiceBroadcastReceiver to receive result (from fetching book service)
        IntentFilter intentFilter = new IntentFilter(BookService.ACTION_BOOK_SERVICE);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        getActivity().registerReceiver(serviceBroadcastReceiver, intentFilter);

        ean.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //no need
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //no need
            }

            @Override
            public void afterTextChanged(Editable s) {
                String ean =s.toString();
                //catch isbn10 numbers
                if(ean.length()==10 && !ean.startsWith("978")){
                    ean="978"+ean;
                }
                if(ean.length()<13){
                    clearFields();
                    return;
                }

                // **PAA** Once we have an ISBN, fetch the book
                fetchBook(ean);

            }
        });

        rootView.findViewById(R.id.scan_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // **PAA** Start barcode scanner **
                startActivityForResult(new Intent(getActivity(), BarcodeScannerActivity.class),
                        SCAN_BOOK);
            }
        });

        rootView.findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ean.setText("");
            }
        });

        rootView.findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, ean.getText().toString());
                bookIntent.setAction(BookService.DELETE_BOOK);
                getActivity().startService(bookIntent);
                ean.setText("");
            }
        });

        if(savedInstanceState!=null){
            ean.setText(savedInstanceState.getString(EAN_CONTENT));
            ean.setHint("");
        }

        return rootView;
    }

    /*
    private void restartLoader(){
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }
    */

    private void fetchBook(String ean){

        // **PAA** Start service to fetch a book once we have the book's ISBN-13
        Intent bookIntent = new Intent(getActivity(), BookService.class);
        bookIntent.putExtra(BookService.EAN, ean);
        bookIntent.setAction(BookService.FETCH_BOOK);
        getActivity().startService(bookIntent);
        // **PAA** The views in this fragment will be updated with the book info obtained from the
        // fetch book service, therefore a loader is not needed since the book hasn't been inserted
        // into the database yet
        //AddBook.this.restartLoader();
    }

    // **PAA** Define a broadcast receiver to get the result from the BookService (when fetching
    // book
    public class ServiceBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            book = intent.getParcelableExtra(BookService.EXTRA_RESULT);
            Toast.makeText(getActivity(), "Receive Broadcast", Toast.LENGTH_SHORT).show();
            // Since the book will not be inserted into the database until the user confirms it,
            // the views in this fragment will be updated with the book info obtained from the
            // fetch book service, therefore a loader is not needed
            updateViews();
        }
    }

    private void updateViews(){

        if(book != null)
            Toast.makeText(getActivity(), "BOOK NO ES NULO", Toast.LENGTH_SHORT).show();

        else
            Toast.makeText(getActivity(), "Book is NULL", Toast.LENGTH_SHORT).show();

        if(book != null) {

            ((TextView) rootView.findViewById(R.id.bookTitle)).setText(book.getTitle());
            ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText(book.getSubtitle());

            ArrayList<Author> authors = book.getAuthors();
            ((TextView) rootView.findViewById(R.id.authors)).setLines(authors.size());
            String authorsText= "";
            for(int index=0; index<authors.size(); index++)
                authorsText += (authors.get(index)).getName() + "\n";

            ((TextView) rootView.findViewById(R.id.authors)).setText(authorsText);

            String imgURL = book.getImgURL();
            if (Patterns.WEB_URL.matcher(imgURL).matches()) {
                new DownloadImage((ImageView) rootView.findViewById(R.id.bookCover)).execute(imgURL);
                rootView.findViewById(R.id.bookCover).setVisibility(View.VISIBLE);
            }

            ArrayList<Category> categories = book.getCategories();
            ((TextView) rootView.findViewById(R.id.categories)).setLines(categories.size());
            String categoriesText= "";
            for(int index=0; index<categories.size(); index++)
                categoriesText += (categories.get(index)).getName() + "\n";

            ((TextView) rootView.findViewById(R.id.categories)).setText(categoriesText);

            rootView.findViewById(R.id.save_button).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.delete_button).setVisibility(View.VISIBLE);
        }
    }

    /*
    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if(ean.getText().length()==0){
            return null;
        }
        String eanStr= ean.getText().toString();
        if(eanStr.length()==10 && !eanStr.startsWith("978")){
            eanStr="978"+eanStr;
        }
        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(eanStr)),
                null,
                null,
                null,
                null
        );
    }
    */

    /*
    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) {
            return;
        }

        String bookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        ((TextView) rootView.findViewById(R.id.bookTitle)).setText(bookTitle);

        String bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText(bookSubTitle);

        String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));
        String[] authorsArr = authors.split(",");
        ((TextView) rootView.findViewById(R.id.authors)).setLines(authorsArr.length);
        ((TextView) rootView.findViewById(R.id.authors)).setText(authors.replace(",", "\n"));
        String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));
        if(Patterns.WEB_URL.matcher(imgUrl).matches()){
            new DownloadImage((ImageView) rootView.findViewById(R.id.bookCover)).execute(imgUrl);
            rootView.findViewById(R.id.bookCover).setVisibility(View.VISIBLE);
        }

        String categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));
        ((TextView) rootView.findViewById(R.id.categories)).setText(categories);

        rootView.findViewById(R.id.save_button).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.delete_button).setVisibility(View.VISIBLE);
    }

    */

    /*
    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }
    */

    private void clearFields(){
        ((TextView) rootView.findViewById(R.id.bookTitle)).setText("");
        ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText("");
        ((TextView) rootView.findViewById(R.id.authors)).setText("");
        ((TextView) rootView.findViewById(R.id.categories)).setText("");
        rootView.findViewById(R.id.bookCover).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.save_button).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.delete_button).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.scan);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // **PAA** Retrieve book's ISBN-13 barcode obtained from BarcodeScannerActivity
        if((resultCode == Activity.RESULT_FIRST_USER) && requestCode == SCAN_BOOK){

            String barcode = data.getStringExtra(BarcodeScannerActivity.EXTRA_ISBN_13);
            // **PAA** Set the EditText view's text with the book's barcode obtained from the
            // scanner
            ean.setText(barcode);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        getActivity().unregisterReceiver(serviceBroadcastReceiver);
    }
}
