package parser.parser;

import java.util.StringJoiner;

import parser.data.ParseResult;

public class ServiceParser extends BaseJavaParser {
//    private List<String> transactions = new ArrayList<>();
//    private List<String> dependencies = new ArrayList<>();
    

	@Override
    protected void parseSpecialized(String line) {
//        if (line.contains("@Transactional")) {
//            transactions.add("트랜잭션 메소드");
//        }
//        
//        if (line.contains("@Autowired") || line.contains("@Inject")) {
//            String dependency = extractDependency(line);
//            if (!dependency.isEmpty()) {
//                dependencies.add(dependency);
//            }
//        }
    }
    
//    private String extractDependency(String line) {
//        String[] parts = line.trim().split("\\s+");
//        if (parts.length >= 3) {
//            return parts[parts.length - 2];
//        }
//        return "";
//    }
    
    @Override
    protected void addSpecializedDataToResult(ParseResult result) {
//        result.addSpecializedData("transactions", transactions);
//        result.addSpecializedData("dependencies", dependencies);
        buildOutputContent(result);
    }
    
    private void buildOutputContent(ParseResult result) {
		StringBuffer content = new StringBuffer();
        
        result.getMethods().forEach(method -> {
        	StringJoiner joiner = new StringJoiner("|");
        	
        	joiner.add(result.getClassName().concat(".").concat(method.getName()));;
        	joiner.add(result.getClassName());
        	joiner.add(result.getClassComment());
        	joiner.add(method.getName());
        	joiner.add(method.getComment());
        	
        	content.append(joiner.toString()).append("\n");
        });
        

        result.appendOutput(content.toString());
    }

	@Override
	protected void resetSpecializedState() {
		
	}
}
