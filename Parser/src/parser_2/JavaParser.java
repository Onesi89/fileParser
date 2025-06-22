package parser_2;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class JavaParser {
	static String[] types = { "controller", "cbc", "bc", "qc" };
	static Pattern method = Pattern.compile(".*(public|private|protected).*\\(.*\\).*\\{.*");
	static Pattern mapping = Pattern.compile("@(Request|Get|Post|Put|Delete)Mapping");

	public static void main(String[] args) {
		try {
			String inputPath = "C:/Users/kimwp/OneDrive/Desktop/code/parser/MVC/src/mvc";
			String outputPath = "C:\\Users\\kimwp\\OneDrive\\Desktop\\code\\test";
			parse(inputPath, outputPath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static void parse(String in, String out) throws IOException {
		Files.createDirectories(Paths.get(out));
		Map<String, StringBuilder> results = new HashMap<>();
		for (String t : types)
			results.put(t, new StringBuilder());

		Files.walk(Paths.get(in)).filter(p -> p.toString().endsWith(".java")).filter(JavaParser::isTarget)
				.forEach(p -> {
					try {
						parseFile(p, results);
					} catch (IOException e) {
						System.err.println("Error: " + p);
					}
				});

		for (String t : types) {
			if (results.get(t).length() > 0) {
				Files.write(Paths.get(out, t + ".csv"), results.get(t).toString().getBytes());
			}
		}
		System.out.println("Done!");
	}

	static boolean isTarget(Path p) {
		String path = p.toString().toLowerCase();
		for (String t : types) {
			if (path.contains(t)) {
				return t.equals("controller") || path.contains("impl");
			}
		}
		return false;
	}

	static String getType(Path p) {
		String path = p.toString().toLowerCase();
		for (String t : types) {
			if (path.contains(t))
				return t;
		}
		return "unknown";
	}

	static void parseFile(Path file, Map<String, StringBuilder> results) throws IOException {
		String type = getType(file);
		String className = "", classComment = "", classUrl = "";
		String pendingUrl = "", pendingMethod = "";
		StringBuilder comment = new StringBuilder();
		boolean inComment = false;

		for (String line : Files.readAllLines(file)) {
			line = line.trim();
			if (line.isEmpty())
				continue;

			// 주석 처리
			if (line.startsWith("/**")) {
				inComment = true;
				comment.setLength(0);
				comment.append(line);
				continue;
			}
			if (inComment && line.contains("*/")) {
				inComment = false;
				continue;
			}
			if (inComment) {
				comment.append(" ").append(line);
				continue;
			}

			// 클래스 찾기
			if (line.contains("class ") && className.isEmpty()) {
				className = getClassName(line);
				classComment = clean(comment.toString());
				comment.setLength(0);
				continue;
			}

			// 매핑 (Controller용)
			if (type.equals("controller") && mapping.matcher(line).find()) {
				String url = getUrl(line);
				if (className.isEmpty()) {
					classUrl = url;
				} else {
					pendingUrl = url;
					pendingMethod = getMethod(line);
				}
				continue;
			}

			// 메서드 찾기
			if (method.matcher(line).matches()) {
				String methodName = getMethodName(line);
				String methodComment = clean(comment.toString());

				if (type.equals("controller")) {
					results.get("controller").append(String.join("|", classUrl + pendingUrl, className, classComment,
							classUrl, pendingUrl, methodName, methodComment)).append("\n");
					pendingUrl = "";
					pendingMethod = "";
				} else {
					results.get(type).append(String.join("|", className + "." + methodName, className, classComment,
							methodName, methodComment)).append("\n");
				}
				comment.setLength(0);
			}
		}
	}

	static String getClassName(String line) {
		String[] parts = line.split("\\s+");
		for (int i = 0; i < parts.length - 1; i++) {
			if ("class".equals(parts[i])) {
				return parts[i + 1].replaceAll("[{<].*", "");
			}
		}
		return "Unknown";
	}

	static String getMethodName(String line) {
		int idx = line.indexOf('(');
		if (idx > 0) {
			String[] parts = line.substring(0, idx).trim().split("\\s+");
			return parts[parts.length - 1];
		}
		return "Unknown";
	}

	static String getUrl(String line) {
		int start = line.indexOf('"');
		if (start != -1) {
			int end = line.indexOf('"', start + 1);
			if (end > start) {
				return line.substring(start + 1, end);
			}
		}
		return "";
	}

	static String getMethod(String line) {
		if (line.contains("@GetMapping"))
			return "GET";
		if (line.contains("@PostMapping"))
			return "POST";
		if (line.contains("@PutMapping"))
			return "PUT";
		if (line.contains("@DeleteMapping"))
			return "DELETE";
		return "GET";
	}

	static String clean(String s) {
		if (s.isEmpty())
			return "주석없음";
		String c = s.replaceAll("/\\*\\*|\\*/|\\*", "").replaceAll("\\s+", " ").trim();
		return c.length() > 50 ? c.substring(0, 50) + "..." : c;
	}
}