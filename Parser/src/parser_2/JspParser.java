package parser_2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JspParser {
    public static void main(String[] args) {
        try {
    		String inputPath = "C:/Users/kimwp/OneDrive/Desktop/code/parser/MVC/jsp";
    		String outputPath = "C:\\Users\\kimwp\\OneDrive\\Desktop\\code\\test";
            parse(inputPath, outputPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    static void parse(String in, String out) throws IOException {
        Files.createDirectories(Paths.get(out));
        StringBuilder result = new StringBuilder();
        
        Files.walk(Paths.get(in))
            .filter(p -> p.toString().endsWith(".jsp"))
            .forEach(p -> {
                try {
                    parseFile(p, result);
                } catch (IOException e) {
                    System.err.println("Error: " + p);
                }
            });
            
        Files.write(Paths.get(out, "jsp.csv"), result.toString().getBytes());
        System.out.println("Done!");
    }
    
    static void parseFile(Path file, StringBuilder result) throws IOException {
        String name = file.getFileName().toString();
        
        // JSP 파일명 -> .do URL 매핑 추가
        result.append(name).append("|")
              .append(name.replace(".jsp", ".do"))
              .append("\n");
        
        for (String line : Files.readAllLines(file)) {
            if (line.contains(".do")) {
                String url = getUrl(line);
                if (!url.isEmpty()) {
                    result.append(name).append("|").append(url).append("\n");
                }
            }
        }
    }
    
    static String getUrl(String line) {
        int start = line.indexOf("/app");
        if (start == -1) start = line.indexOf("\"");
        if (start == -1) return "";
        
        int end = line.indexOf(".do", start);
        if (end > start) {
            return line.substring(start, end + 3).replace("\"", "");
        }
        return "";
    }
}