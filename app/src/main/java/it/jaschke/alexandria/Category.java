package it.jaschke.alexandria;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by alvarpao on 2/24/2016.
 */
public class Category implements Parcelable{

    private String name;

    public Category(String name){
        this.name = name;
    }

    public Category(Parcel in){
        name = in.readString();
    }

    public void writeToParcel(Parcel parcel, int content){
        parcel.writeString(name);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<Category> CREATOR = new Parcelable.Creator<Category>() {
        @Override
        public Category createFromParcel(Parcel parcel) {
            return new Category(parcel);
        }

        @Override
        public Category[] newArray(int i) {
            return new Category[i];
        }
    };

    public String getName(){ return name; }
}
