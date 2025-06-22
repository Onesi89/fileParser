package parser_1;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleJspParser {

	public static void main(String[] args) {
		String inputPath = "C:/Users/kimwp/OneDrive/Desktop/code/parser/MVC/jsp";
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
		System.out.println("=== JSP 파일 파싱 시작 ===");
		System.out.println("입력: " + inputPath);
		System.out.println("출력: " + outputPath);

		Files.createDirectories(Paths.get(outputPath));

		Map<String, StringBuilder> results = new HashMap<>();
		results.put("jsp", new StringBuilder());

		AtomicInteger count = new AtomicInteger(0);

		// 파일 스캔 및 파싱
		Files.walk(Paths.get(inputPath)).filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".jsp"))
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

	private static void parseFile(Path filePath, Map<String, StringBuilder> results) throws IOException {
		String fileName = filePath.getFileName().toString();
		boolean inJavaDoc = false;

		System.out.println("파싱: " + fileName);

		try (BufferedReader reader = Files.newBufferedReader(filePath)) {
			String line;
			
			// 화면 호출 jsp 위치 추가 필요
			results.get("jsp")
				   .append(String.join("|" ,fileName, fileName.replaceAll(".jsp",  "").concat(".do")))
				   .append("\n");

			while ((line = reader.readLine()) != null) {
				System.out.println(line);
				line = line.trim();
				if (line.isEmpty())
					continue;

				if (line.startsWith("/**")) {
					inJavaDoc = true;
				} else if (inJavaDoc && line.contains("*/")) {
					inJavaDoc = false;
				} else if(inJavaDoc) {
					continue;
				} else if(line.contains(".do")) {
					results.get("jsp")
					   .append(String.join("|" ,fileName,extractUrl(line)))
					   .append("\n");
				}
			}
		}

	}
	
	/**
	 * URL 추출
	 */
	private static String extractUrl(String line) {
		int start = line.indexOf("/app");
		if (start != -1) {
			int end   = line.indexOf(".do");
			if (end > start) {
				return line.substring(start, end+3);
			}
		}
		return "";
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
