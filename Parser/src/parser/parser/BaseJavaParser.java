package parser.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import parser.data.MethodInfo;
import parser.data.ParseContext;
import parser.data.ParseResult;

abstract public class BaseJavaParser implements FileParser {
	// 정규식 패턴 미리 컴파일 및 static 선언으로 메모리 절약
	private static final Pattern METHOD_PATTERN = Pattern.compile(".*(public|private|protected).*\\(.*\\).*\\{.*");
	private static final Pattern CLASS_PATTERN = Pattern.compile(".*class\\s+\\w+.*");
	private static final Pattern JAVADOC_START_PATTERN = Pattern.compile("\\s*/\\*\\*.*");
	private static final Pattern JAVADOC_END_PATTERN = Pattern.compile(".*\\*/.*");

	protected String className = "";
	protected String classComment = "";
	protected final int commentMaxLength = 30;
	protected List<MethodInfo> methods = new ArrayList<>();
	protected ParseContext context = new ParseContext();

	@Override
	public ParseResult parse(Path filePath, String parserType) {
		ParseResult result = new ParseResult(filePath.getFileName().toString(), filePath.toString(), parserType);

		try (BufferedReader reader = Files.newBufferedReader(filePath)) {
			System.out.println("파싱 중: " + filePath.getFileName());

			resetParserState();
			parseFile(reader);

			result.setClassName(className);
			result.setClassComment(classComment);
			result.setMethods(new ArrayList<>(methods));

			addSpecializedDataToResult(result);

		} catch (IOException ie) {
			System.err.println("파일 읽기 오류: " + filePath + " - " + ie.getMessage());
		} catch (Exception e) {
			System.err.println("파싱 오류: " + filePath + " - " + e.getMessage());
		}

		return result;
	}

	private void resetParserState() {
		className = "";
		classComment = "";
		methods.clear();
		context.reset(); // ParseContext에 전체 초기화 메서드 추가
		resetSpecializedState();
	}

	private void parseFile(BufferedReader reader) throws IOException {
		String line;

		while ((line = reader.readLine()) != null) {
			line = line.trim(); // 미리 trim 처리로 중복 호출 방지
			if (line.isEmpty())
				continue; // 빈 줄 스킵

			updateContext(line);

			if (JAVADOC_START_PATTERN.matcher(line).matches()) {
				context.inJavaDoc = true;
				context.startComment(line);
			} else if (context.inJavaDoc && JAVADOC_END_PATTERN.matcher(line).matches()) {
				context.inJavaDoc = false;
			} else if (context.inJavaDoc && !isJavaDocAnnotation(line)) {
				context.appendComment(line);
			} else if (CLASS_PATTERN.matcher(line).matches() && context.braceLevel == 1) {
				className = extractClassName(line);
				classComment = context.hasComment() ? extractComment(context.getComment()) : "주석 없음";
				context.reset();
			} else if (METHOD_PATTERN.matcher(line).matches()) {
				String methodName = extractMethodName(line);
				String methodComment = context.hasComment() ? extractComment(context.getComment()) : "주석 없음";
				methods.add(new MethodInfo(methodName, methodComment));
				context.reset();
			}

			parseSpecialized(line);
		}
	}

	private boolean isJavaDocAnnotation(String line) {
		return line.contains("@param") || line.contains("@return") || line.contains("@throws")
				|| line.contains("@author");
	}

	private void updateContext(String line) {
		context.braceLevel += countOccurrences(line, '{');
		context.braceLevel -= countOccurrences(line, '}');
	}

	private int countOccurrences(String str, char ch) {
		int count = 0;
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == ch)
				count++;
		}
		return count;
	}

	protected String extractClassName(String line) {
		String[] parts = line.split("\\s+");
		for (int i = 0; i < parts.length - 1; i++) {
			if ("class".equals(parts[i])) {
				return parts[i + 1].replaceAll("[{<].*", ""); // 제네릭 타입도 제거
			}
		}
		return "Unknown";
	}

	protected String extractMethodName(String line) {
		int parenIndex = line.indexOf('(');
		if (parenIndex > 0) {
			String beforeParen = line.substring(0, parenIndex).trim();
			String[] parts = beforeParen.split("\\s+");
			return parts[parts.length - 1];
		}
		return "Unknown";
	}

	protected String extractComment(String comment) {
		String formatComment = comment.replaceAll("/\\*\\*|\\*/", "").replaceAll("(?m)^\\s*\\*", "")
				.replaceAll("\\s+", " ") // 모든 공백을 단일 공백으로
				.trim();

		return formatComment.length() > commentMaxLength ? formatComment.substring(0, commentMaxLength) + "..."
				: formatComment;
	}

	protected abstract void resetSpecializedState();

	protected abstract void parseSpecialized(String line);

	protected abstract void addSpecializedDataToResult(ParseResult result);
}
