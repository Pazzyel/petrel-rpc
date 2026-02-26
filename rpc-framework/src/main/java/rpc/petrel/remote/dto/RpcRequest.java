package rpc.petrel.remote.dto;

import lombok.*;
import rpc.petrel.filter.Invocation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Getter
@Builder
@ToString
public class RpcRequest implements Serializable, Invocation {

    private static final long serialVersionUID = 1905122041950251207L;
    private String requestId;
    private String interfaceName;
    private String methodName;
    private Object[] parameters;
    private Class<?>[] paramTypes;
    private String version;
    private String group;
    private Map<String, String> attachment;

    public RpcRequest() {
        this.attachment = new HashMap<>();
    }

    public String getRpcServiceName() {
        return this.getInterfaceName() + this.getGroup() + this.getVersion();
    }

    @Override
    public void setAttachment(String key, String value) {
        attachment.put(key, value);
    }

    @Override
    public void setAttachmentIfAbsent(String key, String value) {
        attachment.putIfAbsent(key, value);
    }

    @Override
    public String getAttachment(String key) {
        return attachment.get(key);
    }

    @Override
    public String getAttachment(String key, String defaultValue) {
        return attachment.getOrDefault(key, defaultValue);
    }

}
