import android.content.Context;
import com.quran.data.di.ActivityScope;
import com.quran.labs.androidquran.util.QuranFileUtils;
import javax.inject.Inject;

@ActivityScope
public class AyahInfoDatabaseProvider {

    private final Context context;
    private final String widthParameter;
    private final QuranFileUtils quranFileUtils;
    private AyahInfoDatabaseHandler databaseHandler;

    @Inject
    public AyahInfoDatabaseProvider(Context context, String widthParameter, QuranFileUtils quranFileUtils) {
        this.context = context;
        this.widthParameter = widthParameter;
        this.quranFileUtils = quranFileUtils;
    }

    public AyahInfoDatabaseHandler getAyahInfoHandler() {
        if (databaseHandler == null) {
            String filename = quranFileUtils.getAyaPositionFileName(widthParameter);
            databaseHandler = AyahInfoDatabaseHandler.getAyahInfoDatabaseHandler(
                    context,
                    filename,
                    quranFileUtils
            );
        }
        return databaseHandler;
    }

    public int getPageWidth() {
        return Integer.parseInt(widthParameter.substring(1));
    }
}
