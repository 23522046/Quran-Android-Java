import android.database.Cursor;
import android.database.DefaultDatabaseErrorHandler;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import timber.log.Timber;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SuraTimingDatabaseHandler {
    private SQLiteDatabase database;

    public static class TimingsTable {
        public static final String TABLE_NAME = "timings";
        public static final String COL_SURA = "sura";
        public static final String COL_AYAH = "ayah";
        public static final String COL_TIME = "time";
    }

    public static class PropertiesTable {
        public static final String TABLE_NAME = "properties";
        public static final String COL_PROPERTY = "property";
        public static final String COL_VALUE = "value";
    }

    private static final Map<String, SuraTimingDatabaseHandler> databaseMap = new HashMap<>();

    public static synchronized SuraTimingDatabaseHandler getDatabaseHandler(String path) {
        SuraTimingDatabaseHandler handler = databaseMap.get(path);
        if (handler == null) {
            handler = new SuraTimingDatabaseHandler(path);
            databaseMap.put(path, handler);
        }
        return handler;
    }

    public static synchronized void clearDatabaseHandlerIfExists(String databasePath) {
        try {
            SuraTimingDatabaseHandler handler = databaseMap.remove(databasePath);
            if (handler != null) {
                handler.closeDatabase();
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private SuraTimingDatabaseHandler(String path) {
        Timber.d("opening gapless data file, %s", path);
        try {
            database = SQLiteDatabase.openDatabase(
                    path, null,
                    SQLiteDatabase.NO_LOCALIZED_COLLATORS, new DefaultDatabaseErrorHandler()
            );
        } catch (SQLiteDatabaseCorruptException sce) {
            Timber.d("database corrupted: %s", path);
            database = null;
        } catch (SQLException se) {
            Timber.d("database at %s %s", path, (new File(path)).exists() ? "exists" : "doesn't exist");
            Timber.e(se);
            database = null;
        }
    }

    private boolean validDatabase() {
        return database != null && database.isOpen();
    }

    public Cursor getAyahTimings(int sura) {
        if (!validDatabase()) {
            return null;
        }

        try {
            return database.query(
                    TimingsTable.TABLE_NAME,
                    new String[]{TimingsTable.COL_SURA, TimingsTable.COL_AYAH, TimingsTable.COL_TIME},
                    TimingsTable.COL_SURA + "=" + sura,
                    null, null, null, TimingsTable.COL_AYAH + " ASC"
            );
        } catch (Exception e) {
            return null;
        }
    }

    public int getVersion() {
        if (!validDatabase()) {
            return -1;
        }
        Cursor cursor = null;
        try {
            cursor = database.query(
                    PropertiesTable.TABLE_NAME,
                    new String[]{PropertiesTable.COL_VALUE},
                    PropertiesTable.COL_PROPERTY + "= 'version'",
                    null, null, null, null
            );
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0);
            } else {
                return 1;
            }
        } catch (Exception e) {
            return 1;
        } finally {
            DatabaseUtils.closeCursor(cursor);
        }
    }

    public void closeDatabase() {
        if (database != null) {
            database.close();
        }
    }
}
