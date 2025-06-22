package parser.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import parser.data.MethodInfo;
import parser.data.ParseContext;
import parser.data.ParseResult;

abstract public class BaseJavaParser implements FileParser {
	protected String className = "";
	protected String classComment = "";
	protected int commnetMaxLength = 30;
	protected List<MethodInfo> methods = new ArrayList<>();
	protected ParseContext context = new ParseContext();

	@Override
	public ParseResult parse(Path filePath, String parserType) {		
		ParseResult result = new ParseResult(filePath.getFileName().toString(), filePath.toString(), parserType);

		try (BufferedReader reader = Files.newBufferedReader(filePath)) {
			System.out.println("파싱 중: " + filePath.getFileName());

			resetParserState();
			parseFile(reader);

			// 결과 객체에 데이터 설정
			result.setClassName(className);
			result.setClassComment(classComment);
			result.setMethods(new ArrayList<>(methods));

			// 특화 데이터 및 출력 내용 생성
			addSpecializedDataToResult(result);

		} catch (IOException ie) {
			ie.printStackTrace();
		} catch (Exception  e) {
			e.printStackTrace();
		}

		return result;
	}

	private void resetParserState() {
		className = "";
		classComment = "";
		methods.clear();
		context = new ParseContext();
		resetSpecializedState();  // 각 파서별 추가 상태 초기화
	}

	private void parseFile(BufferedReader reader) throws IOException {
		String line;

		while ((line = reader.readLine()) != null) {
			updateContext(line);

			// JavaDoc 주석 처리
			if (line.trim().startsWith("/**")) {
				context.inJavaDoc = true;
				context.pendingComment = line + "\n";
			} else if (context.inJavaDoc && line.contains("*/")) {
//				context.pendingComment += line;
				context.inJavaDoc = false;
			} else if (context.inJavaDoc && !(line.contains("@param") || line.contains("@return"))) {
				context.pendingComment += line + "\n";
			} else if (line.contains("class ") && context.braceLevel == 1) {
				className = extractClassName(line);
				classComment = context.pendingComment.isEmpty() ? "주석 없음" : extractComment(context.pendingComment);
				context.reset();
			} else if (isMethodDeclaration(line)) {
				String methodName = extractMethodName(line);
				String methodComment = context.pendingComment.isEmpty() ? "주석 없음" : extractComment(context.pendingComment);
				methods.add(new MethodInfo(methodName, methodComment));
				context.reset();
			}

			// 특화 파싱 (공통 파싱과 함께 실행)
			parseSpecialized(line);
		}
	}

	private void updateContext(String line) {
		if (line.contains("{"))
			context.braceLevel++;
		if (line.contains("}"))
			context.braceLevel--;
	}

	// 공통 유틸리티 메소드들
	protected String extractClassName(String line) {
		String[] parts = line.split("\\s+");
		for (int i = 0; i < parts.length - 1; i++) {
			if (parts[i].equals("class")) {
				return parts[i + 1].replaceAll("[{].*", "");
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

	protected boolean isMethodDeclaration(String line) {
		return line.matches(".*(public|private|protected).*\\(.*\\).*\\{.*");
	}

	protected String extractComment(String comment) {
		String formatComment = comment.replaceAll("/\\*\\*|\\*/", "").replaceAll("(?m)^\\s*\\*", "")
				.replaceAll("\\n", "").trim();

		if (formatComment.length() > commnetMaxLength) {
			formatComment = formatComment.substring(0, commnetMaxLength);
		}
		return formatComment;
	}

	// 각 파서별로 구현해야 하는 추상 메소드들
    protected abstract void resetSpecializedState();
    
	protected abstract void parseSpecialized(String line);

	protected abstract void addSpecializedDataToResult(ParseResult result);
}
