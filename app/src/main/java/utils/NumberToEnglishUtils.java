package utils;

public class NumberToEnglishUtils {
    public static String getEnglish(int number) {
        String en="";
        switch (number) {
            case 0:
                en = "zero";
                break;
            case 1:
                en = "one";
                break;
            case 2:
                en = "two";
                break;
            case 3:
                en = "three";
                break;
            case 4:
                en = "four";
                break;
            case 5:
                en = "five";
                break;
            case 6:
                en = "six";
                break;
            case 7:
                en = "seven";
                break;
            case 8:
            en = "eight";
                break;
            case 9:
                en = "nine";
                break;
        }
        return en;
    }
}
