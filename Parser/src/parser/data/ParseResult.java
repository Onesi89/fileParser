package parser.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParseResult {
	private String fileName;
	private String filePath;
	private String parserType;
	private String className;
	private String classComment;
	private List<MethodInfo> methods;
	private Map<String, Object> specializedData;
	private StringBuffer outputContent;

	public ParseResult(String fileName, String filePath, String parserType) {
		this.fileName = fileName;
		this.filePath = filePath;
		this.parserType = parserType;
		this.methods = new ArrayList<>();
		this.specializedData = new HashMap<>();
		this.outputContent = new StringBuffer();
	}

	// Getters
	public String getFileName() {
		return fileName;
	}

	public String getFilePath() {
		return filePath;
	}

	public String getParserType() {
		return parserType;
	}

	public String getClassName() {
		return className;
	}

	public String getClassComment() {
		return classComment;
	}

	public List<MethodInfo> getMethods() {
		return methods;
	}

	public Map<String, Object> getSpecializedData() {
		return specializedData;
	}

	public String getOutputContent() {
		return outputContent.toString();
	}

	// Setters
	public void setClassName(String className) {
		this.className = className;
	}

	public void setClassComment(String classComment) {
		this.classComment = classComment;
	}

	public void setMethods(List<MethodInfo> methods) {
		this.methods = methods;
	}

	public void addSpecializedData(String key, Object value) {
		specializedData.put(key, value);
	}

	public void appendOutput(String content) {
		outputContent.append(content);
	}
}
