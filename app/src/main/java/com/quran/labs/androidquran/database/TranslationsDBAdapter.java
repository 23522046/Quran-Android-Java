import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.SparseArray;

import com.quran.labs.androidquran.common.LocalTranslation;
import com.quran.labs.androidquran.dao.translation.TranslationItem;
import com.quran.labs.androidquran.database.TranslationsDBHelper.TranslationsTable;
import com.quran.labs.androidquran.util.QuranFileUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import timber.log.Timber;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TranslationsDBAdapter {
    private final Context context;
    private final QuranFileUtils quranFileUtils;
    private final SQLiteDatabase db;

    @Inject
    public TranslationsDBAdapter(Context context, TranslationsDBHelper adapter, QuranFileUtils quranFileUtils) {
        this.context = context;
        this.quranFileUtils = quranFileUtils;
        this.db = adapter.getWritableDatabase();
    }

    private volatile List<LocalTranslation> cachedTranslations = null;

    private long lastWriteTime = 0;

    public SparseArray<LocalTranslation> getTranslationsHash() {
        SparseArray<LocalTranslation> result = new SparseArray<>();
        for (LocalTranslation item : getTranslations()) {
            result.put(item.getId(), item);
        }
        return result;
    }

    @WorkerThread
    public List<LocalTranslation> getTranslations() {
        List<LocalTranslation> cached = cachedTranslations;
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        List<LocalTranslation> items = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.query(TranslationsTable.TABLE_NAME,
                    null, null, null, null, null,
                    TranslationsTable.ID + " ASC");

            while (cursor.moveToNext()) {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                String translator = cursor.getString(2);
                String translatorForeign = cursor.getString(3);
                String filename = cursor.getString(4);
                String url = cursor.getString(5);
                String languageCode = cursor.getString(6);
                int version = cursor.getInt(7);
                int minimumVersion = cursor.getInt(8);
                int displayOrder = cursor.getInt(9);

                if (quranFileUtils.hasTranslation(context, filename)) {
                    items.add(new LocalTranslation(id, filename, name, translator,
                            translatorForeign, url, languageCode, version, minimumVersion, displayOrder));
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        items = Collections.unmodifiableList(items);
        if (!items.isEmpty()) {
            cachedTranslations = items;
        }
        return items;
    }

    public void deleteTranslationByFile(String filename) {
        db.execSQL("DELETE FROM " + TranslationsTable.TABLE_NAME + " WHERE " +
                TranslationsTable.FILENAME + " = ?", new Object[]{filename});
    }

    public boolean writeTranslationUpdates(List<TranslationItem> updates) {
        boolean result = true;
        db.beginTransaction();

        try {
            int cachedNextOrder = -1;
            for (TranslationItem item : updates) {
                if (item.exists()) {
                    int displayOrder = 0;
                    LocalTranslation translation = item.getTranslation();
                    if (item.getDisplayOrder() > -1) {
                        displayOrder = item.getDisplayOrder();
                    } else {
                        Cursor cursor = null;
                        if (cachedNextOrder == -1) {
                            try {
                                cursor = db.query(
                                        TranslationsTable.TABLE_NAME, new String[]{TranslationsTable.DISPLAY_ORDER},
                                        null, null, null, null,
                                        TranslationsTable.DISPLAY_ORDER + " DESC",
                                        "1"
                                );
                                if (cursor != null && cursor.moveToFirst()) {
                                    cachedNextOrder = cursor.getInt(0) + 1;
                                    displayOrder = cachedNextOrder++;
                                }
                            } finally {
                                if (cursor != null) {
                                    cursor.close();
                                }
                            }
                        } else {
                            displayOrder = cachedNextOrder++;
                        }
                    }

                    ContentValues values = new ContentValues();
                    values.put(TranslationsTable.ID, translation.getId());
                    values.put(TranslationsTable.NAME, translation.getDisplayName());
                    values.put(TranslationsTable.TRANSLATOR, translation.getTranslator());
                    values.put(TranslationsTable.TRANSLATOR_FOREIGN,
                            translation.getTranslatorNameLocalized());
                    values.put(TranslationsTable.FILENAME, translation.getFileName());
                    values.put(TranslationsTable.URL, translation.getFileUrl());
                    values.put(TranslationsTable.LANGUAGE_CODE, translation.getLanguageCode());
                    values.put(TranslationsTable.VERSION, item.getLocalVersion());
                    values.put(TranslationsTable.MINIMUM_REQUIRED_VERSION, translation.getMinimumVersion());
                    values.put(TranslationsTable.DISPLAY_ORDER, displayOrder);

                    db.replace(TranslationsTable.TABLE_NAME, null, values);
                } else {
                    db.delete(TranslationsTable.TABLE_NAME,
                            TranslationsTable.ID + " = " + item.getTranslation().getId(), null);
                }
            }
            db.setTransactionSuccessful();

            lastWriteTime = System.currentTimeMillis();
            cachedTranslations = null;
        } catch (Exception e) {
            result = false;
            Timber.d(e, "error writing translation updates");
        } finally {
            db.endTransaction();
        }

        return result;
    }
}
