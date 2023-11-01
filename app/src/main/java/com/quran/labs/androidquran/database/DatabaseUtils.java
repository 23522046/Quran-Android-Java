import android.database.Cursor;

public class DatabaseUtils {
    public static void closeCursor(Cursor cursor) {
        try {
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            // Handle or log the exception as needed
        }
    }
}
