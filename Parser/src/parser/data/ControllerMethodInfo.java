package parser.data;

import java.util.ArrayList;
import java.util.List;

public class ControllerMethodInfo extends MethodInfo {
    private String url;
    private String httpMethod;
    private List<String> parameters;
    
    public ControllerMethodInfo(String name, String comment, String url, String httpMethod) {
        super(name, comment);
        this.url = url;
        this.httpMethod = httpMethod;
        this.parameters = new ArrayList<>();
    }
    
    public String getUrl() { return url; }
    public String getHttpMethod() { return httpMethod; }
    public List<String> getParameters() { return parameters; }
    public void addParameter(String param) { parameters.add(param); }
}