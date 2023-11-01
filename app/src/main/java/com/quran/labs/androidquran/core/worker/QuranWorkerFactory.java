package com.quran.labs.androidquran.core.worker;

import android.content.Context;
import androidx.work.ListenableWorker;
import androidx.work.WorkerFactory;
import androidx.work.WorkerParameters;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Provider;

public class QuranWorkerFactory extends WorkerFactory {

    private final Map<Class<? extends ListenableWorker>, Provider<WorkerTaskFactory>> workerTaskFactories;

    @Inject
    public QuranWorkerFactory(Map<Class<? extends ListenableWorker>, Provider<WorkerTaskFactory>> workerTaskFactories) {
        this.workerTaskFactories = workerTaskFactories;
    }

    @Override
    public ListenableWorker createWorker(
            Context appContext,
            String workerClassName,
            WorkerParameters workerParameters) {
        try {
            Class<?> workerClass = Class.forName(workerClassName);
            Provider<WorkerTaskFactory> factoryProvider = findFactoryProvider(workerClass);
            if (factoryProvider != null) {
                WorkerTaskFactory factory = factoryProvider.get();
                return factory.makeWorker(appContext, workerParameters);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Provider<WorkerTaskFactory> findFactoryProvider(Class<?> workerClass) {
        for (Map.Entry<Class<? extends ListenableWorker>, Provider<WorkerTaskFactory>> entry : workerTaskFactories.entrySet()) {
            if (entry.getKey().isAssignableFrom(workerClass)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
