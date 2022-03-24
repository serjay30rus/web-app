package ru.kutepov.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.WrongCharaterException;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import ru.kutepov.model.Lemma;

import java.io.IOException;
import java.util.*;

public class LemmaFinder {
    private final LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
    private static String wordTypeRegex = "[\\W\\w&&[^а-яА-Я\\s]]";
    private static Logger mainExceptions = LogManager.getLogger("searchFile");

    public LemmaFinder() throws IOException {
    }


    public static HashMap<String, Integer> lemmatize(String string) throws IOException {
        try {
            ArrayList<String> words =
                    new ArrayList<>(Arrays.asList(string.replaceAll(wordTypeRegex, "")
                            .toLowerCase()
                            .split("\\s+")));
            LuceneMorphology luceneMorph = new RussianLuceneMorphology();
            HashMap<String, Integer> result = new HashMap<>();

            String normalWord = "";
            for (String word : words) {
                if (!word.equals("")) {
                    List<String> wordBaseForms = luceneMorph.getMorphInfo(word);
                    if (!wordBaseForms.get(0).contains("МЕЖД") && !wordBaseForms.get(0).contains("ПРЕДЛ") && !wordBaseForms.get(0).contains("СОЮЗ")) {
                        normalWord = luceneMorph.getNormalForms(word).get(0);
                        if (result.containsKey(normalWord)) {
                            result.put(normalWord, result.get(normalWord) + 1);
                        } else {
                            result.put(normalWord, 1);
                        }
                    }
                }
            }
            return result;
        } catch (IndexOutOfBoundsException | NullPointerException | WrongCharaterException exp) {
            mainExceptions.error(exp);
            return null;
        }
    }

    public Set<String> getLemmaSet(String text) {
        String[] textArray = textToArray(text);
        Set<String> lemmaSet = new HashSet<>();
        for (String word : textArray) {
            if (isCorrectWordType(word) && !word.isEmpty()) {
                List<String> wordBaseForms = luceneMorphology.getNormalForms(word);
                lemmaSet.addAll(wordBaseForms);
            }
        }
        return lemmaSet;
    }

    private String[] textToArray(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("([^а-я\\s])", " ")
                .replaceAll("\\s+", " ").split(" ");
    }

    private boolean isCorrectWordType(String word) {
        try {
            List<String> wordInfo = luceneMorphology.getMorphInfo(word);
            for (String morphInfo : wordInfo) {
                if (morphInfo.matches(wordTypeRegex)) {
                    return false;
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println(word);
        }
        return true;
    }

    public Map<String, Float> countLemmasOnField(String text) {
        Map<String, Float> wordCountMap = new HashMap<>();
        String[] textArray = textToArray(text);
        for (String word : textArray) {
            if (isCorrectWordType(word) && !word.isEmpty()) {
                List<String> wordBaseForms = luceneMorphology.getNormalForms(word);
                for (String lemma : wordBaseForms) {
                    wordCountMap.put(lemma, wordCountMap.getOrDefault(lemma, 0f) + 1);
                }
            }
        }
        return wordCountMap;
    }

    public HashMap<String, Float> calculateLemmasRank(Map<String, Lemma> lemmas,
                                                      Map<String, Float> titleFieldLemmas,
                                                      Map<String, Float> bodyFieldLemmas) {
        HashMap<String, Float> lemmasAndRankMap = new HashMap<>();
        for (Map.Entry<String, Lemma> lemma : lemmas.entrySet()) {
            float rank = titleFieldLemmas.getOrDefault(lemma.getKey(), 0f) * 1.0f
                    + bodyFieldLemmas.getOrDefault(lemma.getKey(), 0f) * 0.8f;
            lemmasAndRankMap.put(lemma.getKey(), rank);
        }

        return lemmasAndRankMap;
    }


}
