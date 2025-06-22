package parser.data;

import java.util.ArrayList;
import java.util.List;

public class ParseContext {
	public String currentClass = "";
	public boolean inJavaDoc = false;
	public int braceLevel = 0;

	// StringBuilder로 주석 처리 최적화
	private StringBuilder commentBuilder = new StringBuilder();

	// 컨트롤러 전용 컨텍스트
	public String pendingUrl = "";
	public String pendingHttpMethod = "";
	public List<String> pendingParameters = new ArrayList<>();

	public void reset() {
		commentBuilder.setLength(0); // StringBuilder 초기화
		inJavaDoc = false;
	}

	public void resetMapping() {
		pendingUrl = "";
		pendingHttpMethod = "";
		pendingParameters.clear();
	}

	public void resetAll() {
		reset();
		resetMapping();
		currentClass = "";
		braceLevel = 0;
	}

	public boolean hasPendingMapping() {
		return !pendingUrl.isEmpty() || !pendingHttpMethod.isEmpty();
	}

	// 주석 관련 메서드들
	public void startComment(String line) {
		commentBuilder.setLength(0);
		commentBuilder.append(line).append("\n");
	}

	public void appendComment(String line) {
		commentBuilder.append(line).append("\n");
	}

	public String getComment() {
		return commentBuilder.toString();
	}

	public boolean hasComment() {
		return commentBuilder.length() > 0;
	}
}
