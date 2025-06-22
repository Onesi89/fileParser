package parser.data;

import java.util.ArrayList;
import java.util.List;

public class ParseContext {
	public String currentClass = "";
	public String pendingComment = "";
	public boolean inJavaDoc = false;
	public int braceLevel = 0;

	// 컨트롤러 전용 컨텍스트
	public String pendingUrl = "";
	public String pendingHttpMethod = "";
	public List<String> pendingParameters = new ArrayList<>();

	public void reset() {
		pendingComment = "";
		inJavaDoc = false;
	}

	public void resetMapping() {
		pendingUrl = "";
		pendingHttpMethod = "";
		pendingParameters.clear();
	}

	public boolean hasPendingMapping() {
		return !pendingUrl.isEmpty() || !pendingHttpMethod.isEmpty();
	}
}
