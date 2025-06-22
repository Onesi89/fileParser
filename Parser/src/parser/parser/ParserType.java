package parser.parser;

import java.util.Arrays;

public enum ParserType {
	CONTROLLER("controller"), 
	SERVICE_CBC("cbc"), 
	SERVICE_BC("bc"), 
	SERVICE_QC("qc"), 
	GENERAL("");

	private final String pathKeyword;

	ParserType(String pathKeyword) {
		this.pathKeyword = pathKeyword;
	}

	public String getPathKeyword() {
		return pathKeyword;
	}

	// 경로에서 파서 타입 결정 (단순 판별만)
	public static ParserType fromPath(String path) {
		String lowerPath = path.toLowerCase();
		return Arrays.stream(values())
				.filter(type -> !type.pathKeyword.isEmpty() && lowerPath.contains(type.pathKeyword)).findFirst()
				.orElse(GENERAL);
	}

	// 모든 경로 키워드 반환
	public static String[] getAllPathKeywords() {
		return Arrays.stream(values()).filter(type -> !type.pathKeyword.isEmpty()).map(ParserType::getPathKeyword)
				.toArray(String[]::new);
	}

	// 서비스 타입만 필터링
	public static String[] getServicePathKeywords() {
		return Arrays.stream(values()).filter(type -> type == SERVICE_CBC || type == SERVICE_BC || type == SERVICE_QC)
				.map(ParserType::getPathKeyword).toArray(String[]::new);
	}

	// 서비스 타입 여부 체크
	public boolean isServiceType() {
		return this == SERVICE_CBC || this == SERVICE_BC || this == SERVICE_QC;
	}

}
