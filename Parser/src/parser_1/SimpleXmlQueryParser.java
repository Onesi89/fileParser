package parser_1;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleXmlQueryParser {

	private final static String[] START_TAGS = { "<select", "<delete", "<update", "<insert" };
	private final static String[] END_TAGS = { "</select", "</delete", "</update", "</insert" };

	private static final Pattern START_PATTERN = Pattern.compile("<(select|delete|update|insert)\\s+id=\"([^\"]+)\"",
			Pattern.CASE_INSENSITIVE);

	// SELECT 쿼리에서 FROM 절을 찾아 그 안의 테이블 이름들을 추출하는 패턴 (서브쿼리 포함)
	private static final Pattern SELECT_TABLE_PATTERN = Pattern.compile(
			"\\b(?:FROM|JOIN|INNER\\s+JOIN|LEFT\\s+JOIN|RIGHT\\s+JOIN|FULL\\s+JOIN)\\s+([a-zA-Z0-9_\\.]+)",
			Pattern.CASE_INSENSITIVE);

	// INSERT 쿼리에서 INTO 바로 뒤에 오는 테이블 이름
	private static final Pattern INSERT_TABLE_PATTERN = Pattern.compile("\\bINTO\\s+([a-zA-Z0-9_\\.]+)",
			Pattern.CASE_INSENSITIVE);

	// UPDATE 쿼리에서 UPDATE 바로 뒤에 오는 테이블 이름
	private static final Pattern UPDATE_TABLE_PATTERN = Pattern.compile("\\bUPDATE\\s+([a-zA-Z0-9_\\.]+)",
			Pattern.CASE_INSENSITIVE);

	// DELETE 쿼리에서 FROM 바로 뒤에 오는 테이블 이름 (DELETE TABLE_NAME 도 고려)
	private static final Pattern DELETE_TABLE_PATTERN = Pattern.compile("\\bDELETE(?:\\s+FROM)?\\s+([a-zA-Z0-9_\\.]+)",
			Pattern.CASE_INSENSITIVE);

	public static void main(String[] args) {
		String inputPath = "C:/Users/kimwp/OneDrive/Desktop/code/parser/MVC/mapper";
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
		System.out.println("=== QUERY 파일 파싱 시작 ===");
		System.out.println("입력: " + inputPath);
		System.out.println("출력: " + outputPath);

		Files.createDirectories(Paths.get(outputPath));

		Map<String, StringBuilder> results = new HashMap<>();
		results.put("query", new StringBuilder());

		AtomicInteger count = new AtomicInteger(0);

		// 파일 스캔 및 파싱
		Files.walk(Paths.get(inputPath)).filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".xml"))
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
		String currentTagName = "";
		String currentQueryId = "";

		StringBuilder currentSqlContent = new StringBuilder();
		StringBuilder currentComment = new StringBuilder();

		boolean inJavaDoc = false;
		boolean inQuery = false;

		System.out.println("파싱: " + fileName);

		try (BufferedReader reader = Files.newBufferedReader(filePath)) {
			String line;

			while ((line = reader.readLine()) != null) {
				System.out.println(line);
				line = line.trim();
				if (line.isEmpty())
					continue;

				if (line.startsWith("/**")) {
					inJavaDoc = true;
					currentComment.setLength(0);
					continue;
				} else if (inJavaDoc && line.contains("*/")) {
					inJavaDoc = false;
					continue;
				} else if (inJavaDoc) {
					currentComment.append(line);
					continue;
				}

				Matcher startTagMatcher = START_PATTERN.matcher(line);
				if (startTagMatcher.find()) {
					currentSqlContent.setLength(0);

					inQuery = true;
					currentTagName = startTagMatcher.group(1);
					currentQueryId = startTagMatcher.group(2);
					continue;
				}

				if (inQuery && isQueryEnd(line)) {
					// 출력
					extractAndOutputQueryInfo(filePath, currentQueryId, currentTagName, currentComment.toString(),
							currentSqlContent.toString(), results);

					// 초기화
					inQuery = false;
					currentTagName = "";
					currentQueryId = "";
					currentSqlContent.setLength(0);
					currentComment.setLength(0);
					continue;
				}

				if (inQuery) {
					currentSqlContent.append(line).append("\n");
				}
			}
		}
	}

	/**
	 * 추출된 쿼리 정보를 CSV 형식으로 결과 맵에 추가합니다.
	 */
	private static void extractAndOutputQueryInfo(Path filePath, String queryId, String tagName, String comment,
			String sqlContent, Map<String, StringBuilder> results) {
		// 주석이 없을 경우 null 대신 빈 문자열로 처리
		String finalComment = (comment != null) ? comment.trim() : "";

		// findTableName 함수에 tagName 인자 추가
		List<String> usedTables = findTableName(sqlContent, tagName);
		String tablesString = String.join(";", usedTables); // 테이블 목록을 세미콜론으로 구분된 문자열로 변환

		// CSV 형식으로 결과 저장
		results.get("query").append(filePath.toString()).append("|").append(escapeForCsv(queryId)).append("|")
				.append(escapeForCsv(tagName)).append("|").append(escapeForCsv(finalComment)).append("|")
				.append(escapeForCsv(tablesString)).append("\n");
	}

	private static boolean isQueryEnd(String line) {
		String query = line.toLowerCase().toString();
		return Arrays.stream(END_TAGS).anyMatch(query::startsWith);
	}

	/**
	 * CSV 출력 시 문자열 내의 콤마, 따옴표 등을 이스케이프 처리합니다.
	 */
	private static String escapeForCsv(String value) {
		if (value == null) {
			return "";
		}
		// 문자열에 콤마, 큰따옴표, 줄바꿈이 포함되어 있으면 큰따옴표로 감싸고,
		// 문자열 내의 큰따옴표는 두 개의 큰따옴표로 이스케이프합니다.
		if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
			return "\"" + value.replace("\"", "\"\"") + "\"";
		}
		return value;
	}

	/**
	 * 쿼리 테이블명 추출
	 * 
	 * @param sqlContent SQL 쿼리 전체 내용
	 * @param tagName    쿼리 태그 이름 (select, insert, update, delete)
	 * @return 추출된 테이블 이름 목록
	 */
	private static List<String> findTableName(String sqlContent, String tagName) {
		List<String> tables = new ArrayList<>();
		Pattern patternToUse = null;

		switch (tagName.toLowerCase()) {
		case "select":
			patternToUse = SELECT_TABLE_PATTERN; // SELECT_TABLE_PATTERN 사용
			break;
		case "insert":
			patternToUse = INSERT_TABLE_PATTERN;
			break;
		case "update":
			patternToUse = UPDATE_TABLE_PATTERN;
			break;
		case "delete":
			patternToUse = DELETE_TABLE_PATTERN;
			break;
		default:
			return Collections.emptyList();
		}

		Matcher matcher = patternToUse.matcher(sqlContent);
		while (matcher.find()) {
			String tableName = matcher.group(1).trim();
			// 스키마.테이블 형태에서 스키마만 있는 경우(예: "schema.")는 제외
			if (!tableName.endsWith(".")) {
				tables.add(tableName);
			}
		}
		return tables;
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
