package parser.parser;

import java.util.StringJoiner;

import parser.data.ParseResult;

public class ServiceParser extends BaseJavaParser {

	@Override
	protected void parseSpecialized(String line) {
		// 향후 트랜잭션, 의존성 분석 등을 위한 확장 포인트
	}

	@Override
	protected void addSpecializedDataToResult(ParseResult result) {
		buildOutputContent(result);
	}

	private void buildOutputContent(ParseResult result) {
		StringBuilder content = new StringBuilder(); // StringBuffer → StringBuilder

		result.getMethods().forEach(method -> {
			StringJoiner joiner = new StringJoiner("|");

			joiner.add(result.getClassName() + "." + method.getName())
				  .add(result.getClassName())
				  .add(result.getClassComment())
				  .add(method.getName())
				  .add(method.getComment());

			content.append(joiner.toString()).append("\n");
		});

		result.appendOutput(content.toString());
	}

	@Override
	protected void resetSpecializedState() {
		// 특별한 상태 초기화 없음
	}
}