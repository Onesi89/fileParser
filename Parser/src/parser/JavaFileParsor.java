package parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

import parser.data.ParseResult;
import parser.parser.ControllerParser;
import parser.parser.GeneralParser;
import parser.parser.FileParser;
import parser.parser.ServiceParser;
import parser.result.DistributedBufferedWriter;

public class JavaFileParsor {
	final static String CONTROLLER_PATH_NAME = "controller";
	final static String CBC_PATH_NAME = "cbc";
	final static String BC_PATH_NAME = "bc";
	final static String QC_PATH_NAME = "qc";
	
	final static String[] ALL_PATH_LIST = new String[] {CONTROLLER_PATH_NAME, CBC_PATH_NAME, BC_PATH_NAME, QC_PATH_NAME};
	final static String[] SERVICE_PATH_LIST = new String[] {CBC_PATH_NAME, BC_PATH_NAME, QC_PATH_NAME};
	
	private static final ControllerParser CONTROLLER_PARSER = new ControllerParser();
    private static final ServiceParser SERVICE_PARSER = new ServiceParser();
    private static final GeneralParser GENERAL_PARSER = new GeneralParser();
	
	private static String currentParserType = ""; 
	
	
	public static Path setType(Path pathObj) {
		currentParserType = Arrays.stream(ALL_PATH_LIST).filter(path -> pathObj.toString().contains(path)).findFirst().get();
		return pathObj;
	} 
	
	public static boolean filterPath(String pathStr) {
		return Arrays.stream(ALL_PATH_LIST).anyMatch(path -> pathStr.contains(path));
	}
	
	public static String getServiceType(String pathStr) {
		return Arrays.stream(SERVICE_PATH_LIST).filter(path -> pathStr.contains(path)).findFirst().get();
	}
	
	
	public static ParseResult parseJavaFile(Path filePath) {
		FileParser parser = createParser(filePath);
		return parser.parse(filePath, currentParserType);
	}

	private static FileParser createParser(Path filePath) {
		String pathStr = filePath.toString().toLowerCase();

		if (pathStr.contains(CONTROLLER_PATH_NAME)) {
			return CONTROLLER_PARSER;
		} else if (pathStr.contains(CBC_PATH_NAME) || pathStr.contains(BC_PATH_NAME) || pathStr.contains(QC_PATH_NAME)) {
			return SERVICE_PARSER;
		} else {
			return GENERAL_PARSER;
		}
	}

	public static void processProject(String rootPath, String outputPath) throws Exception {
		processProject(rootPath, outputPath, 50, 5); // 기본값: 50KB 버퍼, 5MB 파일 제한
	}

	public static void processProject(String rootPath, String outputPath, int bufferKB, int fileMB) throws Exception {
			try {
				System.out.println("프로젝트 스캔 시작: " + rootPath);
				System.out.println("출력 경로: " + outputPath);
				System.out.println("설정 - 버퍼: " + bufferKB + "KB, 파일 제한: " + fileMB + "MB");
				System.out.println("=".repeat(60));

				DistributedBufferedWriter writer = new DistributedBufferedWriter(outputPath, bufferKB, fileMB, ALL_PATH_LIST);
				AtomicInteger processedCount = new AtomicInteger(0);

				Files.walk(Paths.get(rootPath))
						.filter(Files::isRegularFile)
						.filter(path -> filterPath(path.toString()))
						.filter(path -> path.toString().endsWith(".java"))
						.map(path -> setType(path))
						.map(JavaFileParsor::parseJavaFile)
						.forEach(result -> {
							writer.writeResult(result);

							// 100개마다 상태 출력
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
//		Scanner scanner = new Scanner(System.in);
//        System.out.println("🔍 JConsole을 연결한 후 Enter를 누르세요...");
//        System.out.println("PID: " + ProcessHandle.current().pid());
//        scanner.nextLine(); // Enter 대기
        
//        System.out.println("📝 JavaDocExtractor 시작!");
        
		String projectPath = "C:/Users/kimwp/OneDrive/Desktop/code/parser/MVC/src/mvc";
		String outputPath = "C:\\Users\\kimwp\\OneDrive\\Desktop\\code\\test";

		// 기본 설정으로 실행
		try {
			processProject(projectPath, outputPath);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
//	    System.out.println("✅ 완료! JConsole 결과를 확인한 후 Enter를 누르세요...");
//        scanner.nextLine(); // 결과 확인 대기
//        
//        scanner.close();

		// 또는 커스텀 설정으로 실행
		// processProject(projectPath, outputPath, 100, 10); // 100KB 버퍼, 10MB 파일 제한
	}

}