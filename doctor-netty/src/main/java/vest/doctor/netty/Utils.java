package vest.doctor.netty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Utils {

    public static int CHUNK_SIZE = 4096;

    public static List<String> split(String str, char c) {
        if (str == null || str.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> ll = new ArrayList<>(2);
        int index = 0;
        while (true) {
            int end = str.indexOf(c, index);
            if (end >= index) {
                ll.add(str.substring(index, end));
                index = end + 1;
            } else {
                ll.add(str.substring(index));
                break;
            }
        }
        return ll;
    }

    public static String squeeze(String s, char c) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        if (s.length() == 1 && s.charAt(0) == c) {
            return s;
        }
        char[] a = s.toCharArray();
        int n = 1;
        for (int i = 1; i < a.length; i++) {
            a[n] = a[i];
            if (a[n] != c) {
                n++;
            } else if (a[n - 1] != c) {
                n++;
            }
        }
        return new String(a, 0, n);
    }
}