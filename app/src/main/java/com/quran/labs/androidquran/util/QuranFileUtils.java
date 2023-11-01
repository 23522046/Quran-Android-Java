package com.quran.labs.androidquran.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import androidx.annotation.WorkerThread;
import com.quran.data.core.QuranFileManager;
import com.quran.data.source.PageProvider;
import com.quran.labs.androidquran.common.Response;
import com.quran.labs.androidquran.data.QuranDataProvider;
import com.quran.labs.androidquran.extension.ZipUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.ForwardingSource;
import okio.Source;
import timber.log.Timber;

import java.io.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class QuranFileUtils implements QuranFileManager {

    private final Context appContext;
    private final QuranScreenInfo quranScreenInfo;
    private final String imageBaseUrl;
    private final String imageZipBaseUrl;
    private final String patchBaseUrl;
    private final String databaseBaseUrl;
    private final String ayahInfoBaseUrl;
    private final String databaseDirectory;
    private final String audioDirectory;
    private final String ayahInfoDirectory;
    private final String imagesDirectory;
    private final boolean ayahInfoDbHasGlyphData;
    private final String gaplessDatabaseRootUrl;

    // Constructor
    public QuranFileUtils(Context context, PageProvider pageProvider, QuranScreenInfo quranScreenInfo) {
        this.appContext = context.getApplicationContext();
        this.quranScreenInfo = quranScreenInfo;

        this.imageBaseUrl = pageProvider.getImagesBaseUrl();
        this.imageZipBaseUrl = pageProvider.getImagesZipBaseUrl();
        this.patchBaseUrl = pageProvider.getPatchBaseUrl();
        this.databaseBaseUrl = pageProvider.getDatabasesBaseUrl();
        this.ayahInfoBaseUrl = pageProvider.getAyahInfoBaseUrl();

        this.databaseDirectory = pageProvider.getDatabaseDirectoryName();
        this.audioDirectory = pageProvider.getAudioDirectoryName();
        this.ayahInfoDirectory = pageProvider.getAyahInfoDirectoryName();
        this.imagesDirectory = pageProvider.getImagesDirectoryName();

        this.ayahInfoDbHasGlyphData = pageProvider.ayahInfoDbHasGlyphData();
        this.gaplessDatabaseRootUrl = pageProvider.getAudioDatabasesBaseUrl();
    }

    @Override
    @WorkerThread
    public boolean isVersion(String widthParam, int version) {
        return version <= 1 || hasVersionFile(appContext, widthParam, version);
    }

    private boolean hasVersionFile(Context context, String widthParam, int version) {
        String quranDirectory = getQuranImagesDirectory(context, widthParam);
        Timber.d("isVersion: checking if version %d exists for width %s at %s", version, widthParam, quranDirectory);

        if (quranDirectory == null) {
            return false;
        }

        try {
            File vFile = new File(quranDirectory + File.separator + ".v" + version);
            return vFile.exists();
        } catch (Exception e) {
            Timber.e(e, "isVersion: exception while checking version file");
            return false;
        }
    }

    @WorkerThread
    public String getPotentialFallbackDirectory(Context context, int totalPages) {
        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            if (haveAllImages(context, "_1920", totalPages, false)) {
                return "1920";
            }
        }
        return null;
    }

    @Override
    public String quranImagesDirectory() {
        return getQuranImagesDirectory(appContext);
    }

    @Override
    public String ayahInfoFileDirectory() {
        String base = quranAyahDatabaseDirectory();
        if (base != null) {
            String filename = ayaPositionFileName();
            return base + File.separator + filename;
        } else {
            return null;
        }
    }

    @WorkerThread
    @Override
    public void removeFilesForWidth(int width, String directoryLambda) {
        String widthParam = "_" + width;
        String quranDirectoryWithoutLambda = getQuranImagesDirectory(appContext, widthParam);
        if (quranDirectoryWithoutLambda == null) {
            return;
        }

        String quranDirectory = directoryLambda(quranDirectoryWithoutLambda);
        File file = new File(quranDirectory);

        if (file.exists()) {
            deleteFileOrDirectory(file);

            String ayahDatabaseDirectoryWithoutLambda = getQuranAyahDatabaseDirectory(appContext);
            if (ayahDatabaseDirectoryWithoutLambda != null) {
                String ayahDatabaseDirectory = directoryLambda(ayahDatabaseDirectoryWithoutLambda);
                File ayahinfoFile = new File(ayahDatabaseDirectory, "ayahinfo_" + width + ".db");

                if (ayahinfoFile.exists()) {
                    ayahinfoFile.delete();
                }
            }
        }
    }

    @WorkerThread
    @Override
    public void writeVersionFile(String widthParam, int version) {
        String quranDirectory = getQuranImagesDirectory(appContext, widthParam);
        if (quranDirectory != null) {
            File file = new File(quranDirectory, ".v" + version);
            try {
                file.createNewFile();
            } catch (IOException e) {
                Timber.e(e, "Error creating version file");
            }
        }
    }

    @WorkerThread
    @Override
    public void writeNoMediaFileRelative(String widthParam) {
        String quranDirectory = getQuranImagesDirectory(appContext, widthParam);
        if (quranDirectory != null) {
            writeNoMediaFile(quranDirectory);
        }
    }

    public boolean haveAllImages(Context context, String widthParam, int totalPages, boolean makeDirectory) {
        String quranDirectory = getQuranImagesDirectory(context, widthParam);
        Timber.d("haveAllImages: for width %s, directory is: %s", widthParam, quranDirectory);

        if (quranDirectory == null) {
            return false;
        }

        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            File dir = new File(quranDirectory + File.separator);

            if (dir.isDirectory()) {
                Timber.d("haveAllImages: media state is mounted and directory exists");
                String[] fileList = dir.list();

                if (fileList == null) {
                    Timber.d("haveAllImages: null fileList, checking page by page...");

                    for (int i = 1; i <= totalPages; i++) {
                        String name = getPageFileName(i);
                        if (!new File(dir, name).exists()) {
                            Timber.d("haveAllImages: couldn't find page %d", i);
                            return false;
                        }
                    }
                } else if (fileList.length < totalPages) {
                    Timber.d("haveAllImages: found %d files instead of %d.", fileList.length, totalPages);
                    return false;
                }

                return true;
            } else {
                Timber.d("haveAllImages: couldn't find the directory, so %s",
                        makeDirectory ? "making it instead." : "doing nothing.");

                if (makeDirectory) {
                    makeQuranDirectory(context, widthParam);
                }
            }
        }

        return false;
    }

    private boolean isSDCardMounted() {
        String state = Environment.getExternalStorageState();
        return state.equals(Environment.MEDIA_MOUNTED);
    }

    public Response getImageFromSD(Context context, String widthParam, String filename) {
        String location = widthParam != null ? getQuranImagesDirectory(context, widthParam) : getQuranImagesDirectory(context);
        if (location == null) {
            return new Response(Response.ERROR_SD_CARD_NOT_FOUND);
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ALPHA_8;

        Bitmap bitmap = BitmapFactory.decodeFile(location + File.separator + filename, options);

        return bitmap != null ? new Response(bitmap) : new Response(Response.ERROR_FILE_NOT_FOUND);
    }

    private boolean writeNoMediaFile(String parentDir) {
        File file = new File(parentDir, ".nomedia");

        if (file.exists()) {
            return true;
        } else {
            try {
                return file.createNewFile();
            } catch (IOException e) {
                return false;
            }
        }
    }

    public boolean makeQuranDirectory(Context context, String widthParam) {
        String path = getQuranImagesDirectory(context, widthParam);

        if (path != null) {
            File directory = new File(path);
            return (directory.exists() && directory.isDirectory) || (directory.mkdirs() && writeNoMediaFile(path));
        }

        return false;
    }

    private boolean makeDirectory(String path) {
        if (path == null) {
            return false;
        }

        File directory = new File(path);
        return directory.exists() && directory.isDirectory || directory.mkdirs();
    }

    private boolean makeQuranDatabaseDirectory(Context context) {
        return makeDirectory(getQuranDatabaseDirectory(context));
    }

    private boolean makeQuranAyahDatabaseDirectory(Context context) {
        return makeQuranDatabaseDirectory(context) && makeDirectory(getQuranAyahDatabaseDirectory(context));
    }

    @WorkerThread
    @Override
    public void copyFromAssetsRelative(String assetsPath, String filename, String destination) {
        String actualDestination = getQuranBaseDirectory(appContext) + destination;
        File dir = new File(actualDestination);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        copyFromAssets(assetsPath, filename, actualDestination);
    }

    @Override
    public void copyFromAssetsRelativeRecursive(String assetsPath, String directory, String destination) {
        File destinationPath = new File(getQuranBaseDirectory(appContext) + destination);
        File directoryDestinationPath = new File(destinationPath, directory);
        if (!directoryDestinationPath.exists()) {
            directoryDestinationPath.mkdirs();
        }

        String assets[] = new File(appContext.getAssets(), assetsPath).list();
        if (assets == null) {
            assets = new String[0];
        }

        String destinationDirectory = destination + File.separator + directory;

        for (String asset : assets) {
            String path = assetsPath + File.separator + asset;

            if (new File(appContext.getAssets(), path).list().length > 0) {
                copyFromAssetsRelativeRecursive(path, asset, destinationDirectory);
            } else {
                copyFromAssetsRelative(path, asset, destinationDirectory);
            }
        }
    }

    @WorkerThread
    @Override
    public boolean removeOldArabicDatabase() {
        File databaseQuranArabicDatabase = new File(getQuranDatabaseDirectory(appContext),
                QuranDataProvider.QURAN_ARABIC_DATABASE);

        return databaseQuranArabicDatabase.exists() && databaseQuranArabicDatabase.delete();
    }

    @WorkerThread
    private void copyFromAssets(String assetsPath, String filename, String destination) {
        try {
            InputStream input = appContext.getAssets().open(assetsPath);
            FileOutputStream output = new FileOutputStream(new File(destination, filename));

            byte[] buffer = new byte[1024];
            int length;

            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }

            input.close();
            output.close();

            if (filename.endsWith(".zip")) {
                String zipFile = destination + File.separator + filename;
                ZipUtils.unzipFile(zipFile, destination, filename, null);
                // delete the zip file, since there's no need to have it twice
                new File(zipFile).delete();
            }
        } catch (IOException e) {
            Timber.e(e, "Error copying file from assets");
        }
    }

    public Response getImageFromWeb(OkHttpClient okHttpClient, Context context, String widthParam, String filename) {
        return getImageFromWeb(okHttpClient, context, widthParam, filename, false);
    }

    private Response getImageFromWeb(OkHttpClient okHttpClient, Context context, String widthParam, String filename, boolean isRetry) {
        String base = imageBaseUrl;
        String urlString = base + "width" + widthParam + File.separator + filename;
        Timber.d("want to download: %s", urlString);
        Request request = new Request.Builder().url(urlString).build();
        okhttp3.Call call = okHttpClient.newCall(request);
        ResponseBody responseBody = null;

        try {
            okhttp3.Response response = call.execute();
            if (response.isSuccessful()) {
                responseBody = response.body();

                if (responseBody != null) {
                    // handling for BitmapFactory.decodeStream not throwing an error
                    // when the download is interrupted or an exception occurs.
                    // This is taken from both Glide and Picasso.
                    ExceptionCatchingSource exceptionCatchingSource = new ExceptionCatchingSource(responseBody.source());
                    Buffer bufferedSource = exceptionCatchingSource.buffer();
                    Bitmap bitmap = decodeBitmapStream(bufferedSource.inputStream());
                    exceptionCatchingSource.throwIfCaught();

                    if (bitmap != null) {
                        String path = getQuranImagesDirectory(context, widthParam);
                        int warning = Response.WARN_SD_CARD_NOT_FOUND;

                        if (path != null && makeQuranDirectory(context, widthParam)) {
                            path += File.separator + filename;
                            warning = tryToSaveBitmap(bitmap, path) ? 0 : Response.WARN_COULD_NOT_SAVE_FILE;
                        }

                        return new Response(bitmap, warning);
                    }
                }
            }
        } catch (InterruptedIOException iioe) {
            // do nothing, this is expected if the job is canceled
        } catch (IOException ioe) {
            Timber.e(ioe, "exception downloading file");
        } finally {
            responseBody.closeQuietly();
        }

        return isRetry ? new Response(Response.ERROR_DOWNLOADING_ERROR) : getImageFromWeb(okHttpClient, context, filename, widthParam, true);
    }

    private Bitmap decodeBitmapStream(InputStream is) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ALPHA_8;
        return BitmapFactory.decodeStream(is, null, options);
    }

    private boolean tryToSaveBitmap(Bitmap bitmap, String savePath) {
        FileOutputStream output = null;

        try {
            output = new FileOutputStream(savePath);
            return bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
        } catch (IOException ioe) {
            // do nothing
        } finally {
            try {
                if (output != null) {
                    output.flush();
                    output.close();
                }
            } catch (Exception e) {
                // ignore...
            }
        }

        return false;
    }

    public String getQuranBaseDirectory(Context context) {
        String basePath = QuranSettings.getInstance(context).getAppCustomLocation();

        if (!isSDCardMounted()) {
            if (basePath == null || basePath.equals(Environment.getExternalStorageDirectory().getAbsolutePath())
                    || (basePath.contains(BuildConfig.APPLICATION_ID) &&
                    context.getExternalFilesDir(null) == null)) {
                basePath = null;
            }
        }

        if (basePath != null) {
            if (!basePath.endsWith(File.separator)) {
                basePath += File.separator;
            }

            return basePath + QURAN_BASE;
        }

        return null;
    }

    public int getAppUsedSpace(Context context) {
        String baseDirectory = getQuranBaseDirectory(context);

        if (baseDirectory == null) {
            return -1;
        }

        File base = new File(baseDirectory);
        ArrayList<File> files = new ArrayList<File>();
        files.add(base);
        long size = 0;

        while (!files.isEmpty()) {
            File f = files.remove(0);

            if (f.isDirectory()) {
                File[] subFiles = f.listFiles();

                if (subFiles != null) {
                    Collections.addAll(files, subFiles);
                }
            } else {
                size += f.length();
            }
        }

        return (int) (size / (1024 * 1024));
    }

    public String getQuranDatabaseDirectory(Context context) {
        String base = getQuranBaseDirectory(context);
        return (base == null) ? null : base + databaseDirectory;
    }

    public String getQuranAyahDatabaseDirectory(Context context) {
        String base = getQuranBaseDirectory(context);
        return (base == null) ? null : base + ayahInfoDirectory;
    }

    public String audioFileDirectory() {
        return getQuranAudioDirectory(appContext);
    }

    public String getQuranAudioDirectory(Context context) {
        String path = getQuranBaseDirectory(context);

        if (path == null) {
            return null;
        }

        path += audioDirectory;

        File dir = new File(path);

        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }

        writeNoMediaFile(path);
        return path + File.separator;
    }

    public String getQuranImagesBaseDirectory(Context context) {
        String s = getQuranBaseDirectory(context);
        return (s == null) ? null : s + imagesDirectory;
    }

    private String getQuranImagesDirectory(Context context) {
        return getQuranImagesDirectory(context, quranScreenInfo.getWidthParam());
    }

    public String getQuranImagesDirectory(Context context, String widthParam) {
        String base = getQuranBaseDirectory(context);

        if (base == null) {
            return null;
        }

        return base + (imagesDirectory.isEmpty() ? "" : imagesDirectory + File.separator) + "width" + widthParam;
    }

    private String recitationsDirectory() {
        String recitationDirectory = getQuranBaseDirectory(appContext) + "recitation/";
        makeDirectory(recitationDirectory);
        return recitationDirectory;
    }

    @Override
    public String recitationSessionsDirectory() {
        String sessionsDirectory = recitationsDirectory() + "sessions/";
        makeDirectory(sessionsDirectory);
        return sessionsDirectory;
    }

    @Override
    public String recitationRecordingsDirectory() {
        String recordingsDirectory = recitationsDirectory() + "recordings/";
        makeDirectory(recordingsDirectory);
        return recordingsDirectory;
    }

    public String getZipFileUrl(String widthParam) {
        String url = imageZipBaseUrl + "images" + widthParam + ".zip";
        return url;
    }

    public String getPatchFileUrl(String widthParam, int toVersion) {
        return patchBaseUrl + toVersion + "/patch" + widthParam + "_v" + toVersion + ".zip";
    }

    private String getAyaPositionFileName(String widthParam) {
        return "ayahinfo" + widthParam + ".db";
    }

    public String getAyaPositionFileUrl(String widthParam) {
        return ayahInfoBaseUrl + "ayahinfo" + widthParam + ".zip";
    }

    public boolean haveAyaPositionFile(Context context) {
        String base = getQuranAyahDatabaseDirectory(context);

        if (base == null && !makeQuranAyahDatabaseDirectory(context)) {
            return false;
        }

        String filename = getAyaPositionFileName(widthParam);
        String ayaPositionDb = base + File.separator + filename;
        File f = new File(ayaPositionDb);
        return f.exists();
    }

    public boolean hasTranslation(Context context, String fileName) {
        String path = getQuranDatabaseDirectory(context);

        if (path != null) {
            path += File.separator + fileName;
            return new File(path).exists();
        }

        return false;
    }

    @WorkerThread
    @Override
    public boolean hasArabicSearchDatabase() {
        Context context = appContext;

        if (hasTranslation(context, QuranDataProvider.QURAN_ARABIC_DATABASE)) {
            return true;
        } else if (!databaseDirectory.equals(ayahInfoDirectory)) {
            // non-hafs flavors copy their ayahinfo and arabic search database in a subdirectory,
            // so we copy back the arabic database into the translations directory where it can
            // be shared across all flavors of quran android
            File ayahInfoFile = new File(getQuranAyahDatabaseDirectory(context),
                    QuranDataProvider.QURAN_ARABIC_DATABASE);
            String baseDir = getQuranDatabaseDirectory(context);

            if (ayahInfoFile.exists() && baseDir != null) {
                File base = new File(baseDir);
                File translationsFile = new File(base, QuranDataProvider.QURAN_ARABIC_DATABASE);

                if (base.exists() || base.mkdir()) {
                    try {
                        copyFile(ayahInfoFile, translationsFile);
                        return true;
                    } catch (IOException e) {
                        Timber.e(e, "IOException copying file %s to %s", ayahInfoFile, translationsFile);
                    }
                }
            }
        }

        return false;
    }

    private void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        byte[] buf = new byte[1024];
        int len;

        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }

        in.close();
        out.close();
    }

    public String getAyaPositionFileName() {
        return getAyaPositionFileName(widthParam);
    }

    @Override
    public String getAyahDatabaseLastUpdated() {
        return ayahInfoDbLastUpdated;
    }

    @Override
    public void setAyahDatabaseLastUpdated(String timestamp) {
        ayahInfoDbLastUpdated = timestamp;
    }

    @Override
    public boolean ayahInfoDbHasGlyphData() {
        return ayahInfoDbHasGlyphData;
    }

    public String getGaplessDatabaseRootUrl() {
        return gaplessDatabaseRootUrl;
    }

    @Override
    public String getAudioFilePath(String verseKey) {
        String base = getQuranAudioDirectory(appContext);
        if (base == null) {
            return null;
        }

        return base + verseKey + ".mp3";
    }

    @Override
    public boolean isGaplessDatabaseAvailable() {
        return gaplessDatabaseRootUrl != null && !gaplessDatabaseRootUrl.isEmpty();
    }

    private static class ExceptionCatchingSource extends ForwardingSource {
        private IOException caughtException;

        ExceptionCatchingSource(Source delegate) {
            super(delegate);
        }

        @Override
        public long read(Buffer sink, long byteCount) throws IOException {
            try {
                return super.read(sink, byteCount);
            } catch (IOException e) {
                caughtException = e;
                throw e;
            }
        }

        void throwIfCaught() throws IOException {
            if (caughtException != null) {
                throw caughtException;
            }
        }
    }

    public String getAyahDatabasePath(Context context) {
        return getQuranAyahDatabaseDirectory(context) + File.separator + getAyaPositionFileName();
    }

    public String getAyahDatabaseZipPath(Context context) {
        return getQuranAyahDatabaseDirectory(context) + File.separator + getAyaPositionFileName() + ".zip";
    }

    public String getAyahDatabaseZipUrl(Context context) {
        return getAyaPositionFileUrl(widthParam);
    }

    public String quranImagesRootDirectory() {
        return getQuranBaseDirectory(appContext) + imagesDirectory;
    }

    public String ayahInfoRootDirectory() {
        return getQuranBaseDirectory(appContext) + ayahInfoDirectory;
    }

    @Override
    public String getCopyZipFile(Context context, String fileName) {
        return ayahInfoRootDirectory() + File.separator + fileName;
    }

    @Override
    public String getCopyDatabasesPath(Context context, String fileName) {
        return quranImagesRootDirectory() + File.separator + fileName;
    }

    public static String ayaPositionFileName() {
        return "ayahinfo.db";
    }
}
