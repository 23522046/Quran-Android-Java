import com.quran.data.core.QuranInfo;
import com.quran.data.model.QuranText;
import com.quran.data.model.SuraAyah;
import com.quran.data.model.SuraAyahIterator;
import com.quran.data.model.VerseRange;
import com.quran.labs.androidquran.common.LocalTranslation;
import com.quran.labs.androidquran.common.LocalTranslationDisplaySort;
import com.quran.labs.androidquran.common.QuranAyahInfo;
import com.quran.labs.androidquran.common.TranslationMetadata;
import com.quran.labs.androidquran.database.TranslationsDBAdapter;
import com.quran.labs.androidquran.model.translation.TranslationModel;
import com.quran.labs.androidquran.presenter.Presenter;
import com.quran.labs.androidquran.util.QuranSettings;
import com.quran.labs.androidquran.util.TranslationUtil;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.util.*;

public class BaseTranslationPresenter<T> implements Presenter<T> {
    private final TranslationModel translationModel;
    private final TranslationsDBAdapter translationsAdapter;
    private final TranslationUtil translationUtil;
    private final QuranInfo quranInfo;
    private long lastCacheTime = 0;
    private final Map<String, LocalTranslation> translationMap = new HashMap<>();
    protected T translationScreen;
    private Disposable disposable;

    public BaseTranslationPresenter(TranslationModel translationModel,
                                    TranslationsDBAdapter translationsAdapter,
                                    TranslationUtil translationUtil,
                                    QuranInfo quranInfo) {
        this.translationModel = translationModel;
        this.translationsAdapter = translationsAdapter;
        this.translationUtil = translationUtil;
        this.quranInfo = quranInfo;
    }

    public Single<ResultHolder> getVerses(boolean getArabic,
                                          List<String> translationsFileNames,
                                          VerseRange verseRange) {

        List<LocalTranslation> translations = translationsAdapter.getTranslations();
        List<LocalTranslation> sortedTranslations = new ArrayList<>(translations);
        Collections.sort(sortedTranslations, new LocalTranslationDisplaySort());

        List<String> orderedTranslationsFileNames = new ArrayList<>();
        for (LocalTranslation translation : sortedTranslations) {
            if (translationsFileNames.contains(translation.getFilename())) {
                orderedTranslationsFileNames.add(translation.getFilename());
            }
        }

        Observable<String> source = Observable.fromIterable(orderedTranslationsFileNames);

        Observable<List<List<QuranText>>> translationsObservable = source.concatMapEager(db ->
                translationModel.getTranslationFromDatabase(verseRange, db)
                        .map(texts -> ensureProperTranslations(verseRange, texts))
                        .onErrorReturnItem(new ArrayList<>())
                        .toObservable()
        ).toList();

        Single<List<QuranText>> arabicObservable;
        if (!getArabic) {
            arabicObservable = Single.just(new ArrayList<>());
        } else {
            arabicObservable = translationModel.getArabicFromDatabase(verseRange)
                    .onErrorReturnItem(new ArrayList<>());
        }

        return Single.zip(arabicObservable, translationsObservable, getTranslationMapSingle(),
                (arabic, texts, map) -> {
                    LocalTranslation[] translationInfos = getTranslations(orderedTranslationsFileNames, map);
                    List<QuranAyahInfo> ayahInfo = combineAyahData(verseRange, arabic, texts, translationInfos);
                    return new ResultHolder(translationInfos, ayahInfo);
                })
                .subscribeOn(Schedulers.io());
    }

    public List<String> getTranslations(QuranSettings quranSettings) {
        return new ArrayList<>(quranSettings.getActiveTranslations());
    }

    public LocalTranslation[] getTranslations(List<String> translations,
                                              Map<String, LocalTranslation> translationMap) {
        int translationCount = translations.size();

        if (translationCount == 0) {
            // Fallback to all translations when the map is empty
            return translationMap.values().toArray(new LocalTranslation[0]);
        } else {
            List<LocalTranslation> translationList = new ArrayList<>();
            for (String translation : translations) {
                LocalTranslation localTranslation = translationMap.get(translation);
                if (localTranslation == null) {
                    localTranslation = new LocalTranslation(translation, "", "", "");
                }
                translationList.add(localTranslation);
            }
            return translationList.toArray(new LocalTranslation[0]);
        }
    }

    public List<QuranAyahInfo> combineAyahData(VerseRange verseRange,
                                               List<QuranText> arabic,
                                               List<List<QuranText>> texts,
                                               LocalTranslation[] translationInfo) {
        int arabicSize = arabic.size();
        int translationCount = texts.size();
        List<QuranAyahInfo> result = new ArrayList<>();

        if (translationCount > 0) {
            int verses = (arabicSize == 0) ? verseRange.getVersesInRange() : arabicSize;
            for (int i = 0; i < verses; i++) {
                List<QuranText> quranTexts = new ArrayList<>();
                for (List<QuranText> text : texts) {
                    if (text.size() > i) {
                        quranTexts.add(text.get(i));
                    } else {
                        quranTexts.add(null);
                    }
                }

                QuranText arabicQuranText = (arabicSize == 0) ? null : arabic.get(i);
                QuranText element = null;
                for (QuranText quranText : quranTexts) {
                    if (quranText != null) {
                        element = quranText;
                        break;
                    }
                }

                if (element != null) {
                    List<TranslationMetadata> ayahTranslations = new ArrayList<>();
                    for (int index = 0; index < quranTexts.size(); index++) {
                        QuranText quranText = quranTexts.get(index);
                        LocalTranslation translation = (index < translationInfo.length) ? translationInfo[index] : null;
                        int translationMinVersion = (translation != null) ? translation.getMinimumVersion() : 0;
                        int translationId = (translation != null) ? translation.getId() : -1;
                        boolean shouldProcess = (translationMinVersion >= TranslationUtil.MINIMUM_PROCESSING_VERSION);
                        QuranText text = (quranText != null) ? quranText : new QuranText(element.getSura(), element.getAyah(), "");
                        if (shouldProcess) {
                            ayahTranslations.add(translationUtil.parseTranslationText(text, translationId));
                        } else {
                            ayahTranslations.add(new TranslationMetadata(element.getSura(), element.getAyah(), text.getText(), translationId));
                        }
                    }

                    result.add(new QuranAyahInfo(element.getSura(), element.getAyah(),
                            (arabicQuranText != null) ? arabicQuranText.getText() : null, ayahTranslations,
                            quranInfo.getAyahId(element.getSura(), element.getAyah())));
                }
            }
        } else if (arabicSize > 0) {
            for (QuranText arabicText : arabic) {
                result.add(new QuranAyahInfo(arabicText.getSura(), arabicText.getAyah(),
                        arabicText.getText(), Collections.emptyList(),
                        quranInfo.getAyahId(arabicText.getSura(), arabicText.getAyah())));
            }
        }
        return result;
    }

    public List<QuranText> ensureProperTranslations(VerseRange verseRange, List<QuranText> inputTexts) {
        int expectedVerses = verseRange.getVersesInRange();
        int textSize = inputTexts.size();

        List<QuranText> texts = new ArrayList<>(inputTexts);
        if (textSize == 0 || textSize == expectedVerses) {
            return texts;
        }

        SuraAyah start = new SuraAyah(verseRange.getStartSura(), verseRange.getStartAyah());
        SuraAyah end = new SuraAyah(verseRange.getEndingSura(), verseRange.getEndingAyah());
        SuraAyahIterator iterator = new SuraAyahIterator(quranInfo, start, end);

        int i = 0;
        while (iterator.next()) {
            QuranText item = (i < texts.size()) ? texts.get(i) : null;
            if (item == null || item.getSura() != iterator.getSura() || item.getAyah() != iterator.getAyah()) {
                texts.add(i, new QuranText(iterator.getSura(), iterator.getAyah(), ""));
            }
            i++;
        }
        return texts;
    }

    private Single<Map<String, LocalTranslation>> getTranslationMapSingle() {
        if (this.translationMap.isEmpty() || this.lastCacheTime != translationsAdapter.getLastWriteTime()) {
            return Single.fromCallable(() -> translationsAdapter.getTranslations())
                    .map(translations -> {
                        Map<String, LocalTranslation> map = new HashMap<>();
                        for (LocalTranslation translation : translations) {
                            map.put(translation.getFilename(), translation);
                        }
                        return map;
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSuccess(map -> {
                        this.lastCacheTime = translationsAdapter.getLastWriteTime();
                        this.translationMap.clear();
                        this.translationMap.putAll(map);
                    });
        } else {
            return Single.just(this.translationMap);
        }
    }

    public class ResultHolder {
        public final LocalTranslation[] translations;
        public final List<QuranAyahInfo> ayahInformation;

        public ResultHolder(LocalTranslation[] translations, List<QuranAyahInfo> ayahInformation) {
            this.translations = translations;
            this.ayahInformation = ayahInformation;
        }
    }

    @Override
    public void bind(T what) {
        translationScreen = what;
    }

    @Override
    public void unbind(T what) {
        translationScreen = null;
        if (disposable != null) {
            disposable.dispose();
        }
    }
}
