package rpc.petrel.config;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@ToString
public class RpcServiceConfig {
    /**
     * service version
     */
    private String version = "";
    /**
     * when the interface has multiple implementation classes, distinguish by group
     */
    private String group = "";

    /**
     * target service
     */
    private Object service;

    /**
     * 和RpcRequest的同名方法内容一致
     * @return 服务名称 (interface + group + version)
     */
    public String getRpcServiceName() {
        return this.getServiceName() + this.getGroup() + this.getVersion();
    }

    public String getServiceName() {
        //返回实现的接口名字
        //return this.service.getClass().getInterfaces()[0].getCanonicalName();
        return this.service.getClass().getInterfaces()[0].getName();//和RpcClientProxy构造RpcRequest是一致
    }
}
