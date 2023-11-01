package com.quran.labs.androidquran.util;

import io.reactivex.rxjava3.core.Maybe;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;

import javax.inject.Inject;

public class ImageUtil {

    private final OkHttpClient okHttpClient;

    @Inject
    public ImageUtil(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
    }

    public Maybe<File> downloadImage(String url, File outputPath) {
        return Maybe.fromCallable(() -> {
            if (!outputPath.exists()) {
                File destination = new File(outputPath.getPath() + ".tmp");
                Request request = new Request.Builder()
                        .url(url)
                        .build();
                okhttp3.Call call = okHttpClient.newCall(request);

                try {
                    Response response = call.execute();
                    if (response.isSuccessful()) {
                        // Save the png from the download to a temporary file
                        try (BufferedSource source = response.body().source();
                             BufferedSink destinationSink = Okio.buffer(Okio.sink(destination))) {
                            destinationSink.writeAll(source);
                        }

                        // Write it to the normal file
                        try (BufferedSource destinationSource = Okio.buffer(Okio.source(destination));
                             BufferedSink outputPathSink = Okio.buffer(Okio.sink(outputPath))) {
                            outputPathSink.writeAll(destinationSource);
                        }

                        // Delete the old temporary file
                        destination.delete();
                    }
                } catch (IOException ioException) {
                    // If we're interrupted, pretend nothing happened. This happened
                    // due to a dispose / cancellation. Maybe's fromCallable will not
                    // actually emit in this case.
                    //
                    // A more proper fix would probably be to use Maybe.create and
                    // set a cancellation handler to stop the network call.
                }
            }
            return outputPath;
        });
    }
}
