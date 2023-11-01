package com.quran.labs.androidquran.core.worker.di;

import com.quran.labs.androidquran.core.worker.WorkerKey;
import com.quran.labs.androidquran.core.worker.WorkerTaskFactory;
import com.quran.labs.androidquran.worker.AudioUpdateWorker;
import com.quran.labs.androidquran.worker.MissingPageDownloadWorker;
import com.quran.labs.androidquran.worker.PartialPageCheckingWorker;
import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

@Module(includes = {AudioUpdateModule.class})
public abstract class WorkerModule {

    @Binds
    @IntoMap
    @WorkerKey(PartialPageCheckingWorker.class)
    public abstract WorkerTaskFactory bindPartialPageCheckingWorkerFactory(
            PartialPageCheckingWorker.Factory workerFactory);

    @Binds
    @IntoMap
    @WorkerKey(MissingPageDownloadWorker.class)
    public abstract WorkerTaskFactory bindMissingPageDownloadWorkerFactory(
            MissingPageDownloadWorker.Factory workerFactory);

    @Binds
    @IntoMap
    @WorkerKey(AudioUpdateWorker.class)
    public abstract WorkerTaskFactory bindAudioUpdateWorkerFactory(
            AudioUpdateWorker.Factory workerFactory);
}
