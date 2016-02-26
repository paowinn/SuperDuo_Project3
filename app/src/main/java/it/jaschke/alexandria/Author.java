package it.jaschke.alexandria;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by alvarpao on 2/24/2016.
 */
public class Author implements Parcelable{

    private String name;

    public Author(String name){
        this.name = name;
    }

    public Author(Parcel in){
        name = in.readString();
    }

    public void writeToParcel(Parcel parcel, int content){
        parcel.writeString(name);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<Author> CREATOR = new Parcelable.Creator<Author>() {
        @Override
        public Author createFromParcel(Parcel parcel) {
            return new Author(parcel);
        }

        @Override
        public Author[] newArray(int i) {
            return new Author[i];
        }

    };

    public String getName(){ return name; }
}
