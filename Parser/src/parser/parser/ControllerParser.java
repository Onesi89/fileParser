package parser.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import parser.data.ControllerMethodInfo;
import parser.data.MethodInfo;
import parser.data.ParseResult;

public class ControllerParser extends BaseJavaParser {

	private String classCommonURL = "/";
	private List<ControllerMethodInfo> controllerMethods = new ArrayList<>();

	/**
	 * URL 요청 읽기
	 */
	@Override
	protected void parseSpecialized(String line) {
		// 매핑 어노테이션 감지 및 파싱
		if (isMappingAnnotation(line)) {
			parseMappingAnnotation(line);
		}

		// 클래스 공통 URL 감지
		if(className.isEmpty() && context.hasPendingMapping()) {
			classCommonURL = context.pendingUrl;
			context.resetMapping();
		} else
		// 메소드 선언 감지 시 매핑 정보와 결합
			if (isMethodDeclaration(line) && context.hasPendingMapping()) {
			MethodInfo target = methods.get(methods.size() - 1); // 위 isMethodDeclaration(line)

			String methodName = target.getName();
			String methodComment = target.getComment().isEmpty() ? "주석 없음" : target.getComment();

			ControllerMethodInfo controllerMethod = new ControllerMethodInfo(methodName, methodComment,
					context.pendingUrl, context.pendingHttpMethod);

			controllerMethods.add(controllerMethod);
			context.resetMapping();
			context.reset();
		}
	}

	private boolean isMappingAnnotation(String line) {
		return line.contains("@RequestMapping") || line.contains("@GetMapping") || line.contains("@PostMapping")
				|| line.contains("@PutMapping") || line.contains("@DeleteMapping");
	}

	private void parseMappingAnnotation(String line) {
		// HTTP 메소드 추출
		if (line.contains("@GetMapping")) {
			context.pendingHttpMethod = "GET";
		} else if (line.contains("@PostMapping")) {
			context.pendingHttpMethod = "POST";
		} else if (line.contains("@PutMapping")) {
			context.pendingHttpMethod = "PUT";
		} else if (line.contains("@DeleteMapping")) {
			context.pendingHttpMethod = "DELETE";
		} else if (line.contains("@RequestMapping")) {
			context.pendingHttpMethod = extractHttpMethodFromRequestMapping(line);
		}

		// URL 추출
		String extractedUrl = extractMapping(line);
		if (!extractedUrl.isEmpty()) {
			context.pendingUrl = extractedUrl;
		}
	}

	private String extractHttpMethodFromRequestMapping(String line) {
		if (line.contains("RequestMethod.GET"))
			return "GET";
		if (line.contains("RequestMethod.POST"))
			return "POST";
		if (line.contains("RequestMethod.PUT"))
			return "PUT";
		if (line.contains("RequestMethod.DELETE"))
			return "DELETE";
		return "GET"; // 기본값
	}


	/**
	 * 따옴표("") 사이 문자열 추출
	 */
	private String extractMapping(String line) {
		if (line.contains("\"")) {
			int start = line.indexOf("\"") + 1;
			int end = line.indexOf("\"", start);
			if (end > start) {
				return line.substring(start, end);
			}
		}
		return "";
	}

	@Override
	protected void addSpecializedDataToResult(ParseResult result) {
		List<MethodInfo> methodInfoList = new ArrayList<>(controllerMethods);
		result.setMethods(methodInfoList);

		result.addSpecializedData("controllerMethods", controllerMethods);
		buildOutputContent(result);
	}

	private void buildOutputContent(ParseResult result) {
		StringBuffer content = new StringBuffer();

		controllerMethods.forEach(method -> {
			StringJoiner joiner = new StringJoiner("|");
			
			joiner.add(result.getClassName());
			joiner.add(result.getClassComment());
			joiner.add(classCommonURL);
			joiner.add(method.getUrl());
			joiner.add(method.getName());
			joiner.add(method.getComment());
			
			content.append(joiner.toString()).append("\n");
		});
		
		result.appendOutput(content.toString());
	}

	@Override
	protected void resetSpecializedState() {
		classCommonURL = "/";
		controllerMethods.clear(); 
		
	}
	
	
}
