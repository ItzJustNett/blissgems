package org.hyn.dropitemcontrol.utils;

public class WordUtils {
   public static String capitalize(String str) {
      if (str != null && !str.isEmpty()) {
         StringBuilder result = new StringBuilder();
         String[] words = str.split("\\s");
         String[] var3 = words;
         int var4 = words.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            String word = var3[var5];
            if (!word.isEmpty()) {
               result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase()).append(" ");
            }
         }

         return result.toString().trim();
      } else {
         return str;
      }
   }
}
