package com.rebuild.utils;

import org.junit.Test;

import java.util.Arrays;
import java.util.Locale;

public class Tests {

    @Test
    public void testLocale() {
        for (Locale l : Locale.getAvailableLocales()) System.out.println(l.toString());
//        System.out.println(Arrays.toString(Locale.getISOCountries()));
//        System.out.println(Arrays.toString(Locale.getISOLanguages()));
    }

}
