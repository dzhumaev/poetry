package com.almir.poetrymemorization;

import java.util.List;

public class LevenshteinDistance {
    private static int minimum(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

    public static int computeLevenshteinDistance(String str1,String str2) {
        int[][] distance = new int[str1.length() + 1][str2.length() + 1];

        for (int i = 0; i <= str1.length(); i++)
            distance[i][0] = i;
        for (int j = 1; j <= str2.length(); j++)
            distance[0][j] = j;

        for (int i = 1; i <= str1.length(); i++)
            for (int j = 1; j <= str2.length(); j++)
                distance[i][j] = minimum(
                        distance[i - 1][j] + 1,
                        distance[i][j - 1] + 1,
                        distance[i - 1][j - 1] + ((str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0 : 1));

        return distance[str1.length()][str2.length()];
    }

    // TODO: check that all the punctuation is stripped out and everything is lowercased
    public static boolean[] showUnrecognizedWords(String[] reference, String[] guess) {
        int[][] T = new int[reference.length + 1][guess.length + 1];

        for (int i = 0; i <= reference.length; i++)
            T[i][0] = i;

        for (int i = 0; i <= guess.length; i++)
            T[0][i] = i;

        for (int i = 1; i <= reference.length; i++) {
            for (int j = 1; j <= guess.length; j++) {
                if (reference[i - 1].equals(guess[j - 1]))
                    T[i][j] = T[i - 1][j - 1];
                else
                    T[i][j] = Math.min(T[i - 1][j], T[i][j - 1]) + 1;
            }
        }

        boolean[] isUnrecognizedWord = new boolean[reference.length];

        for (int i = reference.length, j = guess.length; i > 0 || j > 0; ) {
            if (i > 0 && T[i][j] == T[i - 1][j] + 1) {
                isUnrecognizedWord[--i] = true;
            } else if (j > 0 && T[i][j] == T[i][j - 1] + 1) {
                --j;
            } else if (i > 0 && j > 0 && T[i][j] == T[i - 1][j - 1]) {
                isUnrecognizedWord[--i] = false;
                --j;
            }
        }

        return isUnrecognizedWord;
    }

}