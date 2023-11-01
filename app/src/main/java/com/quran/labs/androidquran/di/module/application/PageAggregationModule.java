package com.quran.labs.androidquran.di.module.application;

import com.quran.data.constant.DependencyInjectionConstants;
import com.quran.data.source.PageProvider;
import com.quran.data.source.QuranDataSource;
import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import java.util.Map;

@Module
public class PageAggregationModule {

    @Provides
    public PageProvider provideQuranPageProvider(
        Map<String, PageProvider> providers,
        @Named(DependencyInjectionConstants.CURRENT_PAGE_TYPE) String pageType
    ) {
        // explicitly error if this doesn't exist, since it should never happen
        return providers.get(pageType);
    }

    @Provides
    public QuranDataSource provideQuranDataSource(PageProvider pageProvider) {
        return pageProvider.getDataSource();
    }
}
