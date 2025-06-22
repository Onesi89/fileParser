package parser.result;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import parser.data.ParseResult;

public class DistributedBufferedWriter {
	private String outputPath;
	private Map<String, StringBuffer> buffers;
	private Map<String, Integer> bufferSizes;
	private Map<String, Integer> fileCounters;
	private Map<String, Long> currentFileSizes;
	private Map<String, Boolean> firstWrite; // 첫 번째 쓰기 여부 추적

	private final int BUFFER_THRESHOLD;
	private final long FILE_SIZE_LIMIT;
	private final String LAST_NAME = ".csv";
	
	String[] types;

	public DistributedBufferedWriter(String outputPath, int bufferThresholdKB, int fileSizeLimitMB, String[] types) throws Exception {
		this.outputPath = outputPath;
		this.BUFFER_THRESHOLD = bufferThresholdKB * 1024;
		this.FILE_SIZE_LIMIT = fileSizeLimitMB * 1024L * 1024L;

		this.buffers = new HashMap<>();
		this.bufferSizes = new HashMap<>();
		this.fileCounters = new HashMap<>();
		this.currentFileSizes = new HashMap<>();
		this.firstWrite = new HashMap<>();
		this.types = types;
		
		initializeBuffers();
		cleanupExistingFiles(); // 기존 파일 정리

		try {
			Files.createDirectories(Paths.get(outputPath));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initializeBuffers() throws Exception{
		if(this.types.length < 1) { 
			throw new Exception("ERROR");
		}
		
		for (String type : this.types) {
			buffers.put(type, new StringBuffer());
			bufferSizes.put(type, 0);
			fileCounters.put(type, 1);
			currentFileSizes.put(type, 0L);
			firstWrite.put(type, true);
		}
	}

	private void cleanupExistingFiles() {
		for (String type :this.types) {
			try {
				// 기본 파일 삭제
				Path mainFile = Paths.get(outputPath, type + LAST_NAME);
				Files.deleteIfExists(mainFile);

				// 분산된 파일들도 삭제 (최대 10개까지 체크)
				for (int i = 2; i <= 10; i++) {
					Path splitFile = Paths.get(outputPath, type + "_" + i + LAST_NAME);
					if (Files.exists(splitFile)) {
						Files.deleteIfExists(splitFile);
					}
				}

				System.out.println("기존 " + type + " 파일들 삭제 완료");

			} catch (IOException e) {
				System.err.println("파일 정리 중 오류: " + e.getMessage());
			}
		}
	}

	public void writeResult(ParseResult result) {
		String type = result.getParserType();
		String content = result.getOutputContent();

		buffers.get(type).append(content);
		bufferSizes.put(type, bufferSizes.get(type) + content.getBytes().length);

		if (bufferSizes.get(type) >= BUFFER_THRESHOLD) {
			flushBuffer(type);
		}
	}

	private void flushBuffer(String type) {
		try {
			StringBuffer buffer = buffers.get(type);
			if (buffer.length() == 0)
				return;

			int contentSize = buffer.toString().getBytes().length;
			long currentFileSize = currentFileSizes.get(type);

			if (currentFileSize + contentSize > FILE_SIZE_LIMIT) {
				moveToNextFile(type);
			}

			Path filePath = getCurrentFilePath(type);

			// 첫 번째 쓰기면 덮어쓰기(CREATE), 이후는 추가(APPEND)
			StandardOpenOption writeOption = firstWrite.get(type) ? StandardOpenOption.CREATE
					: StandardOpenOption.APPEND;

			try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE,
					writeOption)) {

				writer.write(buffer.toString());

				currentFileSizes.put(type, currentFileSizes.get(type) + contentSize);
				buffers.get(type).setLength(0);
				bufferSizes.put(type, 0);
				firstWrite.put(type, false); // 첫 번째 쓰기 완료

				System.out.println(type + " 버퍼 플러시: " + filePath.getFileName() + " (현재 크기: "
						+ formatSize(currentFileSizes.get(type)) + ")");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void moveToNextFile(String type) {
		int currentCounter = fileCounters.get(type);
		fileCounters.put(type, currentCounter + 1);
		currentFileSizes.put(type, 0L);
		
		firstWrite.put(type, true); // 새 파일은 첫 번째 쓰기로 설정

		System.out.println(
				type + " 파일 분산: " + getCurrentFileName(type) + " (크기 제한 " + formatSize(FILE_SIZE_LIMIT) + " 초과)");
	}

	private Path getCurrentFilePath(String type) {
		return Paths.get(outputPath, getCurrentFileName(type));
	}

	private String getCurrentFileName(String type) {
		int counter = fileCounters.get(type);
		if (counter == 1) {
			return type + LAST_NAME;
		} else {
			return type + "_" + counter + LAST_NAME;
		}
	}

	public void flushAll() {
		buffers.keySet().forEach(this::flushBuffer);
		printFinalSummary();
	}

	private void printFinalSummary() {
		System.out.println("\n=== 파일 결과 ===");
		fileCounters.forEach((type, count) -> {
			System.out.println(type + ": " + count + "처리");
			for (int i = 1; i <= count; i++) {
				String fileName = (i == 1) ? type + LAST_NAME : type + "_" + i + LAST_NAME;
				Path filePath = Paths.get(outputPath, fileName);
				try {
					long size = Files.size(filePath);
					System.out.println("  - " + fileName + ": " + formatSize(size));
				} catch (IOException e) {
					System.out.println("  - " + fileName + ": 생성 실패");
				}
			}
		});
	}

	private String formatSize(long bytes) {
		if (bytes < 1024)
			return bytes + " B";
		if (bytes < 1024 * 1024)
			return (bytes / 1024) + " KB";
		return (bytes / (1024 * 1024)) + " MB";
	}

	public void printCurrentStatus() {
		System.out.println("\n=== 현재 상태 ===");
		bufferSizes.forEach((type, bufferSize) -> {
			long currentFileSize = currentFileSizes.get(type);
			int fileNumber = fileCounters.get(type);
			System.out.println(type + " [파일#" + fileNumber + "]: " + formatSize(currentFileSize) + " + 버퍼("
					+ formatSize(bufferSize) + ")");
		});
	}
}
