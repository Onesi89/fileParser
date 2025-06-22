package parser.parser;

import java.nio.file.Path;

import parser.data.ParseResult;

public interface FileParser {
	ParseResult parse(Path filePath, String parserType);
}
