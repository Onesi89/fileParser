package parser.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import parser.data.ControllerMethodInfo;
import parser.data.MethodInfo;
import parser.data.ParseResult;

public class ControllerParser extends BaseJavaParser {
	// 정규식 패턴 미리 컴파일
	private static final Pattern MAPPING_PATTERN = Pattern.compile("@(Request|Get|Post|Put|Delete)Mapping");
	private static final Pattern URL_PATTERN = Pattern.compile("\"([^\"]+)\"");

	private String classCommonURL = "/";
	private List<ControllerMethodInfo> controllerMethods = new ArrayList<>();

	@Override
	protected void parseSpecialized(String line) {
		if (MAPPING_PATTERN.matcher(line).find()) {
			parseMappingAnnotation(line);
		}

		if (className.isEmpty() && context.hasPendingMapping()) {
			classCommonURL = context.pendingUrl;
			context.resetMapping();
		} else if (isMethodDeclaration(line) && context.hasPendingMapping()) {
			MethodInfo target = methods.get(methods.size() - 1);

			ControllerMethodInfo controllerMethod = new ControllerMethodInfo(target.getName(), target.getComment(),
					context.pendingUrl, context.pendingHttpMethod);

			controllerMethods.add(controllerMethod);
			context.resetMapping();
		}
	}

	private boolean isMethodDeclaration(String line) {
		return line.matches(".*(public|private|protected).*\\(.*\\).*\\{.*");
	}

	private void parseMappingAnnotation(String line) {
		// HTTP 메서드 추출 최적화
		context.pendingHttpMethod = extractHttpMethod(line);

		// URL 추출 최적화
		String extractedUrl = extractMapping(line);
		if (!extractedUrl.isEmpty()) {
			context.pendingUrl = extractedUrl;
		}
	}

	private String extractHttpMethod(String line) {
		if (line.contains("@GetMapping"))
			return "GET";
		if (line.contains("@PostMapping"))
			return "POST";
		if (line.contains("@PutMapping"))
			return "PUT";
		if (line.contains("@DeleteMapping"))
			return "DELETE";
		if (line.contains("@RequestMapping")) {
			return extractHttpMethodFromRequestMapping(line);
		}
		return "GET"; // 기본값
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
		return "GET";
	}

	private String extractMapping(String line) {
		java.util.regex.Matcher matcher = URL_PATTERN.matcher(line);
		return matcher.find() ? matcher.group(1) : "";
	}

	@Override
	protected void addSpecializedDataToResult(ParseResult result) {
		List<MethodInfo> methodInfoList = new ArrayList<>(controllerMethods);
		result.setMethods(methodInfoList);
		result.addSpecializedData("controllerMethods", controllerMethods);
		buildOutputContent(result);
	}

	private void buildOutputContent(ParseResult result) {
		StringBuilder content = new StringBuilder(); // StringBuffer → StringBuilder

		controllerMethods.forEach(method -> {
			StringJoiner joiner = new StringJoiner("|");

			joiner.add(result.getClassName())
				  .add(result.getClassComment())
			      .add(classCommonURL)
			      .add(method.getUrl())
				  .add(method.getName())
			      .add(method.getComment());

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
