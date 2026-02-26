package rpc.petrel.filter;

import java.util.Map;

public interface Invocation {

    String getRpcServiceName();

    String getMethodName();

    void setAttachment(String key, String value);

    void setAttachmentIfAbsent(String key, String value);

    String getAttachment(String key);

    String getAttachment(String key, String defaultValue);

}
