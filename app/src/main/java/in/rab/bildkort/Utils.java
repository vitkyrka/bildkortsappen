package in.rab.bildkort;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class Utils {
    static String inputStreamToString(InputStream input) throws IOException {
        Scanner scanner = new Scanner(input).useDelimiter("\\A");
        String str = scanner.hasNext() ? scanner.next() : "";

        input.close();

        return str;
    }
}
