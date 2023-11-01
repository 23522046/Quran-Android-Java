package com.quran.labs.androidquran.core.worker;

import android.content.Context;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

public interface WorkerTaskFactory {
    ListenableWorker makeWorker(Context appContext, WorkerParameters workerParameters);
}
