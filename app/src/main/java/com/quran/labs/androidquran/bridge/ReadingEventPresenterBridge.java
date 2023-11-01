package com.quran.labs.androidquran.bridge;

import com.quran.data.model.SuraAyah;
import com.quran.data.model.selection.AyahSelection;
import com.quran.data.model.selection.SelectionIndicator;
import com.quran.data.model.selection.SelectionUtilKt;
import com.quran.reading.common.ReadingEventPresenter;
import kotlinx.coroutines.MainScope;
import kotlinx.coroutines.flow.FlowCollector;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.collect;
import kotlinx.coroutines.flow.flow;
import kotlinx.coroutines.flow.onEach;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.FlowKt__TransformKt;

public class ReadingEventPresenterBridge {

    private final MainScope scope = new MainScope();
    private final Flow<Void> clicksFlow;
    private final Flow<AyahSelection> ayahSelectionFlow;
    private final ReadingEventPresenter readingEventPresenter;

    public ReadingEventPresenterBridge(ReadingEventPresenter readingEventPresenter, FlowCollector<? super Void> flowCollector, FlowCollector<? super AyahSelection> ayahSelectionFlowCollector) {
        this.readingEventPresenter = readingEventPresenter;
        this.clicksFlow = readingEventPresenter.getClicksFlow();
        FlowKt__TransformKt.onEach(clicksFlow, (Void aVoid) -> {
            flowCollector.emit(aVoid);
        }, null, 2, null);
        ayahSelectionFlow = readingEventPresenter.getAyahSelectionFlow();
        FlowKt__TransformKt.onEach(ayahSelectionFlow, (AyahSelection ayahSelection) -> {
            ayahSelectionFlowCollector.emit(ayahSelection);
        }, null, 2, null);
        CoroutineScope coroutineScope = scope;
        clicksFlow.collect((FlowCollector)flowCollector);
        ayahSelectionFlow.collect((FlowCollector)ayahSelectionFlowCollector);
    }

    public void setSelection(int sura, int ayah, boolean scrollToAyah) {
        SelectionIndicator selectionIndicator = scrollToAyah ? SelectionIndicator.ScrollOnly : SelectionIndicator.None;
        AyahSelection ayahSelection = SelectionUtilKt.toAyahSelection(new SuraAyah(sura, ayah), selectionIndicator);
        readingEventPresenter.onAyahSelection(ayahSelection);
    }

    public void clearSelectedAyah() {
        readingEventPresenter.onAyahSelection(AyahSelection.None);
    }

    public void withSelectionIndicator(SelectionIndicator selectionIndicator) {
        AyahSelection currentSelection = currentSelection();
        if (currentSelection != AyahSelection.None) {
            AyahSelection updatedSelection = SelectionUtilKt.withSelectionIndicator(currentSelection, selectionIndicator);
            readingEventPresenter.onAyahSelection(updatedSelection);
        }
    }

    public void clearMenuForSelection() {
        AyahSelection currentSelection = currentSelection();
        if (currentSelection != AyahSelection.None) {
            AyahSelection updatedSelection = SelectionUtilKt.withSelectionIndicator(currentSelection, SelectionIndicator.None);
            readingEventPresenter.onAyahSelection(updatedSelection);
        }
    }

    public void dispose() {
        scope.cancel();
    }

    private AyahSelection currentSelection() {
        return ayahSelectionFlow.toLiveData().getValue();
    }
}
