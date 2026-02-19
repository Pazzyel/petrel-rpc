package rpc.pazz.properties;

//key是xxx.xxx.xxx的结构，value可以是值，也可以是嵌套的${}
public record Property(String key, String defaultValue) {
}