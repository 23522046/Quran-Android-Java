import android.graphics.Bitmap;
import androidx.annotation.StringRes;
import com.quran.page.common.data.AyahCoordinates;
import com.quran.page.common.data.PageCoordinates;

public interface QuranPageScreen {
    void setPageCoordinates(PageCoordinates pageCoordinates);
    void setAyahCoordinatesError();
    void setPageBitmap(int page, Bitmap pageBitmap);
    void hidePageDownloadError();
    void setPageDownloadError(@StringRes int errorMessage);
    void setAyahCoordinatesData(AyahCoordinates coordinates);
}
