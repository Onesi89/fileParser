package parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import parser.data.ParseResult;
import parser.parser.FileParser;
import parser.parser.ParserFactory;
import parser.parser.ParserType;
import parser.result.DistributedBufferedWriter;

public class JavaFileParsor {
	private JavaFileParsor() {
		// 유틸리티 클래스이므로 인스턴스 생성 방지
	}

	// 파일 경로 필터링 (ParserType의 경로 키워드 활용)
    public static boolean isJavaFileInTargetPath(String pathStr) {
        ParserType type = ParserType.fromPath(pathStr);
        return type != ParserType.GENERAL;  // GENERAL 타입은 파싱하지 않음
    }
    
    // 특정 타입들만 파싱하고 싶을 때 (선택적 사용)
    public static boolean isTargetParserType(ParserType type) {
        switch (type) {
            case CONTROLLER:
            case SERVICE_CBC:
            case SERVICE_BC:
            case SERVICE_QC:
                return true;
            case GENERAL:
            default:
                return false;
        }
    }

	// 서비스 타입 판별 (비즈니스 로직)
	public static String getServiceType(String pathStr) {
		ParserType type = ParserType.fromPath(pathStr);
		return type.isServiceType() ? type.getPathKeyword() : "";
	}

	// 파일 파싱 (Factory를 통한 파서 획득 및 실행)
	public static ParseResult parseJavaFile(Path filePath) {
		ParserType type = ParserType.fromPath(filePath.toString());
		FileParser parser = ParserFactory.getParser(type);

		return parser.parse(filePath, type.getPathKeyword());
	}

	public static void processProject(String rootPath, String outputPath) throws Exception {
		processProject(rootPath, outputPath, 50, 5);
	}

	public static void processProject(String rootPath, String outputPath, int bufferKB, int fileMB) throws Exception {
		try {
			System.out.println("프로젝트 스캔 시작: " + rootPath);
			System.out.println("출력 경로: " + outputPath);
			System.out.println("설정 - 버퍼: " + bufferKB + "KB, 파일 제한: " + fileMB + "MB");
			System.out.println("=".repeat(60));

			// ParserType에서 경로 키워드 가져오기
			String[] targetPaths = ParserType.getAllPathKeywords();
			DistributedBufferedWriter writer = new DistributedBufferedWriter(outputPath, bufferKB, fileMB, targetPaths);
			AtomicInteger processedCount = new AtomicInteger(0);

			Files.walk(Paths.get(rootPath))
					.filter(Files::isRegularFile)
					.filter(path -> path.toString().endsWith(".java"))
					.filter(path -> isJavaFileInTargetPath(path.toString()))
					.map(JavaFileParsor::parseJavaFile)
					.forEach(result -> {
						writer.writeResult(result);

						if (processedCount.incrementAndGet() % 100 == 0) {
							System.out.println("처리 완료: " + processedCount.get() + "개 파일");
							writer.printCurrentStatus();
						}
					});

			writer.flushAll();

			System.out.println("=".repeat(60));
			System.out.println("분석 완료! 총 " + processedCount.get() + "개 파일 처리");

		} catch (IOException e) {
			System.err.println("프로젝트 스캔 중 오류 발생: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		String projectPath = "C:/Users/kimwp/OneDrive/Desktop/code/parser/MVC/src/mvc";
		String outputPath = "C:\\Users\\kimwp\\OneDrive\\Desktop\\code\\test";

		try {
			processProject(projectPath, outputPath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}