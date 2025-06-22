package parser.parser;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

public class ParserFactory {
	// 파서 인스턴스 캐싱
	private static final Map<ParserType, FileParser> PARSER_CACHE = new EnumMap<>(ParserType.class);

	private ParserFactory() {
		// 유틸리티 클래스이므로 인스턴스 생성 방지
	}

	// 파일 경로로부터 적절한 파서 반환
	public static FileParser getParser(Path filePath) {
		ParserType type = ParserType.fromPath(filePath.toString());
		return getParser(type);
	}

	// 파서 타입으로부터 파서 반환 (캐싱 적용)
	public static FileParser getParser(ParserType type) {
		return PARSER_CACHE.computeIfAbsent(type, ParserFactory::createParser);
	}

	// 파서 타입에 따른 파서 인스턴스 생성
	private static FileParser createParser(ParserType type) {
		switch (type) {
		case CONTROLLER:
			return new ControllerParser();
		case SERVICE_CBC:
		case SERVICE_BC:
		case SERVICE_QC:
			return new ServiceParser();
		case GENERAL:
		default:
			return new GeneralParser();
		}
	}

	// 테스트나 메모리 정리용
	public static void clearCache() {
		PARSER_CACHE.clear();
	}
}
