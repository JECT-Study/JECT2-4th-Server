package com.ject.vs.util;

import java.util.List;
import java.util.Locale;

public class SlangFilter {
    private static int[] buildFailure(char[] pattern) {
        int m = pattern.length;
        int[] fail = new int[m];
        fail[0] = 0;
        int j = 0;

        for(int i = 1; i < m; i++) {
            while(j > 0 && pattern[i] != pattern[j]) {
                j = fail[j-1];
            }
            if(pattern[i] == pattern[j]) j++;
            fail[i] = j;
        }
        return fail;
    }

    private static boolean kmpContains(char[] text, char[] pattern) {
        int n = text.length;
        int m = pattern.length;
        if(m == 0) return false;

        int[] fail = buildFailure(pattern);
        int j = 0;

        for(int i = 0; i < n; i++) {
            while(j > 0 && text[i] != pattern[j]) {
                j = fail[j-1];
            }
            if(text[i] == pattern[j]) j++;
            if(j == m) return true;
        }
        return false;
    }

    public static boolean containsSlang(String input, List<String> slangList) {
        if(input == null || input.isBlank()) return false;
        char[] text = input.toLowerCase(Locale.ROOT).toCharArray();

        for(String slang : slangList) {
            if(kmpContains(text, slang.toCharArray())) return true;
        }
        return false;
    }
}
