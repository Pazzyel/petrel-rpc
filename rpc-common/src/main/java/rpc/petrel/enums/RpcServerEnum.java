package rpc.petrel.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public enum RpcServerEnum {

    NETTY("nettyServer"),
    SOCKET("socketServer");

    private final String name;
}
