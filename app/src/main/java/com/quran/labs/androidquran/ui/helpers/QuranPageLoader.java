import android.content.Context;
import com.quran.labs.androidquran.common.Response;
import com.quran.data.di.ActivityScope;
import com.quran.labs.androidquran.util.QuranDisplayHelper;
import com.quran.labs.androidquran.util.QuranFileUtils;
import com.quran.labs.androidquran.util.QuranScreenInfo;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import timber.log.Timber;

import javax.inject.Inject;

@ActivityScope
public class QuranPageLoader {

    private final Context appContext;
    private final OkHttpClient okHttpClient;
    private final String imageWidth;
    private final QuranScreenInfo quranScreenInfo;
    private final QuranFileUtils quranFileUtils;

    @Inject
    public QuranPageLoader(
            Context appContext,
            OkHttpClient okHttpClient,
            String imageWidth,
            QuranScreenInfo quranScreenInfo,
            QuranFileUtils quranFileUtils) {
        this.appContext = appContext;
        this.okHttpClient = okHttpClient;
        this.imageWidth = imageWidth;
        this.quranScreenInfo = quranScreenInfo;
        this.quranFileUtils = quranFileUtils;
    }

    private Response loadImage(int pageNumber) {
        Response response = null;
        OutOfMemoryError oom = null;
        try {
            response = QuranDisplayHelper.getQuranPage(
                    okHttpClient, appContext, imageWidth, pageNumber, quranFileUtils
            );
        } catch (OutOfMemoryError me) {
            Timber.w("out of memory exception loading page %d, %s", pageNumber, imageWidth);
            oom = me;
        }

        if (response == null ||
                (response.getBitmap() == null &&
                        response.getErrorCode() != Response.ERROR_SD_CARD_NOT_FOUND)) {
            if (quranScreenInfo.isDualPageMode()) {
                Timber.w("tablet got bitmap null, trying alternate width...");

                String param = quranScreenInfo.getWidthParam();
                if (param.equals(imageWidth)) {
                    param = quranScreenInfo.getTabletWidthParam();
                }

                response = QuranDisplayHelper.getQuranPage(
                        okHttpClient, appContext, param, pageNumber, quranFileUtils
                );

                if (response.getBitmap() == null) {
                    Timber.w("bitmap still null, giving up... [%d]", response.getErrorCode());
                }
            }
            Timber.w("got response back as null... [%d]", response != null ? response.getErrorCode() : -1);
        }

        if ((response == null || response.getBitmap() == null) && oom != null) {
            throw oom;
        }
        response.setPageData(pageNumber);
        return response;
    }

    public Observable<Response> loadPages(int[] pages) {
        return Observable.fromArray(pages)
                .flatMap(page -> Observable.fromCallable(() -> loadImage(page)))
                .subscribeOn(Schedulers.io());
    }
}
