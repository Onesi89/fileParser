package parser_1;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class SimpleJavaParser {
	// 설정
	private static final String[] TARGET_PATHS = { "controller", "cbc", "bc", "qc" };
	private static final String CONTROLLER_KEYWORD = "controller";
	private static final String IMPL_KEYWORD = "impl";
	private static final Pattern METHOD_PATTERN = Pattern.compile(".*(public|private|protected).*\\(.*\\).*\\{.*");
	private static final Pattern MAPPING_PATTERN = Pattern.compile("@(Request|Get|Post|Put|Delete)Mapping");

	public static void main(String[] args) {
		String inputPath = "C:/Users/kimwp/OneDrive/Desktop/code/parser/MVC/src/mvc";
		String outputPath = "C:\\Users\\kimwp\\OneDrive\\Desktop\\code\\test";

		try {
			parseProject(inputPath, outputPath);
		} catch (Exception e) {
			System.err.println("오류: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 프로젝트 파싱 실행
	 */
	public static void parseProject(String inputPath, String outputPath) throws IOException {
		System.out.println("=== Java 파일 파싱 시작 ===");
		System.out.println("입력: " + inputPath);
		System.out.println("출력: " + outputPath);

		Files.createDirectories(Paths.get(outputPath));

		Map<String, StringBuilder> results = new HashMap<>();
		Arrays.stream(TARGET_PATHS).forEach(path -> results.put(path, new StringBuilder()));

		AtomicInteger count = new AtomicInteger(0);

		// 파일 스캔 및 파싱
		Files.walk(Paths.get(inputPath))
			 .filter(Files::isRegularFile)
			 .filter(path -> path.toString().endsWith(".java"))
			 .filter(SimpleJavaParser::isTargetFile)
			 .forEach(path -> {
					try {
						parseFile(path, results);
						count.incrementAndGet();
					} catch (IOException e) {
						System.err.println("파일 파싱 실패: " + path);
					}
				});

		// 결과 파일 저장
		saveResults(outputPath, results);

		System.out.println("=== 파싱 완료 (" + count.get() + "개 파일) ===");
	}

	/**
	 * 타겟 파일 여부 확인
	 */
	private static boolean isTargetFile(Path path) {
		String pathStr = path.toString().toLowerCase();
		
		  return Arrays.stream(TARGET_PATHS)
				  .filter(pathStr::contains)
                  .anyMatch(targetKeyword -> {
                          if (!CONTROLLER_KEYWORD.equals(targetKeyword)) {
                              return pathStr.contains(IMPL_KEYWORD);
                          }
                          // 3. "controller" 키워드인 경우 바로 true 반환
                          return true;
                      }
                  );
	}

	/**
	 * 파일 타입 결정 (controller/service)
	 */
    private static String getFileType(Path path) {
        String pathStr = path.toString().toLowerCase();
        for (String targetPath : TARGET_PATHS) {
            if (pathStr.contains(targetPath)) {
                return targetPath;
            }
        }
        return "unknown";
    }

	/**
	 * 단일 파일 파싱
	 */
	private static void parseFile(Path filePath, Map<String, StringBuilder> results) throws IOException {
		String fileType = getFileType(filePath);
		String fileName = filePath.getFileName().toString();

		System.out.println("파싱: " + fileName);

		String className = "";
		String classComment = "";
		String classURL = "";
		StringBuilder currentComment = new StringBuilder();
		String pendingUrl = "";
		String pendingMethod = ""; //GET 등

		boolean inJavaDoc = false;

		try (BufferedReader reader = Files.newBufferedReader(filePath)) {
			String line;

			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty())
					continue;

				// JavaDoc 처리
				if (line.startsWith("/**")) {
					inJavaDoc = true;
					currentComment.setLength(0);
					currentComment.append(line);
				} else if (inJavaDoc && line.contains("*/")) {
					inJavaDoc = false;
				} else if (inJavaDoc && !line.contains("@param") && !line.contains("@return")) {
					currentComment.append(" ").append(line);
				}

				// 클래스 찾기
				else if (line.contains("class ") && className.isEmpty()) {
					className = extractClassName(line);
					classComment = cleanComment(currentComment.toString());
					currentComment.setLength(0);
				}

				// 매핑 어노테이션 (Controller용)
				else if (fileType.equals("controller") && MAPPING_PATTERN.matcher(line).find()) {
					if(className.isEmpty()) {
						classURL   = extractUrl(line);
					} else {
						pendingUrl = extractUrl(line);
						pendingMethod = extractHttpMethod(line);
					}
				}

				// 메서드 찾기
				else if (METHOD_PATTERN.matcher(line).matches()) {
					String methodName = extractMethodName(line);
					String methodComment = cleanComment(currentComment.toString());

					// 결과 저장
					if (fileType.equals("controller")) {
						results.get("controller")
						       .append(String.join("|",classURL + pendingUrl, className, classComment, classURL, pendingUrl,methodName, methodComment))
						       .append("\n");
						pendingUrl = "";
						pendingMethod = "";
					} else {
						results.get(fileType)
						       .append(String.join("|", className + "." + methodName, className,classComment, methodName, methodComment))
						       .append("\n");
					}

					currentComment.setLength(0);
				}
			}
		}
	}

	/**
	 * 클래스명 추출
	 */
	private static String extractClassName(String line) {
		String[] parts = line.split("\\s+");
		for (int i = 0; i < parts.length - 1; i++) {
			if ("class".equals(parts[i])) {
				return parts[i + 1].replaceAll("[{<].*", "");
			}
		}
		return "Unknown";
	}

	/**
	 * 메서드명 추출
	 */
	private static String extractMethodName(String line) {
		int parenIndex = line.indexOf('(');
		if (parenIndex > 0) {
			String beforeParen = line.substring(0, parenIndex).trim();
			String[] parts = beforeParen.split("\\s+");
			return parts[parts.length - 1];
		}
		return "Unknown";
	}

	/**
	 * URL 추출
	 */
	private static String extractUrl(String line) {
		int start = line.indexOf('"');
		if (start != -1) {
			int end = line.indexOf('"', start + 1);
			if (end > start) {
				return line.substring(start + 1, end);
			}
		}
		return "";
	}

	/**
	 * HTTP 메서드 추출
	 */
	private static String extractHttpMethod(String line) {
		if (line.contains("@GetMapping"))
			return "GET";
		if (line.contains("@PostMapping"))
			return "POST";
		if (line.contains("@PutMapping"))
			return "PUT";
		if (line.contains("@DeleteMapping"))
			return "DELETE";
		if (line.contains("@RequestMapping")) {
			if (line.contains("RequestMethod.POST"))
				return "POST";
			if (line.contains("RequestMethod.PUT"))
				return "PUT";
			if (line.contains("RequestMethod.DELETE"))
				return "DELETE";
			return "GET";
		}
		return "";
	}

	/**
	 * 주석 정리
	 */
	private static String cleanComment(String comment) {
		if (comment.isEmpty())
			return "주석없음";

		String cleaned = comment.replaceAll("/\\*\\*|\\*/", "").replaceAll("\\*", "").replaceAll("\\s+", " ").trim();

		return cleaned.length() > 50 ? cleaned.substring(0, 50) + "..." : cleaned;
	}

	/**
	 * 결과 파일 저장
	 */
	private static void saveResults(String outputPath, Map<String, StringBuilder> results) throws IOException {
		for (Map.Entry<String, StringBuilder> entry : results.entrySet()) {
			String fileName = entry.getKey() + ".csv";
			Path filePath = Paths.get(outputPath, fileName);

			try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
				writer.write(entry.getValue().toString());
			}

			System.out.println("저장 완료: " + fileName);
		}
	}

}
