package com.quran.labs.androidquran.view;

import com.quran.data.model.audio.Qari;
import com.quran.labs.androidquran.common.audio.model.QariItem;
import com.quran.labs.androidquran.common.audio.repository.CurrentQariManager;

import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.SupervisorJob;
import kotlinx.coroutines.cancel;
import kotlinx.coroutines.launch;
import javax.inject.Inject;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.flowOn;
import kotlinx.coroutines.flow.observeOn;
import kotlinx.coroutines.withContext;

public class CurrentQariBridge {

    private final CurrentQariManager currentQariManager;
    private final CoroutineScope scope;

    @Inject
    public CurrentQariBridge(CurrentQariManager currentQariManager) {
        this.currentQariManager = currentQariManager;
        this.scope = new CoroutineScope(new SupervisorJob());
    }

    public void listenToQaris(Function1<Qari, Unit> lambda) {
        scope.launch(() -> {
            withContext(Dispatchers.Main) {
                currentQariManager
                        .flow()
                        .collect(lambda);
            }
        });
    }

    public void unsubscribeAll() {
        scope.cancel();
    }
}
