package com.quran.labs.androidquran.ui.helpers;

import java.util.Set;

public abstract class AyahHighlight {
    public abstract String getKey();

    public boolean isTransition() {
        return this instanceof TransitionAyahHighlight;
    }

    public static class SingleAyahHighlight extends AyahHighlight {
        private final String key;

        public SingleAyahHighlight(String key) {
            this.key = key;
        }

        public SingleAyahHighlight(int sura, int ayah) {
            this(sura, ayah, -1);
        }

        public SingleAyahHighlight(int sura, int ayah, int word) {
            this.key = sura + ":" + ayah + (word < 0 ? "" : ":" + word);
        }

        public static Set<AyahHighlight> createSet(Set<String> ayahKeys) {
            return ayahKeys.stream()
                    .map(SingleAyahHighlight::new)
                    .collect(java.util.stream.Collectors.toSet());
        }

        @Override
        public String getKey() {
            return key;
        }
    }

    public static class TransitionAyahHighlight extends AyahHighlight {
        private final AyahHighlight source;
        private final AyahHighlight destination;

        public TransitionAyahHighlight(AyahHighlight source, AyahHighlight destination) {
            this.source = source;
            this.destination = destination;
        }

        @Override
        public String getKey() {
            return source.getKey() + "->" + destination.getKey();
        }
    }
}
