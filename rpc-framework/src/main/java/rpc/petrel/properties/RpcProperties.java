package rpc.petrel.properties;

import lombok.Getter;
import lombok.Setter;
import rpc.petrel.enums.*;

import java.util.Objects;

@Setter
public class RpcProperties {

    private Registry registry = new Registry();

    private String connection = "netty";

    private String serializer = "kryo";

    private String compression = "gzip";

    private PropertyResolver resolver;

    public RpcProperties() {
        this.resolver = PropertyResolver.createPropertyResolver();
        this.registry = new Registry();
        this.registry.setType(Objects.requireNonNullElse(this.resolver.getProperty(PropertiesKey.REGISTRY_TYPE),"zookeeper"));
        this.registry.setAddress(Objects.requireNonNullElse(this.resolver.getProperty(PropertiesKey.REGISTRY_ADDRESS),"localhost:2181"));
        this.connection = Objects.requireNonNullElse(this.resolver.getProperty(PropertiesKey.REGISTRY_CONNECTION),"netty");
        this.serializer = Objects.requireNonNullElse(this.resolver.getProperty(PropertiesKey.SERIALIZER),"kryo");
        this.compression = Objects.requireNonNullElse(this.resolver.getProperty(PropertiesKey.COMPRESSION),"gzip");
    }


    @Getter
    @Setter
    public static class Registry {
        private String type = "zookeeper";
        private String address = "localhost:2181";
    }

    public ServiceRegistryEnum getRegistryType() {
        switch (registry.getType()) {
            case "zookeeper":
                return ServiceRegistryEnum.ZK;
            default:
                throw new RuntimeException("Unsupported registry type: " + registry.getType());
        }
    }

    public String getRegistryAddress() {
        return registry.getAddress();
    }

    /**
     * 和getClientType用于返回一样的值，返回的类型不同是设计的失误
     * @return 通信方法
     */
    public RpcServerEnum getServerType() {
        switch (connection) {
            case "netty":
                return RpcServerEnum.NETTY;
            case "socket":
                return RpcServerEnum.SOCKET;
            default:
                throw new RuntimeException("Unsupported connection type: " + connection);
        }
    }

    public RpcRequestTransportEnum getClientType() {
        switch (connection) {
            case "netty":
                return RpcRequestTransportEnum.NETTY;
            case "socket":
                return RpcRequestTransportEnum.SOCKET;
            default:
                throw new RuntimeException("Unsupported connection type: " + connection);
        }
    }

    public SerializationTypeEnum getSerializationType() {
        switch (serializer) {
            case "kryo":
                return SerializationTypeEnum.KRYO;
            case "protostuff":
                return SerializationTypeEnum.PROTOSTUFF;
            case "hessian":
                return SerializationTypeEnum.HESSIAN;
            default:
                throw new RuntimeException("Unsupported serialization type: " + serializer);
        }
    }

    public CompressTypeEnum getCompressionType() {
        switch (compression) {
            case "gzip":
                return CompressTypeEnum.GZIP;
            default:
                throw new RuntimeException("Unsupported compression type: " + compression);
        }
    }
}
