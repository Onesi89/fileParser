package parser.data;

public class MethodInfo {
	private String name;
	private String comment;

	public MethodInfo(String name, String comment) {
		this.name = name;
		this.comment = comment;
	}

	public String getName() {
		return name;
	}

	public String getComment() {
		return comment;
	}
}
