package parser_2;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class XmlParser {
	static Pattern start = Pattern.compile("<(select|insert|update|delete)\\s+id=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    static Pattern end = Pattern.compile("</(select|insert|update|delete)>", Pattern.CASE_INSENSITIVE);
    static Pattern table = Pattern.compile("\\b(?:FROM|INTO|UPDATE|JOIN)\\s+([a-zA-Z0-9_\\.]+)", Pattern.CASE_INSENSITIVE);
    
    public static void main(String[] args) {
        try {
    		String inputPath = "C:/Users/kimwp/OneDrive/Desktop/code/parser/MVC/mapper";
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
            .filter(p -> p.toString().endsWith(".xml"))
            .forEach(p -> {
                try {
                    parseFile(p, result);
                } catch (IOException e) {
                    System.err.println("Error: " + p);
                }
            });
            
        Files.write(Paths.get(out, "queries.csv"), result.toString().getBytes());
        System.out.println("Done!");
    }
    
    static void parseFile(Path file, StringBuilder result) throws IOException {
        boolean inQuery = false, inComment = false;
        String queryId = "", queryType = "";
        StringBuilder sql = new StringBuilder();
        StringBuilder comment = new StringBuilder();
        
        for (String line : Files.readAllLines(file)) {
            line = line.trim();
            
            // 주석 처리 (<!-- --> 또는 /** */)
            if (line.contains("<!--") || line.contains("/**")) {
                inComment = true;
                comment.setLength(0);
                comment.append(line);
                continue;
            }
            if (inComment && (line.contains("-->") || line.contains("*/"))) {
                comment.append(" ").append(line);
                inComment = false;
                continue;
            }
            if (inComment) {
                comment.append(" ").append(line);
                continue;
            }
            
            Matcher m = start.matcher(line);
            if (m.find()) {
                inQuery = true;
                queryType = m.group(1);
                queryId = m.group(2);
                sql.setLength(0);
                continue;
            }
            
            if (inQuery && end.matcher(line).find()) {
                String tables = getTables(sql.toString());
                String cleanComment = comment.toString()
                    .replaceAll("<!--|-->|/\\*\\*|\\*/|\\*", "")
                    .trim();
                    
                result.append(file).append("|")
                      .append(queryId).append("|")
                      .append(queryType).append("|")
                      .append(cleanComment).append("|")
                      .append(tables).append("\n");
                      
                inQuery = false;
                comment.setLength(0);
                continue;
            }
            
            if (inQuery) {
                sql.append(line).append(" ");
            }
        }
    }
    
    static String getTables(String sql) {
        Set<String> tables = new HashSet<>();
        Matcher m = table.matcher(sql);
        while (m.find()) {
            tables.add(m.group(1));
        }
        return String.join(";", tables);
    }
}