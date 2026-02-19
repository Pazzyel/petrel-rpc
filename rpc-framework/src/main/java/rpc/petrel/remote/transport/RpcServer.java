package rpc.petrel.remote.transport;

import rpc.petrel.extension.SPI;

@SPI
public interface RpcServer {
    void start();
}
