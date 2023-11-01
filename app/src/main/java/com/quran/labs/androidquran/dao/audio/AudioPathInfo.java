import android.os.Parcel;
import android.os.Parcelable;

@Parcelize
public class AudioPathInfo implements Parcelable {
    private final String urlFormat;
    private final String localDirectory;
    private final String gaplessDatabase;

    public AudioPathInfo(String urlFormat, String localDirectory, String gaplessDatabase) {
        this.urlFormat = urlFormat;
        this.localDirectory = localDirectory;
        this.gaplessDatabase = gaplessDatabase;
    }

    public String getUrlFormat() {
        return urlFormat;
    }

    public String getLocalDirectory() {
        return localDirectory;
    }

    public String getGaplessDatabase() {
        return gaplessDatabase;
    }

    protected AudioPathInfo(Parcel in) {
        urlFormat = in.readString();
        localDirectory = in.readString();
        gaplessDatabase = in.readString();
    }

    public static final Creator<AudioPathInfo> CREATOR = new Creator<AudioPathInfo>() {
        @Override
        public AudioPathInfo createFromParcel(Parcel in) {
            return new AudioPathInfo(in);
        }

        @Override
        public AudioPathInfo[] newArray(int size) {
            return new AudioPathInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(urlFormat);
        dest.writeString(localDirectory);
        dest.writeString(gaplessDatabase);
    }
}
