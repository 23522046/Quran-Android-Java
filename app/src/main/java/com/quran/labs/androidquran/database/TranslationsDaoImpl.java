import android.content.Context;
import android.database.Cursor;
import com.quran.data.dao.TranslationsDao;
import com.quran.data.model.QuranText;
import com.quran.data.model.VerseRange;
import com.quran.labs.androidquran.data.QuranDataProvider;
import com.quran.labs.androidquran.database.DatabaseHandler;
import com.quran.labs.androidquran.util.QuranFileUtils;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.withContext;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class TranslationsDaoImpl implements TranslationsDao {
    private final Context appContext;
    private final QuranFileUtils quranFileUtils;

    @Inject
    public TranslationsDaoImpl(Context appContext, QuranFileUtils quranFileUtils) {
        this.appContext = appContext;
        this.quranFileUtils = quranFileUtils;
    }

    @Override
    public List<QuranText> allAyahs() {
        return withContext(Dispatchers.IO, () -> {
            DatabaseHandler databaseHandler = DatabaseHandler.getDatabaseHandler(
                    appContext, QuranDataProvider.QURAN_ARABIC_DATABASE, quranFileUtils);
            Cursor cursor = databaseHandler.getVerses(new VerseRange(1, 1, 114, 6, -1), DatabaseHandler.TextType.TRANSLATION);
            List<QuranText> result = new ArrayList<>();
            if (cursor != null) {
                try {
                    while (cursor.moveToNext()) {
                        int sura = cursor.getInt(1);
                        int ayah = cursor.getInt(2);
                        String text = cursor.getString(3);

                        QuranText quranText = new QuranText(sura, ayah, text, null);
                        result.add(quranText);
                    }
                } finally {
                    cursor.close();
                }
            }
            return result;
        });
    }
}
