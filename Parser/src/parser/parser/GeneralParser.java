package parser.parser;

import parser.data.ParseResult;

public class GeneralParser extends BaseJavaParser {
    
	@Override
    protected void parseSpecialized(String line) {
        // 일반적인 특화 파싱은 없음
    }
    
    @Override
    protected void addSpecializedDataToResult(ParseResult result) {
        buildOutputContent(result);
    }
    
    private void buildOutputContent(ParseResult result) {
//        StringBuffer content = new StringBuffer();
//        
//        content.append("## ").append(result.getClassName()).append("\n");
//        content.append("**파일**: ").append(result.getFileName()).append("\n");
//        content.append("**타입**: 일반 클래스\n");
//        content.append("**설명**: ").append(result.getClassComment()).append("\n");
//        
//        content.append("\n### 📄 메소드들\n");
//        result.getMethods().forEach(method -> {
//            content.append("- **").append(method.getName()).append("()**: ")
//                   .append(method.getComment()).append("\n");
//        });
//        
//        content.append("\n---\n\n");
//        result.appendOutput(content.toString());
    }

	@Override
	protected void resetSpecializedState() {
		// TODO Auto-generated method stub
		
	}
}