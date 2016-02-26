package it.jaschke.alexandria;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;

/**
 * Created by alvarpao on 2/24/2016.
 */
public class Book implements Parcelable{

    private String ean;
    private String title;
    private String subtitle;
    private String description;
    private String imgURL;
    private ArrayList<Author> authors;
    private ArrayList<Category> categories;

    public Book(String ean, String title, String subtitle, String description, String imgURL,
                ArrayList<Author> authors, ArrayList<Category> categories){

        this.ean = ean;
        this.title = title;
        this.subtitle = subtitle;
        this.description = description;
        this.imgURL = imgURL;
        this.authors = authors;
        this.categories = categories;
    }


    public Book(Parcel in){
        ean = in.readString();
        title = in.readString();
        subtitle = in.readString();
        description = in.readString();
        imgURL = in.readString();
        authors = (ArrayList<Author>) in.readArrayList(Author.class.getClassLoader());
        categories = (ArrayList<Category>) in.readArrayList(Category.class.getClassLoader());
    }

    public void writeToParcel(Parcel parcel, int content){
        parcel.writeString(ean);
        parcel.writeString(title);
        parcel.writeString(subtitle);
        parcel.writeString(description);
        parcel.writeString(imgURL);
        parcel.writeList(authors);
        parcel.writeList(categories);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<Book> CREATOR = new Parcelable.Creator<Book>() {
        @Override
        public Book createFromParcel(Parcel parcel) {
            return new Book(parcel);
        }

        @Override
        public Book[] newArray(int i) {
            return new Book[i];
        }

    };

    public String getEan(){ return ean; }
    public String getTitle(){ return title; }
    public String getSubtitle(){ return subtitle; }
    public String getDescription(){ return description; }
    public String getImgURL(){ return imgURL; }
    public ArrayList<Author> getAuthors(){ return authors; }
    public ArrayList<Category> getCategories(){ return categories; }

}
